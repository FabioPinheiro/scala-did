package fmgp.crypto

import zio.json._
import fmgp.util.Base64
import fmgp.crypto.SHA256
import fmgp.did.VerificationMethodReferenced
import fmgp.did.SetU
import fmgp.did.comm.KWAlgorithm

// ###########
// ### JWT ###
// ###########

opaque type Payload = Base64
object Payload:
  def fromBase64url(data: String): Payload = Base64.fromBase64url(data)
  extension (data: Payload)
    /** decode the base64url to a string */
    def content: String = data.decodeToString
    def base64url: String = data.urlBase64
    def base64: Base64 = data
  given decoder: JsonDecoder[Payload] = Base64.decoder
  given encoder: JsonEncoder[Payload] = Base64.encoder

/** JWM_SIGNATURE is a Base64 url encode */
opaque type SignatureJWM = String
object SignatureJWM:
  def apply(value: String): SignatureJWM = value
  extension (signature: SignatureJWM)
    def value: String = signature
    def base64: Base64 = Base64.fromBase64url(signature.value)

  given decoder: JsonDecoder[SignatureJWM] = JsonDecoder.string.map(SignatureJWM(_))
  given encoder: JsonEncoder[SignatureJWM] = JsonEncoder.string.contramap[SignatureJWM](_.value)

opaque type JWT = (ProtectedHeaderJWT, Payload, SignatureJWT)
object JWT {

  def apply(protectedHeader: ProtectedHeaderJWT, payload: Payload, signature: SignatureJWT): JWT =
    (protectedHeader, payload, signature)

  def fromStrings(protectedHeader: String, payload: String, signature: String): JWT =
    apply(
      ProtectedHeaderJWT.fromBase64url(protectedHeader),
      Payload.fromBase64url(payload),
      SignatureJWT.fromBase64url(signature)
    )

  def unsafeFromEncodedJWT(str: String): JWT = str.split('.') match {
    case Array(protectedHeader: String, payload: String, signature: String) =>
      fromStrings(protectedHeader, payload, signature)
  }
  extension (jwt: JWT)
    inline def protectedHeader: ProtectedHeaderJWT = jwt._1
    inline def payload: Payload = jwt._2
    inline def signature: SignatureJWT = jwt._3
    def base64JWTFormatWithNoSignature = jwt.protectedHeader.urlBase64 + "." + jwt.payload.urlBase64
    def base64JWTFormat = jwt.base64JWTFormatWithNoSignature + "." + jwt.signature.urlBase64
    // def base64: Base64 = Base64.fromBase64url(jwt.value)
}

opaque type ProtectedHeaderJWT = Base64
object ProtectedHeaderJWT {
  def fromBase64url(data: String): Payload = Base64.fromBase64url(data)
  extension (header: ProtectedHeaderJWT)
    /** decode the base64url to a string */
    def content: String = header.decodeToString
    def base64url: String = header.urlBase64
    def base64: Base64 = header
    def toJWTHeader: Either[String, JWTHeader] = header.asObj[JWTHeader].map(_.obj)
}

// ###########
// ### JWS ###
// ###########

opaque type SignatureJWT = Base64
object SignatureJWT {
  def fromBase64url(data: String): Payload = Base64.fromBase64url(data)
  extension (signature: SignatureJWT)
    /** decode the base64url to a string */
    def content: String = signature.decodeToString
    def base64url: String = signature.urlBase64
    def base64: Base64 = signature

}

// #############
// ### Utils ###
// #############

final case class JWTHeader(
    alg: Option[JWAAlgorithm], // Algorithm
    jku: Option[String], // JWK Set URL
    jwk: Option[String], // JSON Web Key
    kid: Option[String], // Key ID
    // x5u: Option[String], // X.509 URL
    // x5c: Option[String], // X.509 Certificate Chain
    // x5t: Option[String], // X.509 Certificate SHA-1 Thumbprint
    // x5t#S256: Option[String], // X.509 Certificate SHA-256 Thumbprint
    typ: Option[String], // Type
    cty: Option[String], // Content Type
    crit: Option[Seq[String]], // Critical
)
object JWTHeader {
  given decoder: JsonDecoder[JWTHeader] = DeriveJsonDecoder.gen[JWTHeader]
  given encoder: JsonEncoder[JWTHeader] = DeriveJsonEncoder.gen[JWTHeader]
}

/** @see
  *   based on https://github.com/panva/jose/blob/main/docs/interfaces/types.JWTPayload.md
  */
final case class JWTPayload(
    aud: Option[SetU[String]],
    exp: Option[Long],
    iat: Option[Long],
    iss: Option[String],
    jti: Option[String],
    nbf: Option[Long],
    sub: Option[String],
)
object JWTPayload {
  import fmgp.did.SetU.given
  given decoder: JsonDecoder[JWTPayload] = DeriveJsonDecoder.gen[JWTPayload]
  given encoder: JsonEncoder[JWTPayload] = DeriveJsonEncoder.gen[JWTPayload]
}
