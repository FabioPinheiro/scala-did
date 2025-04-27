package fmgp.did.method.prism

import zio._
import zio.http._
import zio.json._
import fmgp.did._

object DIDResolverProxy {

  def make: ZIO[Client & Scope, Nothing, DIDResolverProxy] =
    for {
      client <- ZIO.service[Client]
      scope <- ZIO.service[Scope]
    } yield DIDResolverProxy(
      "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/diddoc",
      client,
      scope
    )

  def layer: ZLayer[Client & Scope, Nothing, DIDResolverProxy] =
    ZLayer.fromZIO(make)
}

case class DIDResolverProxy(urlBase: String, client: Client, scope: Scope) {
  def httpProxy(did: DID): IO[ResolverError, DIDDocument] = {
    for {
      res <- Client
        .batched(Request.get(path = s"$urlBase/${did.string}"))
        .provideEnvironment(ZEnvironment(client, scope))
        .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      _ = res
      data <- res.body.asString
        .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      didDoc <- data.fromJson[DIDDocument] match
        case Left(error) => ZIO.fail(DIDresolutionFail.fromParseError("DIDResolverProxy", error))
        case Right(doc)  => ZIO.succeed(doc)
    } yield (didDoc)
  }
}
