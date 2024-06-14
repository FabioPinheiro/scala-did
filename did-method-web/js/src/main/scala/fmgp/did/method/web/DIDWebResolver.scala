package fmgp.did.method.web

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO

import scala.util.chaining.scalaUtilChainingOps
import scala.scalajs.js
import org.scalajs.dom._

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
