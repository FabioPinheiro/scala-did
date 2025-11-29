package fmgp.util

import scala.util.chaining.*

import zio.*
import zio.json.*
import zio.http.{MediaType as ZMediaType, *}

import fmgp.did.*
import fmgp.did.comm.*
import fmgp.crypto.error.*

@deprecated
trait MessageDispatcher {
  // TODO deprecate this
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
  ): ZIO[Any, DidFail, String]
}

@deprecated
object MessageDispatcherJVM {
  val layer: ZLayer[Client & Scope, Throwable, MessageDispatcher] =
    ZLayer.fromZIO(
      for {
        client <- ZIO.service[Client]
        scope <- ZIO.service[Scope]
      } yield MessageDispatcherJVM(client, scope)
    )
}

@deprecated
class MessageDispatcherJVM(client: Client, scope: Scope) extends MessageDispatcher {
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String
  ): ZIO[Any, DidFail, String] = {
    val contentTypeHeader = msg.`protected`.obj.typ
      .getOrElse(MediaTypes.ENCRYPTED)
      .pipe(e => Header.ContentType(ZMediaType(e.mainType, e.subType)))
    // val xForwardedHostHeader = xForwardedHost.map(x => Header.Custom(customName = MyHeaders.xForwardedHost, x))
    for {
      res <- Client
        .request(
          Request
            .post(path = destination, body = Body.fromCharSequence(msg.toJson))
            .setHeaders(Headers(contentTypeHeader))
        )
        .tapError(ex => ZIO.logWarning(s"Fail when calling '$destination': ${ex.toString}"))
        .mapError(ex => SomeThrowable(ex))
      data <- res.body.asString
        .tapError(ex => ZIO.logError(s"Fail parse http response body: ${ex.toString}"))
        .mapError(ex => SomeThrowable(ex))
      _ <- res.status.isError match
        case true  => ZIO.logError(data)
        case false => ZIO.logInfo(data)
    } yield (data)
  }.provideEnvironment(ZEnvironment(client, scope))
}
