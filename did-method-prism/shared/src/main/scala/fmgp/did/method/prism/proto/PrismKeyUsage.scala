package fmgp.did.method.prism.proto

import zio.json._
import proto.prism.KeyUsage
import fmgp.util.safeValueOf

enum PrismKeyUsage:
  // case UnknownKeyUsage extends PrismKeyUsage
  case MasterKeyUsage extends PrismKeyUsage
  case IssuingKeyUsage extends PrismKeyUsage
  case KeyagreementKeyUsage extends PrismKeyUsage
  case AuthenticationKeyUsage extends PrismKeyUsage
  case RevocationKeyUsage extends PrismKeyUsage
  case CapabilityinvocationKeyUsage extends PrismKeyUsage
  case CapabilitydelegationKeyUsage extends PrismKeyUsage
  case VdrKeyUsage extends PrismKeyUsage
  // case UnrecognizedKeyUsage extends PrismKeyUsage

object PrismKeyUsage {
  given decoder: JsonDecoder[PrismKeyUsage] = JsonDecoder.string.mapOrFail(e => safeValueOf(PrismKeyUsage.valueOf(e)))
  given encoder: JsonEncoder[PrismKeyUsage] = JsonEncoder.string.contramap((e: PrismKeyUsage) => e.toString)

  type ProtoKeyUsage = KeyUsage.MASTER_KEY.type | KeyUsage.ISSUING_KEY.type | KeyUsage.KEY_AGREEMENT_KEY.type |
    KeyUsage.AUTHENTICATION_KEY.type | KeyUsage.REVOCATION_KEY.type | KeyUsage.CAPABILITY_INVOCATION_KEY.type |
    KeyUsage.CAPABILITY_DELEGATION_KEY.type | KeyUsage.VDR_KEY.type

  def fromProto(p: PrismKeyUsage.ProtoKeyUsage) = p match
    case KeyUsage.MASTER_KEY                => MasterKeyUsage
    case KeyUsage.ISSUING_KEY               => IssuingKeyUsage
    case KeyUsage.KEY_AGREEMENT_KEY         => KeyagreementKeyUsage
    case KeyUsage.AUTHENTICATION_KEY        => AuthenticationKeyUsage
    case KeyUsage.REVOCATION_KEY            => RevocationKeyUsage
    case KeyUsage.CAPABILITY_INVOCATION_KEY => CapabilityinvocationKeyUsage
    case KeyUsage.CAPABILITY_DELEGATION_KEY => CapabilitydelegationKeyUsage
    case KeyUsage.VDR_KEY                   => VdrKeyUsage

}
