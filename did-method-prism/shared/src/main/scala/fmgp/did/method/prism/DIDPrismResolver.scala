package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto._

class DIDPrismResolver(baseUrl: String, httpUtils: HttpUtils) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] = did.toDID match {
    case prism: DIDPrism =>
      DIDPrismResolver
        .didDocument(baseUrl, prism)
        .provideEnvironment(ZEnvironment(httpUtils))
    case did if DIDPrism.regexPrism.matches(did.string) =>
      DIDPrismResolver
        .didDocument(baseUrl, DIDPrism(did.specificId))
        .provideEnvironment(ZEnvironment(httpUtils))
    case did => ZIO.fail(UnsupportedMethod(did.namespace))
  }
}
object DIDPrismResolver {
  def make(baseUrl: String): ZIO[HttpUtils, Nothing, DIDPrismResolver] = for {
    httpUtils <- ZIO.service[HttpUtils]
  } yield DIDPrismResolver(baseUrl, httpUtils)

  def layer(baseUrl: String): URLayer[HttpUtils, Resolver] = layerDIDPrismResolver(baseUrl)
  def layerDIDPrismResolver(baseUrl: String): URLayer[HttpUtils, DIDPrismResolver] = ZLayer.fromZIO(make(baseUrl))

  // /** see https://identity.foundation/peer-did-method-spec/#generation-method */
  def didDocument(baseUrl: String, did: DIDPrism): ZIO[HttpUtils, ResolverError, DIDDocument] = did match {
    case prism: DIDPrism =>
      for {
        httpUtils <- ZIO.service[HttpUtils]
        didDoc <- httpUtils
          .getT[DIDDocument](s"$baseUrl/${did.string}")
          .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      } yield didDoc
  }
}
