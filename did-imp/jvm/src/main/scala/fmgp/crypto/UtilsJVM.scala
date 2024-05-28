package fmgp.crypto

import scala.concurrent.Future
import scala.util.Try
import scala.util.chaining._
import scala.jdk.CollectionConverters._

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.{Payload => JosePayload}
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.StandardCharset

import zio.json._

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm.EncryptedMessageGeneric
import fmgp.did.comm._
import fmgp.util._
import fmgp.crypto.UtilsJVM.toJWK
import fmgp.crypto.error._

given Conversion[Base64Obj[ProtectedHeader], JWEHeader] with
  def apply(x: Base64Obj[ProtectedHeader]) = {
    val encryptionMethod = x.obj.enc match
      case ENCAlgorithm.XC20P           => EncryptionMethod.XC20P
      case ENCAlgorithm.A256GCM         => EncryptionMethod.A256GCM
      case ENCAlgorithm.`A256CBC-HS512` => EncryptionMethod.A256CBC_HS512

    val algorithm = x.obj.alg match
      case KWAlgorithm.`ECDH-ES+A256KW`  => JWEAlgorithm.ECDH_ES_A256KW
      case KWAlgorithm.`ECDH-1PU+A256KW` => JWEAlgorithm.ECDH_1PU_A256KW

    x match
      case Base64Obj(_, Some(original)) =>
        JWEHeader.parse(Base64URL.from(original.urlBase64))
      case Base64Obj(obj, None) =>
        obj match
          case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
            val aux = new JWEHeader.Builder(algorithm, encryptionMethod)
              .agreementPartyVInfo(apv.base64)
              .ephemeralPublicKey(epk.toJWK)
            typ.map(e => aux.`type`(JOSEObjectType(e.typ)))
            aux.build()
          case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
            val aux = new JWEHeader.Builder(algorithm, encryptionMethod)
              .agreementPartyVInfo(apv.base64)
              .ephemeralPublicKey(epk.toJWK)
              .senderKeyID(skid.value)
              .agreementPartyUInfo(apu.base64)
            typ.map(e => aux.`type`(JOSEObjectType(e.typ)))
            aux.build()
  }

given Conversion[Base64, com.nimbusds.jose.util.Base64URL] with
  def apply(x: Base64) = new com.nimbusds.jose.util.Base64URL(x.urlBase64)

object UtilsJVM {

  object unsafe {

    given Conversion[ENCAlgorithm, EncryptionMethod] with
      def apply(x: ENCAlgorithm) = {
        x match
          case ENCAlgorithm.XC20P           => EncryptionMethod.XC20P
          case ENCAlgorithm.A256GCM         => EncryptionMethod.A256GCM
          case ENCAlgorithm.`A256CBC-HS512` => EncryptionMethod.A256CBC_HS512
      }

    /** Don't import this by default */
    given Conversion[ProtectedHeader, JWEHeader] with
      def apply(x: ProtectedHeader) = {
        val encryptionMethod = x.enc match
          case ENCAlgorithm.XC20P           => EncryptionMethod.XC20P
          case ENCAlgorithm.A256GCM         => EncryptionMethod.A256GCM
          case ENCAlgorithm.`A256CBC-HS512` => EncryptionMethod.A256CBC_HS512

        val algorithm = x.alg match
          case KWAlgorithm.`ECDH-ES+A256KW`  => JWEAlgorithm.ECDH_ES_A256KW
          case KWAlgorithm.`ECDH-1PU+A256KW` => JWEAlgorithm.ECDH_1PU_A256KW

        x match {
          case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
            val aux = new JWEHeader.Builder(algorithm, encryptionMethod)
              .agreementPartyVInfo(apv.base64)
              .ephemeralPublicKey(epk.toJWK)
            typ.map(e => aux.`type`(JOSEObjectType(e.typ)))
            aux.build()
          case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
            val aux = new JWEHeader.Builder(algorithm, encryptionMethod)
              .agreementPartyVInfo(apv.base64)
              .ephemeralPublicKey(epk.toJWK)
              .senderKeyID(skid.value)
              .agreementPartyUInfo(apu.base64)
            typ.map(e => aux.`type`(JOSEObjectType(e.typ)))
            aux.build()
        }
      }
  }

  type Base64URLString = String // FIXME

  extension (alg: JWAAlgorithm) {
    def toJWSAlgorithm = alg match {
      case JWAAlgorithm.ES256K => JWSAlgorithm.ES256K
      case JWAAlgorithm.ES256  => JWSAlgorithm.ES256
      case JWAAlgorithm.ES384  => JWSAlgorithm.ES384
      case JWAAlgorithm.ES512  => JWSAlgorithm.ES512
      case JWAAlgorithm.EdDSA  => JWSAlgorithm.EdDSA
    }
  }
  extension (curve: Curve) {
    def toJWKCurve = curve match {
      case Curve.`P-256`   => JWKCurve.P_256
      case Curve.`P-384`   => JWKCurve.P_384
      case Curve.`P-521`   => JWKCurve.P_521
      case Curve.secp256k1 => JWKCurve.SECP256K1
      case Curve.X25519    => JWKCurve.X25519
      case Curve.Ed25519   => JWKCurve.Ed25519
    }
  }

  // extension (ecKey: JWKECKey) {
  //   def verify(jwm: SignedMessage, alg: JWAAlgorithm): Boolean =
  //   def sign(payload: Array[Byte], alg: JWAAlgorithm): SignedMessage =
  // }

  def ecKeyVerifyJWM(ecKey: JWKECKey, jwm: SignedMessage, alg: JWAAlgorithm): Boolean = {
    val maybeKeyID = Option(ecKey.getKeyID())
    val _key = ecKey.toPublicJWK
    val verifier = new ECDSAVerifier(_key.toPublicJWK)
    val signatureObjs: Seq[JWMSignatureObj] = maybeKeyID match
      case None => jwm.signatures // Try all signatures
      case Some(keyId) =>
        jwm.signatures
          .filter(_.`protected`.obj.kid match {
            case None                                      => true // Try all signatures that does not specify the key
            case Some(VerificationMethodReferenced(value)) => keyId == value // Try this signature
          })
    signatureObjs.exists { obj =>
      val base64noSignature = obj.`protected`.base64url + "." + jwm.payload.base64url
      val header = {
        val h = new JWSHeader.Builder(obj.`protected`.obj.alg.toJWSAlgorithm)
        maybeKeyID.foreach(h.keyID(_))
        h.build()
      }
      verifier.verify(
        header,
        (base64noSignature).getBytes(StandardCharset.UTF_8),
        obj.signature.base64
      )
    }
  }

  // TODO return ZIO
  def ecKeyVerifyJWT(ecKey: ECKey, jwt: JWT): Boolean =
    ecKeyVerifyJWT(ecKey.toJWK, jwt)

  def ecKeyVerifyJWT(ecKey: JWKECKey, jwt: JWT): Boolean = {
    for {
      headerJson <- jwt.protectedHeader.content.fromJson[ast.Json.Obj]
      alg <- headerJson.get("alg").map(_.asString) match
        case None              => Left("The field 'alg' must exist")
        case Some(None)        => Left("The field 'alg' must be of the type String")
        case Some(Some(value)) => Right(value)
      algorithmTmp <- safeValueOf(JWAAlgorithm.valueOf(alg))
      algorithm <- algorithmTmp match
        case JWAAlgorithm.ES256K => Right(algorithmTmp)
        case JWAAlgorithm.ES256  => Right(algorithmTmp)
        case JWAAlgorithm.ES384  => Right(algorithmTmp)
        case JWAAlgorithm.ES512  => Right(algorithmTmp)
        case JWAAlgorithm.EdDSA  => Left(s"This method do not support algorithm '$algorithmTmp'")
      header = {
        val h = new JWSHeader.Builder(algorithm.toJWSAlgorithm)
        Option(ecKey.getKeyID()).foreach(h.keyID(_))
        h.build()
      }
      ret = new ECDSAVerifier(ecKey.toPublicJWK.toPublicJWK).verify(
        header,
        jwt.base64JWTFormatWithNoSignature.getBytes(StandardCharset.UTF_8),
        jwt.signature.base64
      )
    } yield ret
  }.getOrElse(false)

  def ecKeySignJWM(ecKey: JWKECKey, payload: Array[Byte], alg: JWAAlgorithm): SignedMessage = {
    val jwt = ecKeySignJWT(ecKey, payload, alg)

    SignedMessage(
      payload = jwt.payload,
      Seq(
        JWMSignatureObj(
          `protected` = jwt.protectedHeader.base64.unsafeAsObj[SignProtectedHeader],
          signature = SignatureJWM(jwt.signature.base64url),
          header = None // TODO Some(JWMHeader(kid = ))
        )
      )
    )
  }

  // TODO return ZIO
  def ecKeySignJWT(
      ecKey: ECKey,
      payload: Array[Byte]
  ): JWT = ecKeySignJWT(
    ecKey = ecKey.toJWK,
    payload = payload,
    alg = ecKey.jwaAlgorithmtoSign
  )

  def ecKeySignJWT(
      ecKey: JWKECKey,
      payload: Array[Byte],
      alg: JWAAlgorithm
  ): JWT = {
    require(ecKey.isPrivate(), "EC JWK must include the private key (d)")

    val signer: JWSSigner = new ECDSASigner(ecKey) // Create the EC signer
    val header: JWSHeader = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(ecKey.getKeyID()).build()
    val payloadObj = new JosePayload(payload)
    val jwsObject: JWSObject = new JWSObject(header, payloadObj) // Creates the JWS object with payload

    jwsObject.sign(signer)
    val jwt = JWT.unsafeFromEncodedJWT(jwsObject.serialize())
    assert(jwt.payload.base64url == payloadObj.toBase64URL.toString) // redundant check
    assert(jwt.signature.base64url == jwsObject.getSignature.toString) // redundant check

    jwt
  }

  // extension (okpKey: OctetKeyPair) {
  //   def verify(jwm: SignedMessage, alg: JWAAlgorithm): Boolean
  //   def signWithEd25519(payload: Array[Byte], alg: JWAAlgorithm): SignedMessage
  // }

  def okpKeyVerifyJWMWithEd25519(okpKey: OctetKeyPair, jwm: SignedMessage, alg: JWAAlgorithm): Boolean = {
    assert(
      okpKey.getCurve().getName() == JWKCurve.Ed25519.getName(),
      "This method can only be call with Curve.Ed25519"
    ) // TODO make it safe
    assert(
      alg == JWAAlgorithm.EdDSA,
      "This method can only be call with JWAAlgorithm.EdDSA"
    ) // TODO make it safe

    val maybeKeyID = Option(okpKey.getKeyID())
    val _key = okpKey.toPublicJWK
    val verifier = new Ed25519Verifier(_key.toPublicJWK)

    val signatureObjs: Seq[JWMSignatureObj] = maybeKeyID match
      case None => jwm.signatures // Try all signatures
      case Some(keyId) =>
        jwm.signatures
          .filter(_.`protected`.obj.kid match {
            case None                                      => true // Try all signatures that does not specify the key
            case Some(VerificationMethodReferenced(value)) => keyId == value // Try this signature
          })

    signatureObjs.exists { obj =>
      val base64noSignature = obj.`protected`.base64url + "." + jwm.payload.base64url
      val header = {
        val h = new JWSHeader.Builder(obj.`protected`.obj.alg.toJWSAlgorithm)
        maybeKeyID.foreach(h.keyID(_))
        h.build()
      }
      verifier.verify(
        header,
        (base64noSignature).getBytes(StandardCharset.UTF_8),
        obj.signature.base64
      )
    }
  }

  // TODO return ZIO
  def okpKeyVerifyJWTWithEd25519(okpKey: OKPKey, jwt: JWT): Boolean =
    okpKeyVerifyJWTWithEd25519(okpKey = okpKey.toJWK, jwt)

  def okpKeyVerifyJWTWithEd25519(okpKey: OctetKeyPair, jwt: JWT): Boolean = {

    assert(
      okpKey.getCurve().getName() == JWKCurve.Ed25519.getName(),
      "This method can only be call with Curve.Ed25519"
    ) // TODO make it safe

    for {
      headerJson <- jwt.protectedHeader.content.fromJson[ast.Json.Obj]
      alg <- headerJson.get("alg").map(_.asString) match
        case None              => Left("The field 'alg' must exist")
        case Some(None)        => Left("The field 'alg' must be of the type String")
        case Some(Some(value)) => Right(value)
      algorithmTmp <- safeValueOf(JWAAlgorithm.valueOf(alg))
      algorithm <- algorithmTmp match
        case JWAAlgorithm.ES256K => Left(s"This method can only be call with JWAAlgorithm.EdDSA (got '$algorithmTmp')")
        case JWAAlgorithm.ES256  => Left(s"This method can only be call with JWAAlgorithm.EdDSA (got '$algorithmTmp')")
        case JWAAlgorithm.ES384  => Left(s"This method can only be call with JWAAlgorithm.EdDSA (got '$algorithmTmp')")
        case JWAAlgorithm.ES512  => Left(s"This method can only be call with JWAAlgorithm.EdDSA (got '$algorithmTmp')")
        case JWAAlgorithm.EdDSA  => Right(JWAAlgorithm.EdDSA)
      header = {
        val h = new JWSHeader.Builder(algorithm.toJWSAlgorithm)
        Option(okpKey.getKeyID()).foreach(h.keyID(_))
        h.build()
      }
      ret = new Ed25519Verifier(okpKey.toPublicJWK.toPublicJWK).verify(
        header,
        jwt.base64JWTFormatWithNoSignature.getBytes(StandardCharset.UTF_8),
        jwt.signature.base64
      )
    } yield ret
  }.getOrElse(false)

  def okpKeySignJWMWithEd25519(
      okpKey: OctetKeyPair,
      payload: Array[Byte],
      alg: JWAAlgorithm
  ): SignedMessage = {
    val jwt = okpKeySignJWTWithEd25519(okpKey, payload, alg)

    SignedMessage(
      payload = jwt.payload,
      Seq(
        JWMSignatureObj(
          `protected` = jwt.protectedHeader.base64.unsafeAsObj[SignProtectedHeader],
          signature = SignatureJWM(jwt.signature.base64url),
          header = None // TODO Some(JWMHeader(kid = ))
        )
      )
    )
  }

  // TODO return ZIO
  def okpKeySignJWTWithEd25519(
      okpKey: OKPKey,
      payload: Array[Byte]
  ): JWT = okpKeySignJWTWithEd25519(
    okpKey = okpKey.toJWK,
    payload = payload,
    alg = okpKey.jwaAlgorithmtoSign
  )

  def okpKeySignJWTWithEd25519(
      okpKey: OctetKeyPair,
      payload: Array[Byte],
      alg: JWAAlgorithm
  ): JWT = {
    require(okpKey.isPrivate(), "OKP JWK must include the private key (d)")
    assert(
      okpKey.getCurve().getName() == JWKCurve.Ed25519.getName(),
      "This method can only be call with Curve.Ed25519"
    ) // TODO make it safe

    val signer: JWSSigner = new Ed25519Signer(okpKey) // Create the OKP signer
    val header: JWSHeader = new JWSHeader.Builder(alg.toJWSAlgorithm).keyID(okpKey.getKeyID()).build()
    val payloadObj = new JosePayload(payload)
    val jwsObject: JWSObject = new JWSObject(header, payloadObj) // Creates the JWS object with payload

    jwsObject.sign(signer)
    val jwt = JWT.unsafeFromEncodedJWT(jwsObject.serialize())
    assert(jwt.payload.base64url == payloadObj.toBase64URL.toString) // redundant check
    assert(jwt.signature.base64url == jwsObject.getSignature.toString) // redundant check

    jwt
  }

  extension (key: OKP_EC_Key)
    def toJWK: JWKECKey | OctetKeyPair = {
      key match {
        case ec: ECKey   => ec.toJWK
        case okp: OKPKey => okpKey2JWK(okp)
      }
    }
  extension (okp: OKPKey) def toJWK: OctetKeyPair = okpKey2JWK(okp)
  extension (ec: ECKey) def toJWK: JWKECKey = ecKey2JWK(ec)

  def ecKey2JWK(ec: ECKey): JWKECKey = {
    val x = Base64.fromBase64url(ec.x)
    val y = Base64.fromBase64url(ec.y)
    val builder = ec.getCurve match {
      case c: Curve.`P-256`.type   => JWKECKey.Builder(c.toJWKCurve, x, y)
      case c: Curve.`P-384`.type   => JWKECKey.Builder(c.toJWKCurve, x, y)
      case c: Curve.`P-521`.type   => JWKECKey.Builder(c.toJWKCurve, x, y)
      case c: Curve.secp256k1.type => JWKECKey.Builder(c.toJWKCurve, x, y)
    }
    ec.kid.foreach(builder.keyID)
    ec match { // for private key
      case _: PublicKey  => // ok (just the public key)
      case k: PrivateKey => builder.d(Base64.fromBase64url(k.d))
    }
    builder.build()
  }

  def okpKey2JWK(okp: OKPKey): OctetKeyPair = {
    val builder = okp.getCurve match {
      case c: Curve.Ed25519.type => OctetKeyPair.Builder(c.toJWKCurve, Base64.fromBase64url(okp.x))
      case c: Curve.X25519.type  => OctetKeyPair.Builder(c.toJWKCurve, Base64.fromBase64url(okp.x))
    }
    okp.kid.foreach(builder.keyID)
    okp match { // for private key
      case _: PublicKey  => // ok (just the public key)
      case k: PrivateKey => builder.d(Base64.fromBase64url(k.d))
    }
    builder.build()
  }

  extension (key: PublicKey) {
    def verify(jwm: SignedMessage) =
      key.toJWK match {
        case ecKey: JWKECKey =>
          Right(ecKeyVerifyJWM(ecKey, jwm, key.jwaAlgorithmtoSign))
        case okpKey: OctetKeyPair if key.crv == Curve.Ed25519 =>
          Right(okpKeyVerifyJWMWithEd25519(okpKey, jwm, key.jwaAlgorithmtoSign))
        case okpKey: OctetKeyPair => // TODO other curves are not suported ATM
          Left(UnsupportedCurve(obtained = key.crv, supported = Set(Curve.Ed25519))) // NotImplementedError()
      }

  }

  extension (key: PrivateKey) {
    def sign(payload: Array[Byte]) =
      key.toPublicKey.toJWK match {
        case ecKey: JWKECKey =>
          Right(ecKeySignJWM(ecKey, payload, key.jwaAlgorithmtoSign))
        case okpKey: OctetKeyPair if key.crv == Curve.Ed25519 =>
          Right(okpKeySignJWMWithEd25519(okpKey, payload, key.jwaAlgorithmtoSign))
        case okpKey: OctetKeyPair => // TODO other curves are not suported ATM
          Left(UnsupportedCurve(obtained = key.crv, supported = Set(Curve.Ed25519))) // NotImplementedError()
      }

  }

}
