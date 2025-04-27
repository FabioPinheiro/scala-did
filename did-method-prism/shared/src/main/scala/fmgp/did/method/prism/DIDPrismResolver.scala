package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto._

class DIDPrismResolver extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[UnsupportedMethod, DIDDocument] = did.toDID match {
    case prism: DIDPrism => DIDPrismResolver.didDocument(prism)
    case did if DIDPrism.regexPrism.matches(did.string) =>
      DIDPrismResolver.didDocument(DIDPrism(did.specificId))
    case did => ZIO.fail(UnsupportedMethod(did.namespace))
  }
}
object DIDPrismResolver {
  val default = new DIDPrismResolver()
  val layer: ULayer[Resolver] = ZLayer.succeed(default)
  val layerDIDPrismResolver: ULayer[DIDPrismResolver] = ZLayer.succeed(default)

  // /** see https://identity.foundation/peer-did-method-spec/#generation-method */
  def didDocument(did: DIDPrism): UIO[DIDDocument] = did match {
    case prism: DIDPrism => genesisDocument(prism)
  }

  def genesisDocument(did: DIDPrism): UIO[DIDDocument] = ZIO.succeed(???)
}
