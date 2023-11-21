package fmgp.did.framework

import scala.scalajs.js
import org.scalajs.dom
import zio._
import zio.json._
import zio.stream._
import fmgp.did.comm._

/** this API is still a WIP
  *
  * The Auto reconnect feature was remove.
  */
class TransportWSImp[MSG](
    outboundBuf: Queue[MSG],
    inboundBuf: Hub[MSG],
    override val ws: Websocket[TransportWSImp.Err],
    /*private*/ val jsWS: dom.WebSocket,
) extends TransportWS[Any, MSG] {

  override def outbound: ZSink[Any, Transport.OutErr, MSG, Nothing, Unit] = ZSink.fromQueue(outboundBuf)
  override def inbound: ZStream[Any, Transport.InErr, MSG] = ZStream.fromHub(inboundBuf)

  def subscribe: ZIO[Scope, Nothing, Dequeue[MSG]] = inboundBuf.subscribe
  def recive[R, E](process: (MSG) => ZIO[R, E, Unit]) = inbound.runForeach(process)

}

object TransportWSImp {
  type MSG = String
  type Err = Throwable

  def wsUrlFromWindowLocation = org.scalajs.dom.window.location.origin.replaceFirst("http", "ws") + "/ws"

  def layer: ZLayer[Any, Nothing, TransportWSImp[MSG]] = ZLayer.fromZIO(make())

  def make(
      wsUrl: String = wsUrlFromWindowLocation, // "ws://localhost:8080/ws",
      boundSize: Int = 10,
  ): ZIO[Any, Nothing, TransportWSImp[MSG]] = for {
    outbound <- Queue.bounded[MSG](boundSize)
    inbound <- Hub.bounded[MSG](boundSize)

    // JS WebSocket bindings onOpen/onClose/onMessage/onError
    tmpWS = new dom.WebSocket(wsUrl)

    wsProgram: Websocket[Err] = new Websocket[Err] {
      override val socketID = "wsProgramJS"
      override def onMessage(message: String): UIO[Unit] =
        ZIO.logDebug(s"onMessage: $message") *> inbound.offer(message) *> ZIO.unit

      override def send(message: String) = ZIO.attempt(tmpWS.send(message))
      override def close = ZIO.succeed(tmpWS.close())
    }

    transportWS = new TransportWSImp[MSG](outbound, inbound, wsProgram, tmpWS)
    _ <- ZIO.logDebug("transportWS.bindings")
    _ <- ZIO.unit.delay(1.second).debug
    streamSendMessages <- ZStream.fromQueue(outbound).runForeach(data => ZIO.succeed(tmpWS.send(data))).fork
    streamOnMessage <- ZStream
      .async[Any, Err, Unit] { callback =>
        tmpWS.onopen = { (ev: dom.Event) =>
          callback(wsProgram.onOpen(ev.`type`).mapError(Option.apply(_)) *> ZIO.succeed(Chunk()))
        }
        tmpWS.onmessage = { (ev: dom.MessageEvent) =>
          callback { wsProgram.onMessage(ev.data.toString).mapError(Option.apply(_)) *> ZIO.succeed(Chunk()) }
        }
        tmpWS.onerror = { (ev: dom.Event) =>
          val message = ev
            .asInstanceOf[js.Dynamic]
            .message
            .asInstanceOf[js.UndefOr[String]]
            .fold("")("Error: " + _)
          callback(wsProgram.onError(ev.`type`, message).mapError(Option.apply(_)) *> ZIO.succeed(Chunk()))
        }
        tmpWS.onclose = { (ev: dom.CloseEvent) =>
          callback(wsProgram.onClose(ev.reason).mapError(Option.apply(_)) *> ZIO.succeed(Chunk()))
        }
      }
      .runDrain
      .fork
  } yield transportWS
}
