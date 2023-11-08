package fmgp.util

import zio._
import zio.json._
import zio.stream._
import zio.http._
import fmgp.did._
import fmgp.crypto.error._
import fmgp.did.comm._

/** this API is still a WIP
  *
  * The Auto reconnect feature was remove.
  */
class TransportWSImp[MSG](
    outboundBuf: Queue[MSG],
    inboundBuf: Hub[MSG],
    val ws: Websocket[Throwable],
) extends TransportWS[Any, MSG] {

  override def outbound: ZSink[Any, Transport.OutErr, MSG, Nothing, Unit] = ZSink.fromQueue(outboundBuf)
  override def inbound: ZStream[Any, Transport.InErr, MSG] = ZStream.fromHub(inboundBuf)
}

class TransportDIDCommWS(transport: TransportWSImp[String]) extends TransportDIDComm[Any] {

  /** Send to the other side. Out going Messages */
  def outbound: ZSink[Any, Transport.OutErr, SignedMessage | EncryptedMessage, Nothing, Unit] =
    transport.outbound.contramap((_: Message).toJson)

  /** Reciving from the other side. Income Messages */
  def inbound: ZStream[Any, Transport.InErr, SignedMessage | EncryptedMessage] =
    transport.inbound.map(_.fromJson[Message]).collect {
      case Right(sMsg: SignedMessage)    => sMsg
      case Right(eMsg: EncryptedMessage) => eMsg
    }

  def id: TransportID = transport.id
}

object TransportWSImp {

  def createWebSocketAppWithOperator(
      annotationMap: Seq[LogAnnotation]
  ): WebSocketApp[Operator & Operations] =
    WebSocketApp(
      handler = Handler
        .fromFunctionZIO((channel: Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]]) =>
          val socketID = Websocket.nextSocketName
          ZIO.logAnnotate(Websocket.logAnnotation(socketID), annotationMap: _*) {
            for {
              transport <- make(channel, identifier = socketID)
              ws <- WebsocketJVMImp
                .bindings(channel, transport.ws)
                .tapError(e => ZIO.logError(e.getMessage))
                .fork
              op <- ZIO.service[Operator]
              transportWarp = TransportDIDCommWS(transport)
              _ <- op
                .receiveTransport(transportWarp)
                .tapErrorCause(ZIO.logErrorCause(_))
                .mapError(DidException(_))
              _ <- ws.join *> ZIO.log("WebsocketJVM CLOSE")
            } yield ws
          }
        ),
      customConfig = None
    )

  def make(
      channel: Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]],
      identifier: String,
      boundSize: Int = 10,
  ): ZIO[Any, Nothing, TransportWSImp[String]] = for {
    outbound <- Queue.bounded[String](boundSize)
    inbound <- Hub.bounded[String](boundSize)
    ws = new Websocket[Throwable] {
      override val socketID: String = identifier
      override def onMessageProgram(message: String) =
        inbound.offer(message).flatMap {
          case true => ZIO.unit
          case false =>
            ZIO.logError(s"Message lost in inbound: '$message'") *>
              ZIO.fail(new RuntimeException(s"Message lost in inbound: '$message'"))
        }
      override def sendProgram(value: String) = channel.send(ChannelEvent.Read(WebSocketFrame.text(value)))
    }
    _ <- ZStream
      .fromQueue(outbound)
      .runForeach(data => ws.send(data))
      .fork
  } yield TransportWSImp[String](outbound, inbound, ws)
}