package fmgp.did.comm

import fmgp.did.DIDDocument
import fmgp.crypto.*

import fmgp.crypto.CryptoOperationsImp.*
import munit.*
import zio.json.*
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import concurrent.ExecutionContext.Implicits.global

/** didImpJS/testOnly fmgp.did.comm.SignedMessageSuite */
class SignedMessageSuite extends FunSuite {

  // ### parse ###

  test("parse SignedMessage") {
    val str = SignedMessageExamples.exampleSignatureEdDSA_json.fromJson[SignedMessage] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, SignedMessageExamples.exampleSignatureEdDSA_obj)
    }
  }

  // ### sign ###

  test("sign and verify plaintextMessage using ECKey secp256k1") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    sign(key, DIDCommExamples.plaintextMessageObj)
      .flatMap(jwsObject => verify(key.toPublicKey, jwsObject))
      .map(e => assert(e))
  }

  test("verify plaintextMessage example using ECKey secp256k1") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    verify(key.toPublicKey, SignedMessageExamples.exampleSignatureES256K_obj)
      .map(e => assert(e))
  }

  test("sign and verify plaintextMessage using ECKey P-256") {
    val key: ECPrivateKey = JWKExamples.senderKeyP256.fromJson[ECPrivateKey].toOption.get
    sign(key, DIDCommExamples.plaintextMessageObj)
      .flatMap(jwsObject => verify(key.toPublicKey, jwsObject))
      .map(e => assert(e))

  }

  test("verify plaintextMessage example using ECKey P-256") {
    val key: ECPrivateKey = JWKExamples.senderKeyP256.fromJson[ECPrivateKey].toOption.get
    verify(key.toPublicKey, SignedMessageExamples.exampleSignatureES256_obj)
      .map(e => assert(e))
  }

  // https://github.com/scalameta/munit/issues/554
  test("sign and verify plaintextMessage using ECKey Ed25519") {
    val key = JWKExamples.senderKeyEd25519.fromJson[PrivateKey].toOption.get

    sign(key, DIDCommExamples.plaintextMessageObj)
      .flatMap(jwsObject => verify(key.toPublicKey, jwsObject))
      .map(e => assert(e))
  }

  test("verify plaintextMessage example using ECKey Ed25519") {
    val key = JWKExamples.senderKeyEd25519.fromJson[PrivateKey].toOption.get
    verify(key.toPublicKey, SignedMessageExamples.exampleSignatureEdDSA_obj)
      .map(e => assert(e))
  }

  // ### FAIL ###

  test("fail verify plaintextMessage using ECKey secp256k1") {
    val key: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
    verify(key.toPublicKey, SignedMessageExamples.exampleSignatureEdDSA_failSignature_obj)
      .map(e => assert(!e))
  }

}
