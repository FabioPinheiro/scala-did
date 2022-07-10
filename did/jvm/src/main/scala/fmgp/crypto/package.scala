package fmgp

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.StandardCharset
import fmgp.did.comm.PlaintextMessageClass
import zio.json._

import scala.util.chaining._
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.crypto.Ed25519Verifier
import scala.util.Try
import scala.util.Success
import scala.util.Failure

package object crypto {

  extension (alg: JWAAlgorithm) {
    def toJVM = alg match {
      case JWAAlgorithm.ES256K => JWSAlgorithm.ES256K
      case JWAAlgorithm.ES256  => JWSAlgorithm.ES256
      case JWAAlgorithm.EdDSA  => JWSAlgorithm.EdDSA
    }
  }
  extension (curve: Curve) {
    def toJVM = curve match {
      case Curve.`P-256`   => JWKCurve.P_256
      case Curve.`P-384`   => JWKCurve.P_384
      case Curve.`P-521`   => JWKCurve.P_521
      case Curve.secp256k1 => JWKCurve.SECP256K1
      case Curve.X25519    => JWKCurve.X25519
      case Curve.Ed25519   => JWKCurve.Ed25519
    }
  }

  extension (ecKey: JWKECKey) {
    def verify(jwm: JWM, alg: JWAAlgorithm): Boolean = {
      val _key = ecKey.toPublicJWK
      val verifier = new ECDSAVerifier(_key.toPublicJWK);
      val haeder = new JWSHeader.Builder(alg.toJVM).keyID(_key.getKeyID()).build()
      verifier.verify(
        haeder,
        (jwm.signatures.head.`protected` + "." + jwm.payload).getBytes(StandardCharset.UTF_8),
        Base64URL(jwm.signatures.head.signature) // FIXME .head
      )
    }

    def sign(plaintext: PlaintextMessageClass, alg: JWAAlgorithm): JWM = { // TODO use PlaintextMessage
      require(ecKey.isPrivate(), "EC JWK must include the private key (d)")

      val signer: JWSSigner = new ECDSASigner(ecKey) // Create the EC signer
      val haeder: JWSHeader = new JWSHeader.Builder(alg.toJVM).keyID(ecKey.getKeyID()).build()
      val payloadObj = new Payload(plaintext.toJson)
      val jwsObject: JWSObject = new JWSObject(haeder, payloadObj) // Creates the JWS object with payload

      jwsObject.sign(signer)
      jwsObject.serialize().split('.') match {
        case Array(protectedValue, payload, signature) =>
          assert(payload == payloadObj.toBase64URL.toString) // redundant check
          assert(signature == jwsObject.getSignature.toString) // redundant check
          JWM(
            payload = payload,
            Seq(JWMSignatureObj(`protected` = `protectedValue`, signature = signature)) // TODO haeder
          )
      }
    }
  }

  extension (okpKey: OctetKeyPair) {
    def verify(jwm: JWM, alg: JWAAlgorithm): Boolean = {
      val _key = okpKey.toPublicJWK
      val verifier = new Ed25519Verifier(_key.toPublicJWK);
      val haeder = new JWSHeader.Builder(alg.toJVM).keyID(_key.getKeyID()).build()
      verifier.verify(
        haeder,
        (jwm.signatures.head.`protected` + "." + jwm.payload).getBytes(StandardCharset.UTF_8),
        Base64URL(jwm.signatures.head.signature) // FIXME .head
      )
    }

    def sign(plaintext: PlaintextMessageClass, alg: JWAAlgorithm): JWM = { // TODO use PlaintextMessage
      require(okpKey.isPrivate(), "EC JWK must include the private key (d)")

      val signer: JWSSigner = new Ed25519Signer(okpKey) // Create the OKP signer
      val haeder: JWSHeader = new JWSHeader.Builder(alg.toJVM).keyID(okpKey.getKeyID()).build()
      val payloadObj = new Payload(plaintext.toJson)

      val jwsObject: JWSObject = new JWSObject(haeder, payloadObj) // Creates the JWS object with payload

      jwsObject.sign(signer)
      jwsObject.serialize().split('.') match {
        case Array(protectedValue, payload, signature) =>
          assert(payload == payloadObj.toBase64URL.toString) // redundant check
          assert(signature == jwsObject.getSignature.toString) // redundant check
          JWM(
            payload = payload,
            Seq(JWMSignatureObj(`protected` = `protectedValue`, signature = signature)) // TODO haeder
          )
      }
    }
  }

  extension (key: OKP_EC_Key) {
    def toJWK: JWKECKey | OctetKeyPair = {
      key match {
        case ec: ECKey =>
          val builder = ec.getCurve match {
            case Curve.`P-256`   => JWKECKey.Builder(JWKCurve.P_256, Base64URL(ec.x), Base64URL(ec.y))
            case Curve.`P-384`   => JWKECKey.Builder(JWKCurve.P_384, Base64URL(ec.x), Base64URL(ec.y))
            case Curve.`P-521`   => JWKECKey.Builder(JWKCurve.P_521, Base64URL(ec.x), Base64URL(ec.y))
            case Curve.secp256k1 => JWKECKey.Builder(JWKCurve.SECP256K1, Base64URL(ec.x), Base64URL(ec.y))
          }
          key.kid.foreach(builder.keyID)
          key match { // for private key
            case _: PublicKey  => // ok (just the public key)
            case k: PrivateKey => builder.d(Base64URL(k.d))
          }
          builder.build()
        case okp: OKPKey =>
          val builder = okp.getCurve match {
            case Curve.Ed25519 => OctetKeyPair.Builder(JWKCurve.Ed25519, Base64URL(okp.x))
            case Curve.X25519  => OctetKeyPair.Builder(JWKCurve.X25519, Base64URL(okp.x))
          }
          key.kid.foreach(builder.keyID)
          key match { // for private key
            case _: PublicKey  => // ok (just the public key)
            case k: PrivateKey => builder.d(Base64URL(k.d))
          }
          builder.build()
      }
    }
  }

  extension (key: PrivateKey) {
    def verify(jwm: JWM): Boolean = key.toJWK match {
      case ecKey: JWKECKey      => ecKey.verify(jwm, key.jwaAlgorithmtoSign)
      case okpKey: OctetKeyPair => okpKey.verify(jwm, key.jwaAlgorithmtoSign)
    }

  }

  extension (key: OKP_EC_Key) {
    def sign(plaintext: PlaintextMessageClass): JWM = { // TODO use PlaintextMessageClass
      key.toJWK match {
        case ecKey: JWKECKey      => ecKey.sign(plaintext, key.jwaAlgorithmtoSign)
        case okpKey: OctetKeyPair => okpKey.sign(plaintext, key.jwaAlgorithmtoSign)
      }
    }
  }

}
