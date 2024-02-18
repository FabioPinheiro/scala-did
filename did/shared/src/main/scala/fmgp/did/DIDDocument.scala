package fmgp.did

import zio.json._
import fmgp.crypto.PublicKey

/** DIDDocument
  *
  * https://w3c.github.io/did-core/#did-document-properties
  *
  * <https://github.com/w3c/?q=did&type=all&language=&sort>=
  *
  *   - authentication -> challenge-response protocol
  *   - assertionMethod -> Issuer key (for purposes of issuing a Verifiable Credential)
  *   - keyAgreement -> tablishing a secure communication channel with the recipient
  *   - capabilityInvocation -> Master key (for authorization to update the DID Document.)
  *   - capabilityDelegation -> ...
  */
trait DIDDocument extends DID {
  def id: Required[DIDSubject] // = s"$scheme:$namespace:$specificId"
  def alsoKnownAs: NotRequired[Set[String]]
  def controller: NotRequired[Either[String, Set[String]]]
  def verificationMethod: NotRequired[Set[VerificationMethod]]

  def authentication: NotRequired[SetU[VerificationMethod]]
  def assertionMethod: NotRequired[SetU[VerificationMethod]]
  def keyAgreement: NotRequired[Set[VerificationMethod]]
  def capabilityInvocation: NotRequired[SetU[VerificationMethod]]
  def capabilityDelegation: NotRequired[SetU[VerificationMethod]]

  def service: NotRequired[Set[DIDService]]

  // Extra methods
  def didSubject = id.toDID

  private def converteToVerificationMethodReferencedWithKey(vm: VerificationMethod) = vm match {
    case e: VerificationMethodReferenced => // None // verificationMethod is alredy included
      this.verificationMethod.toSeq.flatten
        .find(_.id == e.id)
        .map {
          case v: VerificationMethodReferenced        => ??? // Error?
          case v: VerificationMethodEmbeddedJWK       => VerificationMethodReferencedWithKey(v.id, v.publicKeyJwk)
          case v: VerificationMethodEmbeddedMultibase => ??? // FIXME
        }
    case e: VerificationMethodEmbeddedJWK       => Some(VerificationMethodReferencedWithKey(e.id, e.publicKeyJwk))
    case e: VerificationMethodEmbeddedMultibase => ??? // FIXME
  }

  def allKeysTypeKeyAgreement = keyAgreement
    .getOrElse(Set.empty)
    .flatMap { vm => converteToVerificationMethodReferencedWithKey(vm) }

  val (namespace, specificId) = (id.namespace, id.specificId) // DID.getNamespaceAndSpecificId(id)

  def allKeysTypeAuthentication: Seq[VerificationMethod] = authentication.toSeq.flatMap {
    case v: VerificationMethod                   => Seq(v)
    case seq: Seq[VerificationMethod] @unchecked => seq
  }

  def authenticationByKid(kid: DIDURL): Option[VerificationMethodReferencedWithKey[PublicKey]] =
    allKeysTypeAuthentication
      .find(_.id == kid.string) // FIXME it's possible to have only the fragment
      .flatMap { vm => converteToVerificationMethodReferencedWithKey(vm) }

  private inline def getServices = service.toSeq.flatten
  def getDIDServiceDIDCommMessaging = getServices
    .collect { case e: DIDServiceDIDCommMessaging => e }
  // FIXME
  // def getDIDServiceDIDLinkedDomains = getServices
  //   .collect { case e: DIDServiceDIDLinkedDomains => e }
  // def getDIDServiceDecentralizedWebNode = getServices
  //   .collect { case e: DIDServiceDecentralizedWebNode => e }
}

object DIDDocument {
  given decoder: JsonDecoder[DIDDocument] =
    DIDDocumentClass.decoder.map(e => e)
  given encoder: JsonEncoder[DIDDocument] =
    DIDDocumentClass.encoder.contramap(e =>
      DIDDocumentClass(
        id = e.id,
        alsoKnownAs = e.alsoKnownAs,
        controller = e.controller,
        verificationMethod = e.verificationMethod,
        authentication = e.authentication,
        assertionMethod = e.assertionMethod,
        keyAgreement = e.keyAgreement,
        capabilityInvocation = e.capabilityInvocation,
        capabilityDelegation = e.capabilityDelegation,
        service = e.service,
      )
    )
}

case class DIDDocumentClass(
    id: Required[DIDSubject],
    alsoKnownAs: NotRequired[Set[String]] = None,
    controller: NotRequired[Either[String, Set[String]]] = None,
    verificationMethod: NotRequired[Set[VerificationMethod]] = None,
    authentication: NotRequired[SetU[VerificationMethod]] = None,
    assertionMethod: NotRequired[SetU[VerificationMethod]] = None,
    keyAgreement: NotRequired[Set[VerificationMethod]] = None,
    capabilityInvocation: NotRequired[SetU[VerificationMethod]] = None,
    capabilityDelegation: NotRequired[SetU[VerificationMethod]] = None,
    service: NotRequired[Set[DIDService]] = None,
) extends DIDDocument

object DIDDocumentClass {
  import SetU.{given}
  given decoder: JsonDecoder[DIDDocumentClass] = DeriveJsonDecoder.gen[DIDDocumentClass]
  given encoder: JsonEncoder[DIDDocumentClass] = DeriveJsonEncoder.gen[DIDDocumentClass]
}
