package fmgp.util

import zio._

trait Websocket[E] {

  // Task
  protected def onOpenProgram(evType: String): IO[E, Unit] = ZIO.logDebug(s"WS Connected '$evType'")
  protected def onCloseProgram(reason: String): IO[E, Unit] = ZIO.log(s"WS Closed because '${reason}'")
  protected def onMessageProgram(message: String): IO[E, Unit]
  protected def onErrorProgram(evType: String, errorMessage: String): IO[E, Unit] =
    ZIO.logError(s"WS Error (type:$evType): " + errorMessage)

  /** Transmits data to the server over the WebSocket connection. */
  protected def sendProgram(message: String): IO[E, Unit]
  // def send(data: String): IO[E, Unit]

  // var state: Websocket.State = Websocket.State.CONNECTING
  // Extra https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/readyState
  // final def onStateChangeProgram(state: Websocket.State): UIO[Unit] = ZIO.succeed({ state = s })

  // Metadata
  val socketID: String

  // Binding
  final def onOpen(evType: String): IO[E, Unit] =
    // wsProgram.onStateChange(Websocket.State.OPEN) *>
    onOpenProgram(evType)
  final def onClose(reason: String): IO[E, Unit] =
    // wsProgram.onStateChange(Websocket.State.CLOSED) *>
    onCloseProgram(reason)
  final def onMessage(message: String): IO[E, Unit] =
    onMessageProgram(message)
  final def onError(evType: String, errorMessage: String): IO[E, Unit] =
    // wsProgram.onStateChange(Websocket.State.CLOSED) *>
    onErrorProgram(evType, errorMessage)

  final def send(message: String): IO[E, Unit] =
    sendProgram(message)
  def close: UIO[Unit]

  final def onHandshakeComplete = onOpen(evType = "HandshakeComplete")
  final def onHandshakeTimeout =
    ZIO.logWarning(s"HandshakeTimeout") *> onCloseProgram(reason = "HandshakeTimeout")

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
