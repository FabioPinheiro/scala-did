package fmgp.util

import zio._

trait Websocket[E] {
  // Binding
  def onOpen(evType: String): IO[E, Unit] =
    // wsProgram.onStateChange(Websocket.State.OPEN) *>
    ZIO.logDebug(s"WS Connected '$evType'")
  def onClose(reason: String): IO[E, Unit] =
    // wsProgram.onStateChange(Websocket.State.CLOSED) *>
    ZIO.logDebug(s"WS Closed because '${reason}'")
  def onMessage(message: String): IO[E, Unit]
  def onError(evType: String, errorMessage: String): IO[E, Unit] =
    // wsProgram.onStateChange(Websocket.State.CLOSED) *>
    ZIO.logError(s"WS Error (type:$evType): " + errorMessage)

  /** Transmits data to the server over the WebSocket connection. */
  def send(message: String): IO[E, Unit]

  // Metadata
  val socketID: String

  def close: UIO[Unit]

  def onHandshakeComplete = onOpen(evType = "HandshakeComplete")
  def onHandshakeTimeout =
    ZIO.logWarning(s"HandshakeTimeout") *> onClose(reason = "HandshakeTimeout")

  // var state: Websocket.State = Websocket.State.CONNECTING
  // Extra https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/readyState
  // final def onStateChangeProgram(state: Websocket.State): UIO[Unit] = ZIO.succeed({ state = s })
}

object Websocket {
  type State = State.Value
  val SOCKET_ID = "SocketID"

  private var socketCounter = 1
  // TODO use scala.util.Random.nextLong().toString
  def nextSocketName = "socket:" + this.synchronized { socketCounter += 1; socketCounter }
  def logAnnotation(socketID: String = nextSocketName) = LogAnnotation(SOCKET_ID, socketID)

  /** @see https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/readyState */
  object State extends Enumeration {

    /** Socket has been created. The connection is not yet open. */
    val CONNECTING = Value(0)

    /** The connection is open and ready to communicate. */
    val OPEN = Value(1)

    /** The connection is in the process of closing. */
    val CLOSING = Value(2)

    /** The connection is closed or couldn't be opened. */
    val CLOSED = Value(3)
  }

  // Accessor Methods Inside the Companion Object
  // def onOpen(evType: String): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.onOpen(evType))
  // def onClose(reason: String): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.onClose(reason))
  // def onMessage(message: String): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.onMessage(message))
  // def onError(evType: String, message: String): URIO[Websocket, Unit] =
  //   ZIO.serviceWithZIO(_.onError(evType: String, message: String))
  // def onStateChange(newState: Websocket.State): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.onStateChange(newState))
  // def send(data: String): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.send(data))
}
