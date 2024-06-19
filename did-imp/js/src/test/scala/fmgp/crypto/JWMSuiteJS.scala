package fmgp.crypto

import fmgp.did.DIDDocument
import fmgp.did.comm._
import fmgp.crypto.CryptoOperationsImp._
import munit._
import zio._
import zio.json._

import scala.concurrent.duration.Duration
import scala.scalajs.js.JavaScriptException
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import fmgp.crypto.error.FailToGenerateKey

/** didImpJS/testOnly fmgp.crypto.JWMSuiteJS */
class JWMSuiteJS extends ZSuite {

  import scala.scalajs.js

  testZ("Make Key ES256 (P-256)") {
    for {
      key <- KeyGenerator.makeKeyEC(Curve.`P-256`)
      _ = assertEquals(key.crv, Curve.`P-256`)
    } yield ()
  }

  // TODO
  // testZ("Make Key ES256K (secp256k1)") {
  //   for {
  //     key <- KeyGenerator.makeKeyEC("ES256K")
  //     _ = assertEquals(key.crv, Curve.secp256k1)
  //   } yield ()
  // }

  testZ("Make Key EdDSA (Ed25519)") {
    for {
      key <- KeyGenerator.makeKeyOKP(Curve.Ed25519)
      _ = assertEquals(key.crv, Curve.Ed25519)
    } yield ()
  }
  testZ("Make Key EdDSA (X25519)") {
    for {
      key <- KeyGenerator.makeKeyOKP(Curve.X25519)
      _ = assertEquals(key.crv, Curve.X25519)
    } yield ()
  }

  testZ("[jose] Make Key ES256 (P-256)") {
    for {
      key <- KeyGenerator.joseMakeKeyEC("ES256")
      _ = assertEquals(key.crv, Curve.`P-256`)
    } yield ()
  }

  testZ("[jose] Make Key ES256K (secp256k1)") {
    for {
      key <- KeyGenerator.joseMakeKeyEC("ES256K")
      _ = assertEquals(key.crv, Curve.secp256k1)
    } yield ()
  }
  testZ("[jose] Make Key EdDSA (Ed25519)") {
    for {
      key <- KeyGenerator.joseMakeKeyOKP("EdDSA", "Ed25519")
      _ = assertEquals(key.crv, Curve.Ed25519)
    } yield ()
  }
  testZ("[jose] Make Key EdDSA (X25519)") {
    for {
      key <- KeyGenerator.joseMakeKeyOKP("ECDH-ES+A256KW", "X25519")
      _ = assertEquals(key.crv, Curve.X25519)
    } yield ()
  }

  testZ("sign and verify an example") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    sign(key, DIDCommExamples.plaintextMessageObj).flatMap { jwsObject =>
      verify(key.toPublicKey, jwsObject).map(e => assert(e)) <&>
        verify(key.toPublicKey, SignedMessageExamples.exampleSignatureES256K_obj).map(e => assert(e))
    }
  }

  // TODO REMOVE or remake
  // test("sign and verify plaintextMessage") {
  //   val key = JWKECKey
  //     .apply(
  //       ECCurve.secp256k1,
  //       "aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
  //       "JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
  //     )
  //     .setKid("did:example:alice#key-3")
  //     .setD("N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA")

  //   val data = js.Dynamic.literal("urn:example:claim" -> "foo")
  //   val jwsObject = JWT.sign(data, key)

  //   val publicKey: JWKECKey = key // TODO .toPublicJWK()
  //   assert(Try { JWT.verify(jwsObject, publicKey) } match {
  //     case Success(i)                                              => true
  //     case Failure(JavaScriptException(ex: JWSVerificationFailed)) => false
  //     case Failure(ex)                                             => throw ex
  //   })
  //   assert(Try { JWT.verify(JWMExample.exampleSignatureES256K_obj.base64, publicKey) } match {
  //     case Success(i)                                              => true
  //     case Failure(JavaScriptException(ex: JWSVerificationFailed)) => false
  //     case Failure(ex)                                             => throw ex
  //   })
  // }

  // TODO REMOVE
  // test("sign and verify plaintextMessage using JWKCruve") {
  //   val jwk: JWKCruve = JWKExamples.senderSecp256k1.fromJson[JWKCruve].toOption.get
  //   val jwsObject = jwk.sign(DIDCommExamples.plaintextMessageObj)
  //   assert(jwk.verify(jwsObject))
  //   assert(jwk.verify(JWMExample.example))
  // }

}
