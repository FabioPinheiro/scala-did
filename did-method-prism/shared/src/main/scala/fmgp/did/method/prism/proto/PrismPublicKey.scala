package fmgp.did.method.prism.proto

import zio.json._
import proto.prism.PublicKey
import proto.prism.ECKeyData
import proto.prism.CompressedECKeyData
import fmgp.did.VerificationMethodReferenced
import fmgp.did.VerificationMethodEmbeddedJWK
import fmgp.did.VerificationMethodEmbeddedMultibase
import fmgp.util.hex2bytes
import fmgp.crypto.SharedCryto

sealed trait PrismPublicKey { def id: String }
object PrismPublicKey {
  import proto.prism.PublicKey.KeyData
  import proto.prism.KeyUsage.UNKNOWN_KEY
  import proto.prism.KeyUsage.Unrecognized

  given decoder: JsonDecoder[PrismPublicKey] = {
    import fmgp.util.decoderByteArray
    DeriveJsonDecoder.gen[PrismPublicKey]
  }
  given encoder: JsonEncoder[PrismPublicKey] = {
    import fmgp.util.encoderByteArray
    DeriveJsonEncoder.gen[PrismPublicKey]
  }

  given decoderECKey: JsonDecoder[UncompressedECKey | CompressedECKey] = decoder.mapOrFail {
    case VoidKey(id, reason)  => Left("Expecting UncompressedECKey | CompressedECKey instead of VoidKey")
    case k: UncompressedECKey => Right(k)
    case k: CompressedECKey   => Right(k)
  }
  given encoderECKey: JsonEncoder[UncompressedECKey | CompressedECKey] = encoder.narrow

  def filterECKey(keys: Seq[PrismPublicKey]): Seq[UncompressedECKey | CompressedECKey] =
    keys
      .flatMap[UncompressedECKey | CompressedECKey] { // filter
        case VoidKey(id, reason)                           => None
        case k @ UncompressedECKey(id, usage, curve, x, y) => Some(k)
        case k @ CompressedECKey(id, usage, curve, data)   => Some(k)
      }

  case class VoidKey(id: String, reason: String) extends PrismPublicKey

  sealed trait PrismPublicKey_TMP extends PrismPublicKey { self =>
    import fmgp.did._
    def usage: PrismKeyUsage
    def curve: String

    def publicKey: fmgp.crypto.PublicKeyWithoutKid

    def getVerificationMethod(id: DIDURLSyntax, controller: DIDController): VerificationMethodEmbeddedJWK =
      VerificationMethodEmbeddedJWK(
        id = id,
        controller = controller,
        `type` = VerificationMethodType.JsonWebKey2020,
        expires = None,
        revoked = None,
        publicKeyJwk = publicKey.withKid(id),
        secretKeyJwk = None,
      )
  }
  case class UncompressedECKey(id: String, usage: PrismKeyUsage, curve: String, x: Array[Byte], y: Array[Byte])
      extends PrismPublicKey
      with PrismPublicKey_TMP {
    import fmgp.crypto._
    def publicKey: fmgp.crypto.ECPublicKeyWithoutKid =
      ECPublicKeyWithoutKid(
        kty = KTY.EC,
        crv = fmgp.crypto.ECCurve.valueOf(curve).getOrElse(???), // FIXME
        x = fmgp.util.Base64.encode(x).urlBase64,
        y = fmgp.util.Base64.encode(y).urlBase64,
      )
  }
  case class CompressedECKey(id: String, usage: PrismKeyUsage, curve: String, data: Array[Byte])
      extends PrismPublicKey
      with PrismPublicKey_TMP {
    import fmgp.crypto._
    def publicKey: fmgp.crypto.PublicKeyWithoutKid = {

      fmgp.crypto.Curve.valueOf(curve) match
        case c: fmgp.crypto.ECCurve =>
          val (x, y) = SharedCryto.getXY(data).getOrElse(???)
          ECPublicKeyWithoutKid(
            kty = KTY.EC,
            crv = fmgp.crypto.ECCurve.valueOf(curve).getOrElse(???), // FIXME
            x = fmgp.util.Base64.encode(data.slice(1, 33)).urlBase64, // TODO CHECK this
            y = fmgp.util.Base64.encode(hex2bytes(y)).urlBase64,
          )
        case c: fmgp.crypto.OKPCurve =>
          OKPPublicKeyWithoutKid(kty = KTY.OKP, crv = c, x = fmgp.util.Base64.encode(data).urlBase64)
    }

  }

  def fromProto(key: proto.prism.PublicKey) = {
    key match
      case PublicKey("", usage, keyData, unknownFields) => VoidKey(id = "", "PublicKey id is empty")
      case PublicKey(id, UNKNOWN_KEY, _, unknownFields) => VoidKey(id = id, "PublicKey purpose is missing")
      case PublicKey(id, Unrecognized(unrecognizedValue), _, unknownFields) =>
        VoidKey(id = id, s"PublicKey purpose is not Unrecognized ($unrecognizedValue)")
      case PublicKey(id, usage: PrismKeyUsage.ProtoKeyUsage, KeyData.Empty, unknownFields) =>
        VoidKey(id = id, "PublicKey data is missing")
      case PublicKey(id, usage: PrismKeyUsage.ProtoKeyUsage, KeyData.EcKeyData(value), unknownFields) =>
        value match
          case ECKeyData("", x, y, unknownFields) =>
            VoidKey(id = id, "PublicKey x or y is missing in ECKeyData")
          case ECKeyData(curve, x, y, unknownFields) =>
            if (x.size == 0 || y.size == 0) VoidKey(id = id, "PublicKey curve is missing in ECKeyData")
            UncompressedECKey(
              id = id,
              usage = PrismKeyUsage.fromProto(usage),
              curve = curve,
              x = x.toByteArray(),
              y = y.toByteArray()
            )
      case PublicKey(id, usage: PrismKeyUsage.ProtoKeyUsage, KeyData.CompressedEcKeyData(value), unknownFields) =>
        value match
          case CompressedECKeyData("", data, unknownFields) =>
            VoidKey(id = id, "PublicKey curve is missing in CompressedECKeyData")
          case CompressedECKeyData(curve, data, unknownFields) =>
            if (data.size == 0) VoidKey(id = id, "PublicKey data is missing in CompressedECKeyData")
            CompressedECKey(
              id = id,
              usage = PrismKeyUsage.fromProto(usage),
              curve = curve,
              data = data.toByteArray()
            )
  }
}
