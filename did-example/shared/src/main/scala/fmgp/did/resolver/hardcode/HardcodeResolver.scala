package fmgp.did.method.hardcode

import zio.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.crypto.error.*

class HardcodeResolver extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] =
    HardcodeResolver.didDocumentOf(did)
}

object HardcodeResolver {
  val default = new HardcodeResolver()
  val layer: ULayer[Resolver] = ZLayer.succeed(default)
  val layerHardcodeResolver: ULayer[HardcodeResolver] = ZLayer.succeed(default)

  def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] = did.value match
    // TODO use this // case "did:example:alice"     => ZIO.succeed(DidExample.senderDIDDocument)
    // TODO use this // case "did:example:bob"       => ZIO.succeed(DidExample.recipientDIDDocument)
    case "did:example:alice"     => ZIO.succeed(DidExampleSicpaRustAlice.aliceDIDDocument)
    case "did:example:bob"       => ZIO.succeed(DidExampleSicpaRustBob.bobDIDDocument)
    case "did:example:charlie"   => ZIO.succeed(DidExampleSicpaRustCharlie.charlieDIDDocument)
    case "did:example:mediator1" => ZIO.succeed(DidExampleSicpaRustMediator1.mediator1DIDDocument)
    case "did:example:mediator2" => ZIO.succeed(DidExampleSicpaRustMediator2.mediator2DIDDocument)
    case "did:example:mediator3" => ZIO.succeed(DidExampleSicpaRustMediator3.mediator3DIDDocument)
    case _                       => ZIO.fail(UnsupportedMethod(did.toDID.namespace))
}
