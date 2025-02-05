package fmgp.prism

import fmgp.prism.PrismPublicKey.UncompressedECKey
import fmgp.prism.PrismPublicKey.CompressedECKey
import fmgp.prism.PrismPublicKey.VoidKey
import scala.util.chaining._
import zio.json._
import fmgp.did._

final case class SSI(
    did: String,
    latestHash: Option[String],
    keys: Seq[UncompressedECKey | CompressedECKey],
    services: Seq[MyService],
    context: Seq[String],
    disabled: Boolean
) { self =>
  def appendAny(spo: MySignedPrismOperation[OP]): SSI = spo.operation match
    case _: CreateDidOP     => append(spo.asInstanceOf[MySignedPrismOperation[CreateDidOP]])
    case _: UpdateDidOP     => append(spo.asInstanceOf[MySignedPrismOperation[UpdateDidOP]])
    case _: DeactivateDidOP => append(spo.asInstanceOf[MySignedPrismOperation[DeactivateDidOP]])
    case _                  => self

  private def addKey(k: PrismPublicKey): SSI = k match
    case _: VoidKey           => self
    case k: UncompressedECKey => addKey(k)
    case k: CompressedECKey   => addKey(k)

  private def addKey(k: UncompressedECKey | CompressedECKey): SSI =
    if (keys.exists(_.id == k.id)) self //  ID must be unique
    else self.copy(keys = this.keys :+ k)

  def append(spo: MySignedPrismOperation[CreateDidOP | UpdateDidOP | DeactivateDidOP]): SSI = {
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
                        case VoidKey(id, reason)                           => tmpSSI
                        case k @ UncompressedECKey(id, usage, curve, x, y) => tmpSSI.addKey(k)
                        case k @ CompressedECKey(id, usage, curve, data)   => tmpSSI.addKey(k)
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
  }

  def checkMasterSignature(spo: MySignedPrismOperation[OP]): Boolean =
    (!disabled) & keys
      .find(k => k.usage == PrismKeyUsage.MasterKeyUsage & k.id == spo.signedWith)
      .exists(key => SharedCryto.checkECDSASignature(msg = spo.protobuf.toByteArray, sig = spo.signature, pubKey = key))

  def didDocument: DIDDocument = {

    val authentication = keys
      .filter(_.usage == PrismKeyUsage.AuthenticationKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did))
    val assertionMethod = keys
      .filter(_.usage == PrismKeyUsage.IssuingKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did))
    val keyAgreement = keys
      .filter(_.usage == PrismKeyUsage.KeyagreementKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did))
      .toSet[VerificationMethod]
    val capabilityInvocation = keys
      .filter(_.usage == PrismKeyUsage.CapabilityinvocationKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did))
    val capabilityDelegation = keys
      .filter(_.usage == PrismKeyUsage.CapabilitydelegationKeyUsage)
      .map(k => k.getVerificationMethod(id = s"$did#${k.id}", controller = did))

    val services = this.services
      .map(s => DIDServiceGeneric(id = s.id, `type` = s.`type`, serviceEndpoint = ast.Json.Str(s.serviceEndpoint)))
      .toSet[DIDService]

    DIDDocumentClass(
      id = DIDSubject(this.did),
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
}

object SSI {
  given decoder: JsonDecoder[SSI] = DeriveJsonDecoder.gen[SSI]
  given encoder: JsonEncoder[SSI] = DeriveJsonEncoder.gen[SSI]

  def init(did: String) = {
    assert(did.startsWith("did:prism:"))
    SSI(did = did, latestHash = None, keys = Seq.empty, services = Seq.empty, context = Seq.empty, disabled = false)
  }

  def make(ssi: String, ops: Seq[MySignedPrismOperation[OP]]) =
    ops.foldLeft(SSI.init(ssi)) { case (tmpSSI, op) => tmpSSI.appendAny(op) }
}
