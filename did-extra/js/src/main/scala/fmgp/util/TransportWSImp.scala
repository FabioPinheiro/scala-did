package fmgp.util

import scala.scalajs.js
import org.scalajs.dom
import zio._
import zio.json._
import fmgp.did.comm._
import zio.stream._

type OutErr = Nothing
type InErr = Nothing

/** this API is still a WIP
  *
  * The Auto reconnect feature was remove.
  */
class TransportWSImp[MSG](
    private val outboundBuf: Queue[MSG],
    private val inboundBuf: Hub[MSG],
    /*private val*/ jsWS: dom.WebSocket,
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
  type MSG = String

  def wsUrlFromWindowLocation = org.scalajs.dom.window.location.origin.replaceFirst("http", "ws") + "/ws"

  def layer: ZLayer[Any, Nothing, TransportWSImp[MSG]] = ZLayer.fromZIO(make())

  def makeWSProgram: Websocket = new Websocket {
    var state: Websocket.State = Websocket.State.CONNECTING
    def onMessage(message: String): UIO[Unit] = ZIO.logDebug(s"onMessage: $message")
    def onStateChange(s: Websocket.State): UIO[Unit] = ZIO.succeed({ state = s })
  }

  def make(
      wsUrl: String = wsUrlFromWindowLocation, // "ws://localhost:8080/ws",
      boundSize: Int = 10,
      wsProgram: Websocket = makeWSProgram,
  ): ZIO[Any, Nothing, TransportWSImp[MSG]] = for {
    outbound <- Queue.bounded[MSG](boundSize)
    inbound <- Hub.bounded[MSG](boundSize)

    // JS WebSocket bindings onOpen/onClose/onMessage/onError
    tmpWS = new dom.WebSocket(wsUrl)

    transportWS = new TransportWSImp[MSG](outbound, inbound, tmpWS)
    _ <- ZIO.logDebug("transportWS.bindings")
    _ <- ZIO.unit.delay(1.second).debug
    streamSendMessages <- ZStream.fromQueue(outbound).runForeach(data => ZIO.succeed(tmpWS.send(data))).fork
    streamOnMessage <- ZStream
      .async[Any, Nothing, Unit] { callback =>
        tmpWS.onopen = { (ev: dom.Event) =>
          callback(
            wsProgram.onStateChange(Websocket.State.OPEN) *>
              wsProgram.onOpen(ev.`type`) *>
              ZIO.succeed(Chunk())
          )
        }
        tmpWS.onmessage = { (ev: dom.MessageEvent) =>
          callback {
            val data = ev.data.toString
            wsProgram.onMessage(data) *> inbound.offer(data) *> ZIO.succeed(Chunk())
          }
        }
        tmpWS.onerror = { (ev: dom.Event) =>
          val message = ev
            .asInstanceOf[js.Dynamic]
            .message
            .asInstanceOf[js.UndefOr[String]]
            .fold("")("Error: " + _)
          callback(
            wsProgram.onStateChange(Websocket.State.CLOSED) *>
              wsProgram.onError(ev.`type`, message) *>
              ZIO.succeed(Chunk())
          )
        }
        tmpWS.onclose = { (ev: dom.CloseEvent) =>
          callback(
            wsProgram.onStateChange(Websocket.State.CLOSED) *>
              wsProgram.onClose(ev.reason) *>
              ZIO.succeed(Chunk())
          )
        }
      }
      .runDrain
      .fork
    _ <- ZIO.log("Make TransportWS created (and bindings done)")
  } yield transportWS
}
