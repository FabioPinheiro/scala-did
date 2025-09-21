package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto._

class DIDResolverProxy(baseUrl: String, httpUtils: HttpUtils) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] =
    DIDResolverProxy.didDocument(baseUrl, did.toDID).provideEnvironment(ZEnvironment(httpUtils))
}

object DIDResolverProxy {
  def make(baseUrl: String): ZIO[HttpUtils, Nothing, DIDResolverProxy] =
    ZIO.service[HttpUtils].map(httpUtils => DIDResolverProxy(baseUrl, httpUtils))

  def layer(baseUrl: String): URLayer[HttpUtils, Resolver] = ZLayer.fromZIO(make(baseUrl))

  // /** see https://identity.foundation/peer-did-method-spec/#generation-method */
  def didDocument(baseUrl: String, did: DID): ZIO[HttpUtils, ResolverError, DIDDocument] =
    for {
      httpUtils <- ZIO.service[HttpUtils]
      didDoc <- httpUtils
        .getT[DIDDocument](s"$baseUrl/${did.string}")
        .mapError(ex => DIDresolutionFail.fromThrowable(ex))
    } yield didDoc
}
