package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did._

import scala.util.chaining.scalaUtilChainingOps
import scala.scalajs.js
import org.scalajs.dom._

object DIDResolverProxy {

  // def default: Task[DIDResolverProxy] = make

  def make: UIO[DIDResolverProxy] =
    ZIO.succeed(
      DIDResolverProxy(
        "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/diddoc",
      )
    )

  def layer: ULayer[DIDResolverProxy] =
    ZLayer.fromZIO(make)

}

case class DIDResolverProxy(urlBase: String) {
  def httpProxy(did: DID): IO[ResolverError, DIDDocument] = {
    for {
      ret <- ZIO
        .fromPromiseJS(fetch(s"$urlBase/${did.string}", new RequestInit { method = HttpMethod.GET }))
        .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      data <- ZIO
        .fromPromiseJS(ret.text())
        .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      didDoc <- data.fromJson[DIDDocument] match
        case Left(error) => ZIO.fail(DIDresolutionFail.fromParseError("DIDResolverProxy", error))
        case Right(doc)  => ZIO.succeed(doc)
    } yield (didDoc)
  }
}

object DIDWebResolver {}
