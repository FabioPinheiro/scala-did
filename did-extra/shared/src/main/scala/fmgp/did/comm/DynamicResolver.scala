package fmgp.did.comm

import zio._

import fmgp.did._
import fmgp.crypto.error._
import fmgp.did.method.peer.DidPeerResolver
// import fmgp.did.uniresolver.Uniresolver

//TODO move out of the JVM into the
final case class DynamicResolver(resolver: Resolver) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[DidFail, DIDDocument] =
    for {
      docFromResolver <- resolver.didDocument(did)
      // sm <- transportManager.get
      doc = DIDDocumentClass(
        id = docFromResolver.id,
        alsoKnownAs = docFromResolver.alsoKnownAs,
        controller = docFromResolver.controller,
        verificationMethod = docFromResolver.verificationMethod,
        authentication = docFromResolver.authentication,
        assertionMethod = docFromResolver.assertionMethod,
        keyAgreement = docFromResolver.keyAgreement,
        capabilityInvocation = docFromResolver.capabilityInvocation,
        capabilityDelegation = docFromResolver.capabilityDelegation,
        service = docFromResolver.service, // TODO data from sm
      )
    } yield (doc)
}

object DynamicResolver {
  def layer: ULayer[DynamicResolver] =
    ZLayer.succeed(DynamicResolver(DidPeerResolver.default))
}
