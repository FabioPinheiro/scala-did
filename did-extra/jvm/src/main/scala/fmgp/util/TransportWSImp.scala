package fmgp.util

import zio._
import zio.http._
import fmgp.did._
import fmgp.crypto.error._
import fmgp.did.comm._

import zio.stream._
import zio.http.ChannelEvent._
import zio.http.WebSocketFrame.Binary
import zio.http.WebSocketFrame.Text
import zio.http.WebSocketFrame.Close
import zio.http.WebSocketFrame.Continuation
import zio.http.WebSocketFrame.Ping
import zio.http.WebSocketFrame.Pong

/** this API is still a WIP
  *
  * The Auto reconnect feature was remove.
  */
class TransportWSImp[MSG](
    private val outboundBuf: Queue[MSG],
    private val inboundBuf: Hub[MSG],
    val ws: Websocket[Throwable],
) extends Transport[Any, MSG] {

  def outbound: ZSink[Any, OutErr, MSG, Nothing, Unit] = ZSink.fromQueue(outboundBuf)
  def inbound: ZStream[Any, InErr, MSG] = ZStream.fromHub(inboundBuf)

  def send(message: MSG): zio.UIO[Boolean] =
    ZIO.log(s"send $message") *>
      outboundBuf.offer(message)
  def subscribe: ZIO[Scope, Nothing, Dequeue[MSG]] = inboundBuf.subscribe
  def recive[R, E](process: (MSG) => ZIO[R, E, Unit]) = inbound.runForeach(process)

}

object TransportWSImp {

  type R = Operations & MessageDispatcher
  private type MSG = String

  def wsFunction(
      channel: Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]],
      annotationMap: Seq[LogAnnotation]
  ) = {
    import zio.http.ChannelEvent._

    for {
      transport <- make(channel)
      wsProgram = transport.ws
      job <- channel
        .receiveAll {
          case UserEventTriggered(UserEvent.HandshakeComplete) => wsProgram.onHandshakeComplete
          case UserEventTriggered(UserEvent.HandshakeTimeout)  => wsProgram.onHandshakeTimeout
          case ChannelEvent.Registered                         => wsProgram.onError("Registered", s"Unexpected event")
          case ChannelEvent.Unregistered                       => wsProgram.onClose("Unregistered")
          case ChannelEvent.Read(frame: WebSocketFrame.Text)   => wsProgram.onMessage(frame.text)
          case ChannelEvent.Read(frame: WebSocketFrame.Binary) => wsProgram.onError("Binary", s"Unexpected event")
          case ChannelEvent.Read(Close(status, reason))        => wsProgram.onError("Close", s"Unexpected event")
          case ChannelEvent.Read(_: WebSocketFrame.Continuation) =>
            wsProgram.onError("Continuation", s"Unexpected event")
          case ChannelEvent.Read(Ping)          => wsProgram.onError("Ping", s"Unexpected event")
          case ChannelEvent.Read(Pong)          => wsProgram.onError("Pong", s"Unexpected event")
          case ChannelEvent.ExceptionCaught(ex) => wsProgram.onError("ExceptionCaught", ex.getMessage())
        }
        .tapError(e => ZIO.logError(e.getMessage()))
        .fork
        .debug
      wait <- job.join.debug
      _ <- ZIO.log("WS Function END")
    } yield ()
  }

  def createWebSocketApp(annotationMap: Seq[LogAnnotation]): WebSocketApp[Operations & MessageDispatcher] =
    WebSocketApp(
      handler = Handler
        .fromFunctionZIO(c => wsFunction(c, annotationMap))
        .tapErrorZIO(e => ZIO.logError(e.getMessage())),
      customConfig = None
    )

  def make(
      channel: Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]],
      boundSize: Int = 10,
  ): ZIO[Any, Nothing, TransportWSImp[MSG]] = for {
    outbound <- Queue.bounded[MSG](boundSize)
    inbound <- Hub.bounded[MSG](boundSize)
    ws = new Websocket[Throwable] {
      override def onMessageProgram(message: String) =
        ZIO.log(s"onMessage: $message") *> inbound.offer(message).flatMap {
          case true  => ZIO.unit
          case false => ZIO.fail(new RuntimeException(s"Message lost: '$message'"))
        }
      override def sendProgram(value: String) = channel.send(ChannelEvent.Read(WebSocketFrame.text(value)))
    }
    _ <- ZStream
      .fromQueue(outbound)
      .runForeach(data => ws.send(data))
      .fork
    _ <- ZIO.log("Make TransportWS created (and bindings done)")
  } yield TransportWSImp[String](outbound, inbound, ws)
}
