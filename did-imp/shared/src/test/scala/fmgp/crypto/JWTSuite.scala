package fmgp.crypto

import munit.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import zio.json.*

import fmgp.did.DIDDocument
import fmgp.did.comm.*
import fmgp.util.Base64

/** didImpJVM/testOnly fmgp.crypto.JWTSuite
  *
  * didImpJS/testOnly fmgp.crypto.JWTSuite
  */
class JWTSuite extends ZSuite {

  val senderKeyP256_1 = ECPrivateKey(
    kty = KTY.EC,
    kid = "did:example:alice#key-2",
    crv = Curve.`P-256`,
    d = "7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A",
    x = "2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY",
    y = "BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w"
  )

  val senderKeyP256_otherKey = ECPrivateKey(
    kty = KTY.EC,
    kid = "did:example:alice#key-2", // "did:example:alice#key-p256-1"
    crv = Curve.`P-256`,
    d = "sB0bYtpaXyp-h17dDpMx91N3Du1AdN4z1FUq02GbmLw",
    x = "L0crjMN1g0Ih4sYAJ_nGoHUck2cloltUpUVQDhF2nHE",
    y = "SxYgE7CmEJYi7IDhgK5jI4ZiajO8jPRZDldVhqFpYoo"
  )

  val okp = OKPPrivateKey(
    kty = KTY.OKP,
    crv = Curve.Ed25519,
    d = "pFRUKkyzx4kHdJtFSnlPA9WzqkDT1HWV0xZ5OYZd2SY",
    x = "G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww",
    kid = "did:example:alice#key-1",
  )

  val data = """{"a":123}"""

  val jwtSignES256FromJVM =
    "eyJraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktMiIsImFsZyI6IkVTMjU2In0.eyJhIjoxMjN9.s7wBaeCxW12bTT5SzmKWLG5nyCspl3hsy9xRWM6oSXV5ejvPZgKnuDrE8YHsbQNMcDnYxhaiqYeOSq5vbK-6hw"
  val jwtSignES256FromJS =
    "eyJhbGciOiJFUzI1NiIsImtpZCI6ImRpZDpleGFtcGxlOmFsaWNlI2tleS0yIn0.eyJhIjoxMjN9.BtzXIttKQQwE9dNomldInJOuv8g4SZoXCMN2kSwfnwMkJcrlujYtId7Vf9-7uEq3gN9Pt9ysK4GWBSLY1GvXLw"

  val jwtHeader = JWTHeader(
    alg = JWAAlgorithm.ES256K,
    jku = Some("http://localhost"),
    jwk = Some(JWKExamples.senderKeySecp256k1obj.toPublicKey),
    kid = Some("did:example:123"),
    typ = Some(MediaTypes.DIGITAL_CREDENTIAL_SDJWT.subType),
    cty = None,
    crit = None,
  )

  testZ("EC ES256 (P_256 key) sign and verify JWT") {
    for {
      jwt <- CryptoOperationsImp.signJWT(
        senderKeyP256_1,
        data.getBytes,
        // JWAAlgorithm.ES256
      )
      payload = jwt.protectedHeader.toJWTHeader.toOption
      _ = assertEquals(payload.map(_.alg), Some(JWAAlgorithm.ES256))
      _ = assertEquals(payload.flatMap(_.kid), Some("did:example:alice#key-2"))
      // _ = println(jwt.base64JWTFormat)
      ret <- CryptoOperationsImp.verifyJWT(
        senderKeyP256_1.toPublicKey,
        jwt
      )
    } yield assertEquals(ret, true)
  }

  testZ("EC ES256 (P_256 key) verify JWT sign JVM") {
    CryptoOperationsImp.verifyJWT(
      senderKeyP256_1.toPublicKey,
      JWT.unsafeFromEncodedJWT(jwtSignES256FromJVM)
    )
  }

  testZ("EC ES256 (P_256 key) verify JWT sign JS") {
    CryptoOperationsImp.verifyJWT(
      senderKeyP256_1.toPublicKey,
      JWT.unsafeFromEncodedJWT(jwtSignES256FromJS)
    )
  }

  testZ("EC ES256 (P_256 key) sign and fail to verify if 'kid' of the key do not match") {
    for {
      jwt <- CryptoOperationsImp.signJWT(
        senderKeyP256_1,
        data.getBytes,
        // JWAAlgorithm.ES256
      )
      payload = jwt.protectedHeader.toJWTHeader.toOption
      _ = assertEquals(payload.map(_.alg), Some(JWAAlgorithm.ES256))
      _ = assertEquals(payload.flatMap(_.kid), Some("did:example:alice#key-2"))
      // _ = println(jwt.base64JWTFormat)
      keyWithFailKid = senderKeyP256_1.copy(kid = "did:example:alice#key-fail")
      ret <- CryptoOperationsImp.verifyJWT(
        keyWithFailKid.toPublicKey,
        jwt
      )
    } yield assertEquals(ret, true)
  }

  testZ("EC ES256 (P_256 key) sign and fail to verify JWT if key is different") {
    for {
      jwt <- CryptoOperationsImp.signJWT(
        senderKeyP256_1,
        data.getBytes,
        // JWAAlgorithm.ES256
      )
      payload = jwt.protectedHeader.toJWTHeader.toOption
      _ = assertEquals(payload.map(_.alg), Some(JWAAlgorithm.ES256))
      _ = assertEquals(payload.flatMap(_.kid), Some("did:example:alice#key-2"))
      // _ = println(jwt.base64JWTFormat)
      ret <- CryptoOperationsImp.verifyJWT(
        senderKeyP256_otherKey.toPublicKey,
        jwt
      )
    } yield assertEquals(ret, false)
  }

  testZ("OKP EdDSA (Ed25519 key) sign and verify JWT") {
    for {
      jwt <- CryptoOperationsImp.signJWT(
        okp,
        data.getBytes,
        // JWAAlgorithm.EdDSA
      )
      payload = jwt.protectedHeader.toJWTHeader.toOption
      _ = assertEquals(payload.map(_.alg), Some(JWAAlgorithm.EdDSA))
      _ = assertEquals(payload.flatMap(_.kid), Some("did:example:alice#key-1"))
      // _ = println(jwt.base64JWTFormat)
      ret <- CryptoOperationsImp.verifyJWT(
        okp.toPublicKey,
        jwt
      )
    } yield assertEquals(ret, true)
  }

  test("(JWTUtils) JWTHeader - encode and decode (JSON)") {
    val expected =
      """{"alg":"ES256K","jku":"http://localhost","jwk":{"kty":"EC","crv":"secp256k1","x":"aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk","y":"JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk","kid":"did:example:alice#key-3"},"kid":"did:example:123","typ":"dc+sd-jwt"}"""
    assertEquals(jwtHeader.toJson, expected)
    val obj = jwtHeader.toJson.fromJson[JWTHeader].getOrElse(fail("Fail to parse"))
    assertEquals(obj, jwtHeader)
  }

  test("(JWTUtils) JWTHeader - encode and decode (JSON in BASE64)") {
    val expected = Base64.fromBase64url(
      "eyJqa3UiOiJodHRwOi8vbG9jYWxob3N0Iiwia2lkIjoiZGlkOmV4YW1wbGU6MTIzIiwidHlwIjoiZGMrc2Qtand0IiwiYWxnIjoiRVMyNTZLIiwiandrIjp7Imt0eSI6IkVDIiwiY3J2Ijoic2VjcDI1NmsxIiwia2lkIjoiZGlkOmV4YW1wbGU6YWxpY2Uja2V5LTMiLCJ4IjoiYVRvVzVFYVRxNW1sQWY4QzVFQ1lEU2txc0p5Y3JXLWUxU1E2X0dKY0FPayIsInkiOiJKQUdYOTRjYUEyMVdLcmVYd1lVYU9DWVRCTXJxYVg0S1dJbHNRWlRIV0NrIn19"
    )
    val data = jwtHeader.base64
    // assertEquals(data.urlBase64, expected.urlBase64) // fail becuase of the order of field in the json
    // assertEquals(data, expected)// fail becuase of the order of field in the json
    val obj = data.decodeToString.fromJson[JWTHeader].getOrElse(fail("Fail to parse"))
    assertEquals(obj, jwtHeader)
  }
}
