package fmgp.did

import zio.json.*
import zio.json.ast.Json
import zio.json.ast.JsonCursor
import fmgp.crypto.PublicKey
import fmgp.crypto.OKPPublicKey
import fmgp.did.comm.FROMTO

/** MULTIBASE encoded public key.- https://datatracker.ietf.org/doc/html/draft-multiformats-multibase-03 */
type MULTIBASE = String // TODO

/** VerificationMethod
  *
  * https://w3c-ccg.github.io/security-vocab/#verificationMethod
  * https://w3c.github.io/did-core/#verification-method-properties
  * https://www.w3.org/TR/did-core/#verification-method-properties
  */
sealed trait VerificationMethod {
  def id: DIDURLSyntax
}

object VerificationMethod {
  given decoder: JsonDecoder[VerificationMethod] =
    VerificationMethodEmbedded.decoder.map(e => e).orElse(VerificationMethodReferenced.decoder.map(e => e))
  given encoder: JsonEncoder[VerificationMethod] = {
    VerificationMethodReferenced.encoder
      .orElseEither(VerificationMethodEmbedded.encoder)
      .contramap {
        case l: VerificationMethodReferenced => Left(l)
        case r: VerificationMethodEmbedded   => Right(r)
      }
  }
}

// enum VerificationMethodType:
//   case Multikey extends VerificationMethodType

//   /** @see https://www.w3.org/TR/controller-document/#jsonwebkey */
//   case JsonWebKey extends VerificationMethodType
//   case JsonWebKey2020 extends VerificationMethodType

//   case EcdsaSecp256k1VerificationKey2019 extends VerificationMethodType

//   /** @see https://www.w3.org/TR/vc-di-eddsa/#ed25519verificationkey2020 */
//   case Ed25519VerificationKey2020 extends VerificationMethodType

//   /** @see https://www.w3.org/TR/vc-di-eddsa/#ed25519signature2020 */
//   case Ed25519Signature2020 extends VerificationMethodType

//   case X25519KeyAgreementKey2019 extends VerificationMethodType
//   case X25519KeyAgreementKey2020 extends VerificationMethodType

// object VerificationMethodType {
//   given decoder: JsonDecoder[VerificationMethodType] =
//     JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(VerificationMethodType.valueOf(e)))
//   given encoder: JsonEncoder[VerificationMethodType] =
//     JsonEncoder.string.contramap((e: VerificationMethodType) => e.toString)
// }

type VerificationMethodType = String
object VerificationMethodType {
  val Multikey = "Multikey"
  val JsonWebKey = "JsonWebKey"
  val JsonWebKey2020 = "JsonWebKey2020"
  val EcdsaSecp256k1VerificationKey2019 = "EcdsaSecp256k1VerificationKey2019"
  val Ed25519VerificationKey2020 = "Ed25519VerificationKey2020"
  val Ed25519Signature2020 = "Ed25519Signature2020"
  val X25519KeyAgreementKey2019 = "X25519KeyAgreementKey2019"
  val X25519KeyAgreementKey2020 = "X25519KeyAgreementKey2020"
}

case class VerificationMethodReferencedWithKey[K <: fmgp.crypto.OKP_EC_Key](kid: String, key: K) {
  def vmr = VerificationMethodReferenced(kid)
  def pair = (vmr, key) // TODO REMOVE
}
object VerificationMethodReferencedWithKey {
  def from[K <: fmgp.crypto.OKP_EC_Key](vmr: VerificationMethodReferenced, key: K) =
    VerificationMethodReferencedWithKey(vmr.value, key)
}

case class VerificationMethodReferenced(value: String) extends VerificationMethod {
  def did = DIDSubject(value.split('#').head)
  def id = value // TODO rename value to id
  def fragment = value.split("#", 2).drop(1).head // TODO make it type safe
}
object VerificationMethodReferenced {
  given decoder: JsonDecoder[VerificationMethodReferenced] = JsonDecoder.string.map(VerificationMethodReferenced.apply)
  given encoder: JsonEncoder[VerificationMethodReferenced] = JsonEncoder.string.contramap(_.value)

  // These given are useful if we use the VerificationMethodReferenced as a Key (ex: Map[VerificationMethodReferenced , Value])
  given JsonFieldDecoder[VerificationMethodReferenced] = JsonFieldDecoder.string.map(VerificationMethodReferenced.apply)
  given JsonFieldEncoder[VerificationMethodReferenced] = JsonFieldEncoder.string.contramap(_.value)
}

sealed trait VerificationMethodEmbedded extends VerificationMethod {
  def id: Required[DIDURLSyntax] // "did:example:123456789abcdefghi#keys-1",
  def controller: Required[DIDController] // "did:example:123456789abcdefghi",
  def `type`: Required[VerificationMethodType] // "Ed25519VerificationKey2020",

  /** @see https://w3id.org/security#revoked */
  def expires: NotRequired[DateTimeStamp]
  def revoked: NotRequired[DateTimeStamp]

  // def publicKeyJwk: NotRequired[JSONWebKeyMap]
  // def publicKeyMultibase: NotRequired[MULTIBASE]
  /** this is a Either publicKeyJwk or a publicKeyMultibase */
  def publicKey: Either[MULTIBASE, PublicKey]
}

/** VerificationMethodEmbeddedJWK
  * @see
  *   https://www.w3.org/community/reports/credentials/CG-FINAL-lds-jws2020-20220721/#json-web-key-2020
  *
  * @param publicKeyJwk
  *   is a JSON Web Key (JWK) - RFC7517 - https://www.rfc-editor.org/rfc/rfc7517
  */
case class VerificationMethodEmbeddedJWK(
    id: Required[DIDURLSyntax],
    controller: Required[DIDController],
    `type`: Required[VerificationMethodType],
    expires: NotRequired[DateTimeStamp] = None,
    revoked: NotRequired[DateTimeStamp] = None,
    publicKeyJwk: Required[PublicKey],
    secretKeyJwk: NotRequired[PublicKey] = None,
) extends VerificationMethodEmbedded {
  def publicKey = Right(publicKeyJwk)
}

object VerificationMethodEmbeddedJWK {
  given decoder: JsonDecoder[VerificationMethodEmbeddedJWK] =
    DeriveJsonDecoder.gen[VerificationMethodEmbeddedJWK]
  given encoder: JsonEncoder[VerificationMethodEmbeddedJWK] =
    DeriveJsonEncoder.gen[VerificationMethodEmbeddedJWK]
}

/** VerificationMethodEmbeddedMultibase
  *
  * TODO RENAME to MultiKey
  * @see
  *   https://www.w3.org/TR/vc-data-integrity/#multikey
  *   https://w3c.github.io/vc-data-integrity/contexts/multikey/v1.jsonld
  */
case class VerificationMethodEmbeddedMultibase(
    id: Required[DIDURLSyntax],
    controller: Required[DIDController],
    `type`: Required[VerificationMethodType],
    expires: NotRequired[DateTimeStamp] = None,
    revoked: NotRequired[DateTimeStamp] = None,
    publicKeyMultibase: Required[MULTIBASE],
    secretKeyMultibase: NotRequired[MULTIBASE] = None,
) extends VerificationMethodEmbedded {
  def publicKey = Left(publicKeyMultibase)
}

object VerificationMethodEmbeddedMultibase {
  given decoder: JsonDecoder[VerificationMethodEmbeddedMultibase] =
    DeriveJsonDecoder.gen[VerificationMethodEmbeddedMultibase]
  given encoder: JsonEncoder[VerificationMethodEmbeddedMultibase] =
    DeriveJsonEncoder.gen[VerificationMethodEmbeddedMultibase]
}

object VerificationMethodEmbedded {
  given decoder: JsonDecoder[VerificationMethodEmbedded] =
    Json.Obj.decoder.mapOrFail { originalAst =>
      if (originalAst.fields.exists(e => e._1 == "publicKeyJwk"))
        VerificationMethodEmbeddedJWK.decoder.decodeJson(originalAst.toJson)
      else // publicKeyMultibase
        VerificationMethodEmbeddedMultibase.decoder.decodeJson(originalAst.toJson)
    }

  given encoder: JsonEncoder[VerificationMethodEmbedded] = new JsonEncoder[VerificationMethodEmbedded] {
    override def unsafeEncode(
        b: VerificationMethodEmbedded,
        indent: Option[Int],
        out: zio.json.internal.Write
    ): Unit = b match {
      case obj: VerificationMethodEmbeddedJWK =>
        VerificationMethodEmbeddedJWK.encoder.unsafeEncode(obj, indent, out)
      case obj: VerificationMethodEmbeddedMultibase =>
        VerificationMethodEmbeddedMultibase.encoder.unsafeEncode(obj, indent, out)
    }
  }
}
