package fmgp.did.framework

import zio._
import zio.json._
import zio.stream._
import zio.http._
import fmgp.crypto.error._
import fmgp.did._
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

  def transmissionFlow = Transport.TransmissionFlow.BothWays
  def transmissionType = Transport.TransmissionType.MultiTransmissions

  override def outbound: ZSink[Any, Transport.OutErr, MSG, Nothing, Unit] = ZSink.fromQueue(outboundBuf)
  override def inbound: ZStream[Any, Transport.InErr, MSG] = ZStream.fromHub(inboundBuf)
}

object TransportWSImp {

  // def open(url: String): ZIO[Operator & Operations & Resolver & (Client & Scope), Throwable, String] = {
  //   def channel2Transport(
  //       channel: Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]]
  //   ): ZIO[Operator & (Operations & Resolver), Throwable, TransportDIDCommWS[Any]] = {
  //     val socketID = Websocket.nextSocketName
  //     ZIO.logAnnotate(Websocket.logAnnotation(socketID)) {
  //       for {
  //         transport <- make(channel, identifier = socketID)
  //         ws <- WebsocketJVMImp
  //           .bindings(channel, transport.ws)
  //           .tapError(e => ZIO.logError(e.getMessage))
  //           .fork
  //         op <- ZIO.service[Operator]
  //         transportWarp = TransportDIDCommWS(transport)
  //         _ <- op
  //           .receiveTransport(transportWarp)
  //           .tapErrorCause(ZIO.logErrorCause(_))
  //           .mapError(DidException(_))
  //         _ <- ws.join *> ZIO.log("WebsocketJVM CLOSE")
  //       } yield transportWarp
  //     }
  //   }
  //   val url = URL.decode("url").getOrElse(???)

  //   for {
  //     client <- ZIO.service[Client]
  //     xxx = Handler.fromFunctionZIO(channel2Transport _)
  //     app = WebSocketApp(handler = xxx, customConfig = None)
  //     client2 = if (url.isAbsolute) client.url(url) else client.addUrl(url)
  //     aaa = client2.socket(app)
  //     response <- createWebSocketAppWithOperator(Seq.empty).connect("FIXME").debug
  //     data <- response.body.asString
  //       .tapError(ex => ZIO.logError(s"Fail parse http WS response body: ${ex.getMessage}"))
  //       .orDie
  //     _ <- response.status.isError match
  //       case true  => ZIO.logError(data)
  //       case false => ZIO.logInfo(data)
  //   } yield (data)
  // }

  def createWebSocketAppWithOperator(
      annotationMap: Seq[LogAnnotation]
  ): WebSocketApp[Operator & Operations & Resolver] =
    WebSocketApp(
      handler = Handler
        .fromFunctionZIO((channel: Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]]) =>
          val socketID = Websocket.nextSocketName
          ZIO.logAnnotate(Websocket.logAnnotation(socketID), annotationMap*) {
            for {
              transport <- make(channel, identifier = socketID)
              ws <- fmgp.did.framework.WebsocketJVMImp
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
      override def onMessage(message: String) =
        inbound.offer(message).flatMap {
          case true => ZIO.unit
          case false =>
            ZIO.logError(s"Message lost in inbound: '$message'") *>
              ZIO.fail(new RuntimeException(s"Message lost in inbound: '$message'"))
        }
      override def send(value: String) = channel.send(ChannelEvent.Read(WebSocketFrame.text(value)))
      override def close = channel.shutdown
    }
    _ <- ZStream
      .fromQueue(outbound)
      .runForeach(data => ws.send(data))
      .fork
  } yield TransportWSImp[String](outbound, inbound, ws)
}
