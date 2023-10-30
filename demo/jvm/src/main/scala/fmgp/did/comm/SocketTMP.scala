package fmgp.did.comm

import zio._
import zio.http._
import fmgp.did._
import fmgp.crypto.error._
import fmgp.did.comm._

//FIXME REMOVE DELETE
object SocketTMP {
  def createSocketApp(
      agent: AgentWithSocketManager,
      annotationMap: Seq[LogAnnotation]
  ): ZIO[AgentWithSocketManager & Operations & MessageDispatcher, Nothing, Response] = {
    import zio.http.ChannelEvent._
    val SOCKET_ID = "SocketID"
    Handler
      .webSocket { channel => // WebSocketChannel = Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]]
        val channelId = scala.util.Random.nextLong().toString
        channel.receiveAll {
          case UserEventTriggered(UserEvent.HandshakeComplete) =>
            ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelId), annotationMap: _*) {
              ZIO.logDebug(s"HandshakeComplete $channelId") *>
                DIDSocketManager.registerSocket(channel, channelId)
            }
          case UserEventTriggered(UserEvent.HandshakeTimeout) =>
            ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelId), annotationMap: _*) {
              ZIO.logWarning(s"HandshakeTimeout $channelId")
            }
          case ChannelEvent.Registered =>
            ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelId), annotationMap: _*) {
              DIDSocketManager.registerSocket(channel, channelId)
            }
          case ChannelEvent.Unregistered =>
            ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelId), annotationMap: _*) {
              DIDSocketManager.unregisterSocket(channel, channelId)
            }
          case ChannelEvent.Read(WebSocketFrame.Text(text)) =>
            ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelId), annotationMap: _*) {
              ZIO.log(s"GOT NEW Message in socket_ID $channelId TEXT: $text") *>
                DIDSocketManager
                  .newMessage(channel, text, channelId)
                  .flatMap { case (socketID, encryptedMessage) =>
                    agent.receiveMessage(encryptedMessage, Some(socketID))
                  }
                  .debug
                  .tapError(ex => ZIO.logError(s"Error: ${ex}"))
                  .mapError(ex => DidException(ex))
            }
          case ChannelEvent.Read(any) =>
            ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelId), annotationMap: _*) {
              ZIO.logError(s"Unknown event type from '${channelId}': " + any.getClass())
            }
          case ChannelEvent.ExceptionCaught(ex) =>
            ZIO.logAnnotate(LogAnnotation(SOCKET_ID, channelId), annotationMap: _*) {
              ZIO.log(ex.getMessage())
            }
        }
      }
      .tapErrorZIO(ex => ZIO.logError(ex.getMessage))
      .toResponse
      .provideSomeEnvironment { (env) => env.add(env.get[AgentWithSocketManager].didSocketManager) }
  }
}
