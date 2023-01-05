package fmgp.did.comm.mediator

import zio._

import fmgp.did._
import fmgp.crypto.error._

final case class DynamicResolver(anotherResolver: Resolver, didSocketManager: Ref[DIDSocketManager]) extends Resolver {
  def didDocument(did: DIDSubject): IO[DidMethodNotSupported, DIDDocument] =
    for {
      cleanDoc <- anotherResolver.didDocument(did)
      sm <- didSocketManager.get
      job = sm.ids
      doc = DIDDocumentClass(
        id = cleanDoc.id,
        alsoKnownAs = cleanDoc.alsoKnownAs,
        controller = cleanDoc.controller,
        verificationMethod = cleanDoc.verificationMethod,
        authentication = cleanDoc.authentication,
        assertionMethod = cleanDoc.assertionMethod,
        keyAgreement = cleanDoc.keyAgreement,
        capabilityInvocation = cleanDoc.capabilityInvocation,
        capabilityDelegation = cleanDoc.capabilityDelegation,
        service = cleanDoc.service,
      )
    } yield (doc)

}
