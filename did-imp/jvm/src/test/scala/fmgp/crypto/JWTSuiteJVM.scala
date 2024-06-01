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

/** didImpJVM/testOnly fmgp.crypto.JWTSuiteJVM */
class JWTSuiteJVM extends FunSuite {

  test("EC ES256 (P_256 key) sign and verify JWT") {
    val ecJWK: JWKECKey = JWKECKey
      .Builder(
        JWKCurve.P_256,
        Base64.fromBase64url("2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY"),
        Base64.fromBase64url("BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w")
      )
      .keyID("did:example:alice#key-3")
      .d(Base64.fromBase64url("7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A"))
      .build()

    val jwtObject = ecKeySignJWT(
      ecJWK,
      DIDCommExamples.plaintextMessageObj.toJson.getBytes,
      JWAAlgorithm.ES256
    )
    assertEquals(
      jwtObject.protectedHeader.base64url,
      "eyJraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktMyIsImFsZyI6IkVTMjU2In0"
    )
    assertEquals(
      jwtObject.payload.base64url,
      "eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXBlIjoiPG1lc3NhZ2UtdHlwZS11cmk-IiwidG8iOlsiZGlkOmV4YW1wbGU6Ym9iIl0sImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsImNyZWF0ZWRfdGltZSI6MTUxNjI2OTAyMiwiZXhwaXJlc190aW1lIjoxNTE2Mzg1OTMxLCJib2R5Ijp7Im1lc3NhZ2VfdHlwZV9zcGVjaWZpY19hdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIiwiYW5vdGhlcl9hdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn0sInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tcGxhaW4ranNvbiJ9"
    )
    assert(ecKeyVerifyJWT(ecJWK.toPublicJWK(), jwtObject))
  }

  test("EC ES256 (P_256 key) sign and fail to verify if 'kid' of the key do not match") {
    def ecJWK(kid: String) = JWKECKey
      .Builder(
        JWKCurve.P_256,
        Base64.fromBase64url("2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY"),
        Base64.fromBase64url("BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w")
      )
      .keyID(kid)
      .d(Base64.fromBase64url("7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A"))
      .build()

    val jwtObject = ecKeySignJWM(
      ecJWK("did:example:alice#key-3"),
      DIDCommExamples.plaintextMessageObj.toJson.getBytes,
      JWAAlgorithm.ES256
    )

    val ecPublicJWK: JWKECKey = ecJWK("did:example:alice#key-fail").toPublicJWK()
    assert(!ecKeyVerifyJWM(ecPublicJWK, jwtObject, JWAAlgorithm.ES256))
  }

  test("EC ES256 (P_256 key) sign and fail to verify JWT if key is different") {
    val ecJWK1: JWKECKey = JWKECKey
      .Builder(
        JWKCurve.P_256,
        Base64.fromBase64url("2syLh57B-dGpa0F8p1JrO6JU7UUSF6j7qL-vfk1eOoY"),
        Base64.fromBase64url("BgsGtI7UPsObMRjdElxLOrgAO9JggNMjOcfzEPox18w")
      )
      .keyID("did:example:alice#key-3")
      .d(Base64.fromBase64url("7TCIdt1rhThFtWcEiLnk_COEjh1ZfQhM4bW2wz-dp4A"))
      .build()

    val ecJWK2: JWKECKey = JWKECKey
      .Builder(
        JWKCurve.P_256,
        Base64.fromBase64url("L0crjMN1g0Ih4sYAJ_nGoHUck2cloltUpUVQDhF2nHE"),
        Base64.fromBase64url("SxYgE7CmEJYi7IDhgK5jI4ZiajO8jPRZDldVhqFpYoo")
      )
      .keyID("did:example:alice#key-3") // "did:example:alice#key-p256-1"
      .d(Base64.fromBase64url("sB0bYtpaXyp-h17dDpMx91N3Du1AdN4z1FUq02GbmLw"))
      .build()

    val jwtObject = ecKeySignJWT(
      ecJWK1,
      DIDCommExamples.plaintextMessageObj.toJson.getBytes,
      JWAAlgorithm.ES256
    )
    assert(!ecKeyVerifyJWT(ecJWK2.toPublicJWK(), jwtObject))
  }

  test("OKP EdDSA (Ed25519 key) sign and verify JWT") {

    val okp = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = "pFRUKkyzx4kHdJtFSnlPA9WzqkDT1HWV0xZ5OYZd2SY",
      x = "G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww",
      kid = Some("did:example:alice#key-1"),
    ).toJWK

    val jwtObject = okpKeySignJWTWithEd25519(
      okp,
      DIDCommExamples.plaintextMessageObj.toJson.getBytes,
      JWAAlgorithm.EdDSA
    )
    assert(okpKeyVerifyJWTWithEd25519(okp.toPublicJWK(), jwtObject))
  }

}
