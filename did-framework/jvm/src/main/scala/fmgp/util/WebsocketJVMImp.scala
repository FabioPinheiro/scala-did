package fmgp.util

import zio._
import zio.http._
import zio.http.ChannelEvent._
import zio.http.WebSocketFrame._

object WebsocketJVMImp {
  def bindings(
      channel: Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]],
      wsProgram: Websocket[Throwable],
  ) = {
    for {
      job <- channel
        .receiveAll {
          case UserEventTriggered(UserEvent.HandshakeComplete) => wsProgram.onHandshakeComplete
          case UserEventTriggered(UserEvent.HandshakeTimeout)  => wsProgram.onHandshakeTimeout
          case ChannelEvent.Registered                         => wsProgram.onError("Registered", s"Unexpected event")
          case ChannelEvent.Unregistered                       => wsProgram.onClose("Unregistered")
          case ChannelEvent.Read(frame: Text)                  => wsProgram.onMessage(frame.text)
          case ChannelEvent.Read(frame: Binary)                => wsProgram.onError("Binary", s"Unexpected event")
          case ChannelEvent.Read(Close(status, reason))        => wsProgram.onError("Close", s"Unexpected event")
          case ChannelEvent.Read(_: Continuation)              => wsProgram.onError("Continuation", s"Unexpected event")
          case ChannelEvent.Read(Ping)                         => wsProgram.onError("Ping", s"Unexpected event")
          case ChannelEvent.Read(Pong)                         => wsProgram.onError("Pong", s"Unexpected event")
          case ChannelEvent.ExceptionCaught(ex)                => wsProgram.onError("ExceptionCaught", ex.getMessage())
        }
        .tapError(e => ZIO.logError(e.getMessage()))
        .fork
        .debug
      wait <- job.join.debug
      _ <- ZIO.logDebug("WS Function END")
    } yield ()
  }

}
