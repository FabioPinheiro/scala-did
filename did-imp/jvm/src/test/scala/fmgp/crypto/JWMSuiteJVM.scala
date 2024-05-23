package fmgp.crypto

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}

import munit._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import zio.json._

import fmgp.did.DIDDocument
import fmgp.did.comm._
import fmgp.crypto.UtilsJVM._
import fmgp.util.Base64

/** didImpJVM/testOnly fmgp.crypto.JWMSuiteJVM */
class JWMSuiteJVM extends FunSuite {

  // override val munitTimeout = Duration(1, "s")

  test("sign and verify plaintextMessage with Curve SECP256K1") {
    val ecJWK: JWKECKey = JWKECKey // TODO use senderSecp256k1 parsed with
      .Builder(
        JWKCurve.SECP256K1,
        Base64.fromBase64url("aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk"),
        Base64.fromBase64url("JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk")
      )
      .keyID("did:example:alice#key-3")
      .d(Base64.fromBase64url("N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA"))
      .build()

    val jwsObject = ecKeySign(ecJWK, DIDCommExamples.plaintextMessageObj.toJson.getBytes, JWAAlgorithm.ES256K)

    val ecPublicJWK: JWKECKey = ecJWK.toPublicJWK()
    assert(ecKeyVerify(ecPublicJWK, jwsObject, JWAAlgorithm.ES256K))
    assert(ecKeyVerify(ecPublicJWK, SignedMessageExamples.exampleSignatureES256K_obj, JWAAlgorithm.ES256K))
  }

  test("sign and verify plaintextMessage with Curve Ed25519") {
    val okp = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = "INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug",
      x = "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
      kid = Some("did:example:alice#key-100")
    )
    val okpJWK: OctetKeyPair = okp.toJWK

    val jwsObject =
      okpKeySignWithEd25519(okpJWK, DIDCommExamples.plaintextMessageObj.toJson.getBytes, JWAAlgorithm.EdDSA)

    val ecPublicJWK: OctetKeyPair = okpJWK.toPublicJWK()
    assertEquals((okpKeyVerifyWithEd25519(ecPublicJWK, jwsObject, JWAAlgorithm.EdDSA)), true)
  }

  test("sign and verify plaintextMessage with Curve X25519 (saying is a Curve Ed25519) MUST Fail") {
    val okp = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519, // But the d and x is for Curve X25519
      d = "Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c",
      x = "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw",
      kid = Some("did:example:alice#key-101")
    )
    val okpJWK: OctetKeyPair = okp.toJWK

    val jwsObject =
      okpKeySignWithEd25519(okpJWK, DIDCommExamples.plaintextMessageObj.toJson.getBytes, JWAAlgorithm.EdDSA)

    val ecPublicJWK: OctetKeyPair = okpJWK.toPublicJWK()
    assertEquals(okpKeyVerifyWithEd25519(ecPublicJWK, jwsObject, JWAAlgorithm.EdDSA), false)
  }

  test("sign and verify plaintextMessage with Curve P_256") {
    val ecJWK: JWKECKey = JWKECKey
      .Builder(
        JWKCurve.P_256,
        Base64.fromBase64url("2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY"),
        Base64.fromBase64url("BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w")
      )
      .keyID("did:example:alice#key-3")
      .d(Base64.fromBase64url("7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A"))
      .build()

    val jwsObject = ecKeySign(ecJWK, DIDCommExamples.plaintextMessageObj.toJson.getBytes, JWAAlgorithm.ES256)

    val ecPublicJWK: JWKECKey = ecJWK.toPublicJWK()
    assert(ecKeyVerify(ecPublicJWK, jwsObject, JWAAlgorithm.ES256))
    assert(ecKeyVerify(ecPublicJWK, SignedMessageExamples.exampleSignatureES256_obj, JWAAlgorithm.ES256))
  }

  test("sign and fail verify if 'kid' of the key do not match") {
    def ecJWK(kid: String) = JWKECKey
      .Builder(
        JWKCurve.P_256,
        Base64.fromBase64url("2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY"),
        Base64.fromBase64url("BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w")
      )
      .keyID(kid)
      .d(Base64.fromBase64url("7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A"))
      .build()

    val jwsObject = ecKeySign(
      ecJWK("did:example:alice#key-3"),
      DIDCommExamples.plaintextMessageObj.toJson.getBytes,
      JWAAlgorithm.ES256
    )

    val ecPublicJWK: JWKECKey = ecJWK("did:example:alice#key-fail").toPublicJWK()
    assert(!ecKeyVerify(ecPublicJWK, jwsObject, JWAAlgorithm.ES256))
  }

}
