package fmgp.did.framework

import zio._
import zio.json._
import zio.stream._
import zio.http._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._

object StreamFromWebSocket {

  def zioWebSocketApp[R, E](pipeline: ZPipeline[R, E, String, String]) = for {
    annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
  } yield createWebSocketApp(annotationMap, pipeline)

  private[framework] def createWebSocketApp[R, E](
      annotationMap: Seq[LogAnnotation],
      pipeline: ZPipeline[R, E, String, String]
  ): WebSocketApp[R] = { // Operator & Operations & Resolver
    import zio.http.ChannelEvent._
    import zio.http.WebSocketFrame._

    WebSocketApp(
      handler = Handler
        .fromFunctionZIO { (channel: Channel[ChannelEvent[WebSocketFrame], ChannelEvent[WebSocketFrame]]) =>
          val socketID = Websocket.nextStreamName

          ZIO.scopedWith(scope =>
            ZIO.logAnnotate(Websocket.logAnnotation(socketID), annotationMap*) {
              // Lifting an Asynchronous API to ZStream
              ZStream
                .asyncZIO[Any, Throwable, String] { cb =>
                  channel
                    .receiveAll {
                      case UserEventTriggered(UserEvent.HandshakeComplete) => ZIO.logDebug(s"WS HandshakeComplete")
                      case UserEventTriggered(UserEvent.HandshakeTimeout)  => ZIO.logWarning(s"WS HandshakeTimeout")
                      case ChannelEvent.Registered        => ZIO.logWarning(s"WS Error Registered: Unexpected event")
                      case ChannelEvent.Unregistered      => ZIO.logDebug(s"WS Closed because 'Unregistered'")
                      case ChannelEvent.Read(frame: Text) => ZIO.succeed(cb(ZIO.succeed(zio.Chunk(frame.text))))
                      case ChannelEvent.Read(frame: Binary) =>
                        ZIO.logError("WS: Binary Event NotImplemented") *> ZIO.die(new NotImplementedError)
                      case ChannelEvent.Read(Close(status, reason)) => ZIO.logWarning(s"WS Error Close: " + reason)
                      case ChannelEvent.Read(_: Continuation)       => ZIO.logWarning(s"WS Error Continuation")
                      case ChannelEvent.Read(Ping)                  => ZIO.logWarning(s"WS Error Ping")
                      case ChannelEvent.Read(Pong)                  => ZIO.logWarning(s"WS Error Pong")
                      case ChannelEvent.ExceptionCaught(ex) => ZIO.logError(s"WS ExceptionCaught: " + ex.getMessage)
                    }
                    .onExit(e =>
                      ZIO.logDebug("WS Closeing")
                        *> channel.awaitShutdown
                        *> ZIO.ignore(cb.end)
                        *> ZIO.log("WS Close")
                    )
                    .forkIn(scope)
                }
                .debug("Before Pipeline")
                .via { pipeline }
                // .tap(e => ZIO.logInfo(e))
                .mapZIO(msg => channel.send(ChannelEvent.Read(WebSocketFrame.text(msg))))
                .runDrain
                .catchAll { ex =>
                  // https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1
                  // channel.send(ChannelEvent.Read(Close(1002, Some("reason"))))
                  channel.shutdown // Disconnected (code: 1000, reason: "Bye")
                    .debug(s"WS channel.shutdown with Throwable: $ex")
                }
                .debug("END stream")
            }
          )
        },
      customConfig = None
    )
  }

}
