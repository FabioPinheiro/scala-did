package fmgp.did.method.web

import zio.*
import zio.http.*
import zio.json.*
import fmgp.did.*
import fmgp.did.comm.FROMTO

object DIDWebResolver {

  def make: ZIO[Client & Scope, Nothing, DIDWebResolver] =
    for {
      client <- ZIO.service[Client]
      scope <- ZIO.service[Scope]
    } yield DIDWebResolver(client, scope)

  def layer: ZLayer[Client & Scope, Nothing, Resolver] =
    ZLayer.fromZIO(make)
}

case class DIDWebResolver(client: Client, scope: Scope) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] = did.toDID match {
    case did if (did.namespace == DIDWeb.namespace) =>
      val web = DIDWeb(did.specificId)
      for {
        res <- Client
          .batched(Request.get(path = web.url))
          .provideEnvironment(ZEnvironment(client) ++ ZEnvironment(scope))
          .mapError(ex => DIDresolutionFail.fromThrowable(ex))
        _ = res
        data <- res.body.asString
          .mapError(ex => DIDresolutionFail.fromThrowable(ex))
        didDoc <- data.fromJson[DIDDocument] match
          case Left(error) => ZIO.fail(DIDresolutionFail.fromParseError("DIDResolutionResult", error))
          case Right(doc)  => ZIO.succeed(doc)
      } yield (didDoc)
    case did => ZIO.fail(UnsupportedMethod(did.namespace))
  }
}
