package fmgp.crypto

import zio.json._
import fmgp.util.Base64
import fmgp.crypto.SHA256
import fmgp.did.VerificationMethodReferenced
import fmgp.did.SetU
import fmgp.did.comm.KWAlgorithm
import scala.util.Try
import scala.util.Failure
import scala.util.Success

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

  /** This is the JWT in base64JWTFormat encode in Base64 (double encoded) */
  def unsafeFromBase64(base64: Base64): JWT = unsafeFromEncodedJWT(base64.decodeToString)

  /** This is the JWT in base64JWTFormat encode in Base64 (double encoded) */
  def fromBase64(base64: Base64): Either[String, JWT] =
    Try(unsafeFromEncodedJWT(base64.decodeToString)) match // TODO improve
      case Failure(exception) => Left(s"Fail fromBase64 due: $exception")
      case Success(value)     => Right(value)

  extension (jwt: JWT)
    inline def protectedHeader: ProtectedHeaderJWT = jwt._1
    inline def payload: Payload = jwt._2
    inline def signature: SignatureJWT = jwt._3
    def base64JWTFormatWithNoSignature = jwt.protectedHeader.urlBase64 + "." + jwt.payload.urlBase64
    def base64JWTFormat = jwt.base64JWTFormatWithNoSignature + "." + jwt.signature.urlBase64

    /** This is the JWT in base64JWTFormat encode in Base64 (double encoded) */
    def base64: Base64 = Base64.encode(jwt.base64JWTFormat)
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

opaque type JWTPayload = ast.Json.Obj

/** JWT's Payload
  * @see
  *   https://github.com/panva/jose/blob/main/docs/interfaces/types.JWTPayload.md
  */
object JWTPayload {
  def apply(obj: ast.Json.Obj): JWTPayload = obj
  extension (obj: JWTPayload)
    def json: ast.Json.Obj = obj

    /** Issuer https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.1 */
    def getISS: Option[String] = obj.get("iss").flatMap(_.asString)

    /** Subject https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.2 */
    def getSUB: Option[String] = obj.get("sub").flatMap(_.asString)

    /** Audience https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.3 */
    def getAUD: Option[String] = obj.get("aud").flatMap(_.asString)

    /** Expiration Time https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.4 */
    def getEXP: Option[String] = obj.get("exp").flatMap(_.asString)

  given decoder: JsonDecoder[JWTPayload] = ast.Json.Obj.decoder
  given encoder: JsonEncoder[JWTPayload] = ast.Json.Obj.encoder
}
