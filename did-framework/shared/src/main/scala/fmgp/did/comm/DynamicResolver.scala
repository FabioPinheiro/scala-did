package fmgp.did.comm

import zio.*

import fmgp.did.*
import fmgp.crypto.error.*

//TODO move out of the JVM into the
final case class DynamicResolver(resolver: Resolver) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] =
    for {
      docFromResolver <- resolver.didDocument(did)
      // sm <- transportManager.get
      doc = DIDDocumentClass(
        id = docFromResolver.id,
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
