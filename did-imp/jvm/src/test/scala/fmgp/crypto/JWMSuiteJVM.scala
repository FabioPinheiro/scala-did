package fmgp.crypto

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.Curve

import munit._
import zio.json._

import fmgp.did.DIDDocument
import fmgp.did.comm._
import fmgp.crypto.UtilsJVM._
import fmgp.util.Base64

class JWMSuiteJVM extends FunSuite {

  test("sign and verify plaintextMessage") {
    val ecJWK: ECKey = ECKey // TODO use senderSecp256k1 parsed with
      .Builder(
        Curve.SECP256K1,
        Base64.fromBase64url("aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk"),
        Base64.fromBase64url("JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk")
      )
      .keyID("did:example:alice#key-3")
      .d(Base64.fromBase64url("N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA"))
      .build()

    val jwsObject = ecJWK.sign(DIDCommExamples.plaintextMessageObj.toJson.getBytes, JWAAlgorithm.ES256K)

    val ecPublicJWK: ECKey = ecJWK.toPublicJWK()
    assert(ecPublicJWK.verify(jwsObject, JWAAlgorithm.ES256K))
    assert(ecPublicJWK.verify(SignedMessageExamples.exampleSignatureES256K_obj, JWAAlgorithm.ES256K))
  }

}
