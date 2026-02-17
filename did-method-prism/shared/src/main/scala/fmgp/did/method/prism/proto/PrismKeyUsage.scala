package fmgp.did.method.prism.proto

import zio.json.*
import proto.prism.KeyUsage
import fmgp.util.safeValueOf

//TODO move to fmgp.did.method.prism
enum PrismKeyUsage(val protoEnum: Int, val name: String):
  // case UnknownKeyUsage extends PrismKeyUsage
  case MasterKeyUsage extends PrismKeyUsage(1, "Master")
  case IssuingKeyUsage extends PrismKeyUsage(2, "Issuing")
  case KeyAgreementKeyUsage extends PrismKeyUsage(3, "KeyAgreement")
  case AuthenticationKeyUsage extends PrismKeyUsage(4, "Authentication")
  case RevocationKeyUsage extends PrismKeyUsage(5, "Revocation")
  case CapabilityinvocationKeyUsage extends PrismKeyUsage(6, "CapabilityInvocation")
  case CapabilitydelegationKeyUsage extends PrismKeyUsage(7, "CapabilityDelegation")
  case VdrKeyUsage extends PrismKeyUsage(8, "VDR")
  // case UnrecognizedKeyUsage extends PrismKeyUsage()

object PrismKeyUsage {
  given decoder: JsonDecoder[PrismKeyUsage] = JsonDecoder.string.mapOrFail(e => safeValueOf(PrismKeyUsage.valueOf(e)))
  given encoder: JsonEncoder[PrismKeyUsage] = JsonEncoder.string.contramap((e: PrismKeyUsage) => e.toString)

  type ProtoKeyUsage = KeyUsage.MASTER_KEY.type | KeyUsage.ISSUING_KEY.type | KeyUsage.KEY_AGREEMENT_KEY.type |
    KeyUsage.AUTHENTICATION_KEY.type | KeyUsage.REVOCATION_KEY.type | KeyUsage.CAPABILITY_INVOCATION_KEY.type |
    KeyUsage.CAPABILITY_DELEGATION_KEY.type | KeyUsage.VDR_KEY.type

  def fromProto(p: PrismKeyUsage.ProtoKeyUsage) = p match
    case KeyUsage.MASTER_KEY                => MasterKeyUsage
    case KeyUsage.ISSUING_KEY               => IssuingKeyUsage
    case KeyUsage.KEY_AGREEMENT_KEY         => KeyAgreementKeyUsage
    case KeyUsage.AUTHENTICATION_KEY        => AuthenticationKeyUsage
    case KeyUsage.REVOCATION_KEY            => RevocationKeyUsage
    case KeyUsage.CAPABILITY_INVOCATION_KEY => CapabilityinvocationKeyUsage
    case KeyUsage.CAPABILITY_DELEGATION_KEY => CapabilitydelegationKeyUsage
    case KeyUsage.VDR_KEY                   => VdrKeyUsage

}
