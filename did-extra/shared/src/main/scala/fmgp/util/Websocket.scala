package fmgp.util

import zio._

trait Websocket {
  def onOpen(evType: String): UIO[Unit] = Console.printLine(s"WS Connected '$evType'").orDie
  def onClose(reason: String): UIO[Unit] = Console.printLine(s"WS Closed because '${reason}'").orDie
  def onMessage(message: String): UIO[Unit]
  def onError(evType: String, errorMessage: String): UIO[Unit] =
    Console.printLine(s"WS Error (type:$evType) occurred! " + errorMessage).orDie

  // Extra https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/readyState
  def onStateChange(state: Websocket.State): UIO[Unit]

  // /** Transmits data to the server over the WebSocket connection. */
  // def send(data: String): UIO[Unit]
}

object Websocket {
  type State = State.Value

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
  def onOpen(evType: String): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.onOpen(evType))
  def onClose(reason: String): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.onClose(reason))
  def onMessage(message: String): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.onMessage(message))
  def onError(evType: String, message: String): URIO[Websocket, Unit] =
    ZIO.serviceWithZIO(_.onError(evType: String, message: String))
  def onStateChange(newState: Websocket.State): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.onStateChange(newState))
  // def send(data: String): URIO[Websocket, Unit] = ZIO.serviceWithZIO(_.send(data))
}
