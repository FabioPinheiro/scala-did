package fmgp.did.method.prism

import scala.util.chaining._
import zio.json._
import fmgp.did._
import fmgp.did.method.prism._
import fmgp.did.method.prism.proto._
import fmgp.did.method.prism.cardano._

/** This is the SSI representing a DID PRISM */
final case class SSI(
    did: DIDSubject,
    latestHash: Option[String],
    keys: Seq[PrismPublicKey.UncompressedECKey | PrismPublicKey.CompressedECKey],
    services: Seq[MyService],
    context: Seq[String],
    disabled: Boolean,
    cursor: EventCursor, // append cursor
) { self =>
  def didPrism: DIDPrism = DIDPrism.fromDID(did).getOrElse(???) // FIXME

  def appendAny(spo: MySignedPrismOperation[OP]): SSI = spo.operation match
    case _: CreateDidOP     => append(spo.asInstanceOf[MySignedPrismOperation[CreateDidOP]])
    case _: UpdateDidOP     => append(spo.asInstanceOf[MySignedPrismOperation[UpdateDidOP]])
    case _: DeactivateDidOP => append(spo.asInstanceOf[MySignedPrismOperation[DeactivateDidOP]])
    case _                  => self.copy(cursor = Ordering[EventCursor].max(cursor, spo.eventCursor))

  private def addKey(k: PrismPublicKey): SSI = k match
    case _: PrismPublicKey.VoidKey           => self
    case k: PrismPublicKey.UncompressedECKey => addKey(k)
    case k: PrismPublicKey.CompressedECKey   => addKey(k)

  private def addKey(k: PrismPublicKey.UncompressedECKey | PrismPublicKey.CompressedECKey): SSI =
    if (keys.exists(_.id == k.id)) self //  ID must be unique
    else self.copy(keys = this.keys :+ k)

  def append(spo: MySignedPrismOperation[CreateDidOP | UpdateDidOP | DeactivateDidOP]): SSI = {
    if (Ordering[EventCursor].lteq(spo.eventCursor, this.cursor)) self // Ignore if the event its already process
    else
      {
        spo match
          case MySignedPrismOperation(tx, prismBlockIndex, prismOperationIndex, signedWith, signature, operation, pb) =>
            operation match
              case CreateDidOP(publicKeys, services, context) =>
                latestHash match
                  case Some(value) => self // The Identity already exists
                  case None =>
                    publicKeys
                      .foldLeft(this) { (tmpSSI, key) => tmpSSI.addKey(key) }
                      .copy(latestHash = Some(spo.opHash), services = services, context = context)
                      .pipe(newSSI =>
                        newSSI.checkMasterSignature(spo) match // the signature is checked by itself
                          case false => self
                          case true  => newSSI
                      )
              case UpdateDidOP(previousOperationHash, id, actions) =>
                if (!latestHash.contains(previousOperationHash) & self.checkMasterSignature(spo)) self
                else
                  actions
                    .foldLeft(self) { (tmpSSI, action) =>
                      action match
                        case UpdateDidOP.VoidAction(reason) => tmpSSI
                        case UpdateDidOP.AddKey(key) =>
                          key match
                            case PrismPublicKey.VoidKey(id, reason)                           => tmpSSI
                            case k @ PrismPublicKey.UncompressedECKey(id, usage, curve, x, y) => tmpSSI.addKey(k)
                            case k @ PrismPublicKey.CompressedECKey(id, usage, curve, data)   => tmpSSI.addKey(k)
                        case UpdateDidOP.RemoveKey(keyId)    => tmpSSI.copy(keys = tmpSSI.keys.filter(_.id != keyId))
                        case UpdateDidOP.AddService(service) => tmpSSI.copy(services = services :+ service)
                        case UpdateDidOP.RemoveService(sId)  => tmpSSI.copy(services = services.filter(_.id != sId))
                        case UpdateDidOP.UpdateService(sId, newType, newServiceEndpoints) =>
                          copy(services = services.map {
                            case s @ MyService(id, type_, serviceEndpoint) if id != sId => s
                            case MyService(id, type_, serviceEndpoint) =>
                              if (newType.isEmpty) MyService(id = id, `type` = type_, newServiceEndpoints)
                              else MyService(id = id, `type` = type_, newServiceEndpoints)
                          })
                        case UpdateDidOP.PatchContext(context) => copy(context = context) // replace
                    }
                    .copy(latestHash = Some(spo.opHash))
              case DeactivateDidOP(previousOperationHash, id) =>
                if (!latestHash.contains(previousOperationHash) & self.checkMasterSignature(spo)) self
                else self.copy(latestHash = Some(spo.opHash), disabled = true)
      }.copy(cursor = spo.eventCursor)
  }

  def checkMasterSignature(spo: MySignedPrismOperation[OP]): Boolean =
    (!disabled) & keys
      .find(k => k.usage == PrismKeyUsage.MasterKeyUsage & k.id == spo.signedWith)
      .exists(key => SharedCryto.checkECDSASignature(msg = spo.protobuf.toByteArray, sig = spo.signature, pubKey = key))

  def checkVdrSignature(spo: MySignedPrismOperation[OP]): Boolean =
    (!disabled) & keys
      .find(k => k.usage == PrismKeyUsage.VdrKeyUsage & k.id == spo.signedWith)
      .exists(key => SharedCryto.checkECDSASignature(msg = spo.protobuf.toByteArray, sig = spo.signature, pubKey = key))

  def didDocument: DIDDocument = {
    val authentication = keys
      .filter(_.usage == PrismKeyUsage.AuthenticationKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did.string))
    val assertionMethod = keys
      .filter(_.usage == PrismKeyUsage.IssuingKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did.string))
    val keyAgreement = keys
      .filter(_.usage == PrismKeyUsage.KeyagreementKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did.string))
      .toSet[VerificationMethod]
    val capabilityInvocation = keys
      .filter(_.usage == PrismKeyUsage.CapabilityinvocationKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did.string))
    val capabilityDelegation = keys
      .filter(_.usage == PrismKeyUsage.CapabilitydelegationKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did.string))

    val services = this.services
      .map(s => DIDServiceGeneric(id = s.id, `type` = s.`type`, serviceEndpoint = ast.Json.Str(s.serviceEndpoint)))
      .toSet[DIDService]

    DIDDocumentClass(
      id = this.did,
      alsoKnownAs = None,
      verificationMethod = None,
      authentication = Some(authentication).filter(_.nonEmpty),
      assertionMethod = Some(assertionMethod).filter(_.nonEmpty),
      keyAgreement = Some(keyAgreement).filter(_.nonEmpty),
      capabilityInvocation = Some(capabilityInvocation).filter(_.nonEmpty),
      capabilityDelegation = Some(capabilityDelegation).filter(_.nonEmpty),
      service = Some(services).filter(_.nonEmpty),
    )
  }

  // /** https://github.com/decentralized-identity/universal-resolver/ */
  // def didResolutionResult: DIDResolutionResult = ???

}

object SSI {
  given decoder: JsonDecoder[SSI] = DeriveJsonDecoder.gen[SSI]
  given encoder: JsonEncoder[SSI] = DeriveJsonEncoder.gen[SSI]

  def init(did: DIDSubject) =
    SSI(
      did = did,
      latestHash = None,
      keys = Seq.empty,
      services = Seq.empty,
      context = Seq.empty,
      disabled = false,
      cursor = EventCursor.init
    )

  def make(ssi: DIDSubject, ops: Seq[MySignedPrismOperation[OP]]) =
    ops.foldLeft(SSI.init(ssi)) { case (tmpSSI, op) => tmpSSI.appendAny(op) }

  // def fromCreateEvent(op: MySignedPrismOperation[CreateDidOP | UpdateDidOP | DeactivateDidOP]) =
  def fromCreateEvent(op: MySignedPrismOperation[CreateDidOP]) =
    SSI.init(DIDPrism(op.opHash)).append(op)
}
