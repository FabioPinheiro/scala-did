package fmgp.crypto

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.{Curve as JWKCurve}
import com.nimbusds.jose.jwk.{ECKey as JWKECKey}

import munit.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import zio.json.*

import fmgp.did.DIDDocument
import fmgp.did.comm.*
import fmgp.crypto.UtilsJVM.*
import fmgp.util.Base64

/** didImpJVM/testOnly fmgp.crypto.JWTSuiteJVM */
class JWTSuiteJVM extends FunSuite {

  val okpKey = OKPPrivateKeyWithKid(
    kty = KTY.OKP,
    crv = Curve.Ed25519,
    d = "pFRUKkyzx4kHdJtFSnlPA9WzqkDT1HWV0xZ5OYZd2SY",
    x = "G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww",
    kid = "did:example:alice#key-1",
  )
  val ecKey = ECPrivateKeyWithKid(
    kty = KTY.EC,
    crv = Curve.`P-256`,
    x = "2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY",
    y = "BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w",
    d = "7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A",
    kid = "did:example:alice#key-3",
  )

  val rawPayload: Array[Byte] = DIDCommExamples.plaintextMessageObj.toJson.getBytes

  def okpJwtUnsigned = JWTUnsigned(
    header = JWTHeader(alg = JWAAlgorithm.EdDSA, kid = Some(okpKey.kid)),
    payload = Payload.fromBytes(rawPayload),
  )

  def ecJwtUnsigned = JWTUnsigned(
    header = JWTHeader(alg = JWAAlgorithm.ES256, kid = Some(ecKey.kid)),
    payload = Payload.fromBytes(rawPayload),
  )

  def ecJWK(kid: String) = JWKECKey
    .Builder(
      JWKCurve.P_256,
      Base64.fromBase64url(ecKey.x),
      Base64.fromBase64url(ecKey.y)
    )
    .keyID(kid)
    .d(Base64.fromBase64url(ecKey.d))
    .build()

  test("EC ES256 (P_256 key) sign and verify JWT") {
    val jwtObject = ecKeySignJWT(ecJWK(ecKey.kid), rawPayload, JWAAlgorithm.ES256)
    assertEquals(
      jwtObject.protectedHeader.base64url,
      "eyJraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktMyIsImFsZyI6IkVTMjU2In0"
    )
    assertEquals(
      jwtObject.payload.base64url,
      "eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXBlIjoiPG1lc3NhZ2UtdHlwZS11cmk-IiwidG8iOlsiZGlkOmV4YW1wbGU6Ym9iIl0sImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsImNyZWF0ZWRfdGltZSI6MTUxNjI2OTAyMiwiZXhwaXJlc190aW1lIjoxNTE2Mzg1OTMxLCJib2R5Ijp7Im1lc3NhZ2VfdHlwZV9zcGVjaWZpY19hdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIiwiYW5vdGhlcl9hdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19"
    )
    assert(ecKeyVerifyJWT(ecJWK(ecKey.kid).toPublicJWK(), jwtObject))
  }

  test("EC ES256 (P_256 key) sign and fail to verify if 'kid' of the key do not match") {
    val jwtObject = ecKeySignJWM(ecJWK(ecKey.kid), rawPayload, JWAAlgorithm.ES256)
    val ecPublicJWK: JWKECKey = ecJWK("did:example:alice#key-fail").toPublicJWK()
    assert(!ecKeyVerifyJWM(ecPublicJWK, jwtObject, JWAAlgorithm.ES256))
  }

  test("EC ES256 (P_256 key) sign and fail to verify JWT if key is different") {
    val ecJWK2: JWKECKey = JWKECKey
      .Builder(
        JWKCurve.P_256,
        Base64.fromBase64url("L0crjMN1g0Ih4sYAJ_nGoHUck2cloltUpUVQDhF2nHE"),
        Base64.fromBase64url("SxYgE7CmEJYi7IDhgK5jI4ZiajO8jPRZDldVhqFpYoo")
      )
      .keyID(ecKey.kid)
      .d(Base64.fromBase64url("sB0bYtpaXyp-h17dDpMx91N3Du1AdN4z1FUq02GbmLw"))
      .build()

    val jwtObject = ecKeySignJWT(ecJWK(ecKey.kid), rawPayload, JWAAlgorithm.ES256)
    assert(!ecKeyVerifyJWT(ecJWK2.toPublicJWK(), jwtObject))
  }

  test("OKP EdDSA (Ed25519 key) sign and verify JWT") {
    val okp = okpKey.toJWK
    val jwtObject = okpKeySignJWTWithEd25519(okp, rawPayload, JWAAlgorithm.EdDSA)
    assert(okpKeyVerifyJWTWithEd25519(okp.toPublicJWK(), jwtObject))
  }

  test("OKP EdDSA (Ed25519 key) sign and verify JWT via Extensions") {
    val jwt = okpJwtUnsigned.signWith(okpKey).getOrElse(fail("sign failed"))
    assert(jwt.verifyWith(okpKey.toPublicKey), "verify failed")
  }

  test("EC ES256 (P-256 key) sign and verify JWT via Extensions") {
    val jwt = ecJwtUnsigned.signWith(ecKey).getOrElse(fail("sign failed"))
    assert(jwt.verifyWith(ecKey.toPublicKey), "verify failed")
  }

  test("OKP EdDSA (Ed25519 key) sign via Extensions and verify JWT via UtilsJVM") {
    val jwt = okpJwtUnsigned.signWith(okpKey).getOrElse(fail("sign failed"))
    assert(okpKeyVerifyJWTWithEd25519(okpKey.toJWK.toPublicJWK(), jwt), "verify failed")
  }

  test("OKP EdDSA (Ed25519 key) sign via UtilsJVM and verify JWT via Extensions") {
    val jwt = okpKeySignJWTWithEd25519(okpKey.toJWK, rawPayload, JWAAlgorithm.EdDSA)
    assert(jwt.verifyWith(okpKey.toPublicKey), "verify failed")
  }

  test("EC ES256 (P-256 key) sign via Extensions and verify JWT via UtilsJVM") {
    val jwt = ecJwtUnsigned.signWith(ecKey).getOrElse(fail("sign failed"))
    assert(ecKeyVerifyJWT(ecKey.toJWK.toPublicJWK(), jwt), "verify failed")
  }

  test("EC ES256 (P-256 key) sign via UtilsJVM and verify JWT via Extensions") {
    val jwt = ecKeySignJWT(ecKey.toJWK, rawPayload, JWAAlgorithm.ES256)
    assert(jwt.verifyWith(ecKey.toPublicKey), "verify failed")
  }

}
