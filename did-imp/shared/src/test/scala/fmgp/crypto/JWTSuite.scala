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

  testZ("EC ES256 (P_256 key) sign and verify JWT") {
    for {
      jwt <- CryptoOperationsImp.signJWT(
        senderKeyP256_1,
        data.getBytes,
        // JWAAlgorithm.ES256
      )
      payload = jwt.protectedHeader.toJWTHeader.toOption
      _ = assertEquals(payload.flatMap(_.alg), Some(JWAAlgorithm.ES256))
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
      _ = assertEquals(payload.flatMap(_.alg), Some(JWAAlgorithm.ES256))
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
      _ = assertEquals(payload.flatMap(_.alg), Some(JWAAlgorithm.ES256))
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
      _ = assertEquals(payload.flatMap(_.alg), Some(JWAAlgorithm.EdDSA))
      _ = assertEquals(payload.flatMap(_.kid), Some("did:example:alice#key-1"))
      // _ = println(jwt.base64JWTFormat)
      ret <- CryptoOperationsImp.verifyJWT(
        okp.toPublicKey,
        jwt
      )
    } yield assertEquals(ret, true)
  }

}
