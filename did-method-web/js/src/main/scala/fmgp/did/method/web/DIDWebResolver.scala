package fmgp.did.method.web

import zio.*
import zio.json.*
import fmgp.did.*
import fmgp.did.comm.FROMTO

import scala.util.chaining.scalaUtilChainingOps
import scala.scalajs.js
import org.scalajs.dom.*

object DIDWebResolver {
  val default = DIDWebResolver()

  def make: UIO[DIDWebResolver] =
    ZIO.succeed(DIDWebResolver())

  def layer: ULayer[Resolver] =
    ZLayer.fromZIO(make)
}

case class DIDWebResolver() extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] = did.toDID match {
    case did if (did.namespace == DIDWeb.namespace) =>
      val web = DIDWeb(did.specificId)
      for {
        ret <- ZIO
          .fromPromiseJS(fetch(web.url, new RequestInit { method = HttpMethod.GET }))
          .mapError(ex => DIDresolutionFail.fromThrowable(ex))
        data <- ZIO
          .fromPromiseJS(ret.text())
          .mapError(ex => DIDresolutionFail.fromThrowable(ex))
        didDoc <- data.fromJson[DIDDocument] match
          case Left(error) => ZIO.fail(DIDresolutionFail.fromParseError("DIDResolutionResult", error))
          case Right(doc)  => ZIO.succeed(doc)
      } yield (didDoc)
    case did => ZIO.fail(UnsupportedMethod(did.namespace))
  }
}
