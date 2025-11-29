package fmgp.did

import zio.json.*
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

  /** @see
    *   https://www.w3.org/TR/did-core/#did-document-properties
    *
    * A set of strings that conform to the rules of [RFC3986] for URIs.
    */
  def alsoKnownAs: NotRequired[Set[URI]]

  /** @see
    *   https://www.w3.org/TR/did-core/#verification-methods
    *
    * The VerificationMethod MUST be embedded (can be a reference to another VerificationMethod)
    */
  def verificationMethod: NotRequired[Set[VerificationMethodEmbedded]]

  /** Keys declared in this section are used to signed JWM.
    *
    * The authentication verification relationship is used to specify how the DID subject is expected to be
    * authenticated, for purposes such as logging into a website or engaging in any sort of challenge-response protocol.
    * @see
    *   https://www.w3.org/TR/did-core/#authentication
    */
  def authentication: NotRequired[SetU[VerificationMethod]]

  /** The assertionMethod verification relationship is used to specify how the DID subject is expected to express
    * claims, such as for the purposes of issuing a Verifiable Credential [VC-DATA-MODEL].
    * @see
    *   https://www.w3.org/TR/did-core/#assertion
    */
  def assertionMethod: NotRequired[SetU[VerificationMethod]]

  /** Keys declared in this section are used as target keys when encrypting a message.
    *
    * The keyAgreement verification relationship is used to specify how an entity can generate encryption material in
    * order to transmit confidential information intended for the DID subject, such as for the purposes of establishing
    * a secure communication channel with the recipient.
    * @see
    *   https://www.w3.org/TR/did-core/#key-agreement
    */
  def keyAgreement: NotRequired[Set[VerificationMethod]]

  /** The capabilityInvocation verification relationship is used to specify a verification method that might be used by
    * the DID subject to invoke a cryptographic capability, such as the authorization to update the DID Document.
    * @see
    *   https://www.w3.org/TR/did-core/#capability-invocation
    */
  def capabilityInvocation: NotRequired[SetU[VerificationMethod]]

  /** The capabilityDelegation verification relationship is used to specify a mechanism that might be used by the DID
    * subject to delegate a cryptographic capability to another party, such as delegating the authority to access a
    * specific HTTP API to a subordinate.
    * @see
    *   https://www.w3.org/TR/did-core/#capability-delegation
    */
  def capabilityDelegation: NotRequired[SetU[VerificationMethod]]

  def service: NotRequired[Set[DIDService]]

  // Extra methods
  def didSubject = id.toDID

  private def converteToVerificationMethodReferencedWithKey(vm: VerificationMethod) = vm match {
    case e: VerificationMethodReferenced => // None // verificationMethod is alredy included
      this.verificationMethod.toSeq.flatten
        .find(_.id == e.id)
        .map {
          case v: VerificationMethodEmbeddedJWK       => VerificationMethodReferencedWithKey(v.id, v.publicKeyJwk)
          case v: VerificationMethodEmbeddedMultibase => ??? // FIXME
        }
    case e: VerificationMethodEmbeddedJWK       => Some(VerificationMethodReferencedWithKey(e.id, e.publicKeyJwk))
    case e: VerificationMethodEmbeddedMultibase => ??? // FIXME
  }

  /** Keys for Signing Messages */
  def allKeysTypeKeyAgreement = keyAgreement
    .getOrElse(Set.empty)
    .flatMap { vm => converteToVerificationMethodReferencedWithKey(vm) }

  val (namespace, specificId) = (id.namespace, id.specificId) // DID.getNamespaceAndSpecificId(id)
  /** Keys for Encrypting Messages */
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
  // TODO
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
    alsoKnownAs: NotRequired[Set[URI]] = None,
    verificationMethod: NotRequired[Set[VerificationMethodEmbedded]] = None,
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
