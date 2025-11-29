package fmgp.did.method.prism.node

import fmgp.util.Base64
import proto.prism.*
import proto.prism.CompressedECKeyData
import proto.prism.ECKeyData
import proto.prism.PublicKey.KeyData
import proto.prism.PublicKey.KeyData.CompressedEcKeyData
import proto.prism.PublicKey.KeyData.EcKeyData
import proto.prism.Service
import proto.prism.node.DIDData
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.proto.*

extension (ssi: SSI) {
  def didData: Option[DIDData] = Option.unless(ssi.disabled) {
    DIDData(
      id = ssi.did.string,
      publicKeys = ssi.keys.map(k => SSIExtension.toPublicKey(k)),
      services = ssi.services.map(s => SSIExtension.toService(s)),
      context = ssi.context,
      unknownFields = scalapb.UnknownFieldSet.empty
    )
  }
}

object SSIExtension {

  def toPublicKey(key: PrismPublicKey): PublicKey = {
    PublicKey(
      id = key.id,
      usage = toKeyUsage(key),
      keyData = toKeyData(key),
      unknownFields = scalapb.UnknownFieldSet.empty
    )
  }

  def toKeyUsage(key: PrismPublicKey): KeyUsage = {
    key match {
      case k: PrismPublicKey.UncompressedECKey =>
        k.usage match {
          case PrismKeyUsage.MasterKeyUsage               => KeyUsage.MASTER_KEY
          case PrismKeyUsage.IssuingKeyUsage              => KeyUsage.ISSUING_KEY
          case PrismKeyUsage.KeyagreementKeyUsage         => KeyUsage.KEY_AGREEMENT_KEY
          case PrismKeyUsage.RevocationKeyUsage           => KeyUsage.REVOCATION_KEY
          case PrismKeyUsage.AuthenticationKeyUsage       => KeyUsage.AUTHENTICATION_KEY
          case PrismKeyUsage.CapabilityinvocationKeyUsage => KeyUsage.CAPABILITY_INVOCATION_KEY
          case PrismKeyUsage.CapabilitydelegationKeyUsage => KeyUsage.CAPABILITY_DELEGATION_KEY
          case PrismKeyUsage.VdrKeyUsage                  => KeyUsage.VDR_KEY
        }
      case k: PrismPublicKey.CompressedECKey =>
        k.usage match {
          case PrismKeyUsage.MasterKeyUsage               => KeyUsage.MASTER_KEY
          case PrismKeyUsage.IssuingKeyUsage              => KeyUsage.ISSUING_KEY
          case PrismKeyUsage.KeyagreementKeyUsage         => KeyUsage.KEY_AGREEMENT_KEY
          case PrismKeyUsage.RevocationKeyUsage           => KeyUsage.REVOCATION_KEY
          case PrismKeyUsage.AuthenticationKeyUsage       => KeyUsage.AUTHENTICATION_KEY
          case PrismKeyUsage.CapabilityinvocationKeyUsage => KeyUsage.CAPABILITY_INVOCATION_KEY
          case PrismKeyUsage.CapabilitydelegationKeyUsage => KeyUsage.CAPABILITY_DELEGATION_KEY
          case PrismKeyUsage.VdrKeyUsage                  => KeyUsage.VDR_KEY
        }
      case k: PrismPublicKey.VoidKey => ???
    }
  }

  def toKeyData(key: PrismPublicKey): KeyData = {
    key match {
      case k: PrismPublicKey.UncompressedECKey =>
        KeyData.EcKeyData(
          ECKeyData(
            curve = k.publicKey.crv.toString,
            x = com.google.protobuf.ByteString.copyFrom(Base64.urlDecoder.decode(k.publicKey.x)),
            y = com.google.protobuf.ByteString.copyFrom(Base64.urlDecoder.decode(k.publicKey.y))
          )
        )
      case k: PrismPublicKey.CompressedECKey =>
        KeyData.CompressedEcKeyData(
          CompressedECKeyData(
            curve = k.publicKey.crv.toString,
            data = com.google.protobuf.ByteString.copyFrom(Base64.urlDecoder.decode(k.publicKey.x))
          )
        )
      case k: PrismPublicKey.VoidKey => ???
    }
  }
  // TODO:  MyService name should be renamed to PrismService
  def toService(service: MyService): Service = {
    Service(
      id = service.id,
      `type` = service.`type`,
      serviceEndpoint = service.serviceEndpoint
    )
  }
}
