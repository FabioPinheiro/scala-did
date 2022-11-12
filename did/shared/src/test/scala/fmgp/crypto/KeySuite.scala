package fmgp.crypto

import munit._
import zio.json._
import fmgp.did.DIDDocument
import zio.json.ast.Json

class KeySuite extends FunSuite {

  test("parse serialize Curve P-256") {
    assertEquals(Curve.`P-256`.toJson, """"P-256"""")
    assertEquals(Curve.`P-256`.toJson.fromJson[Curve], Right(Curve.`P-256`))
  }

  test("parse serialize Curve P-384") {
    assertEquals(Curve.`P-384`.toJson, """"P-384"""")
    assertEquals(Curve.`P-384`.toJson.fromJson[Curve], Right(Curve.`P-384`))
  }

  test("parse serialize Curve P-521") {
    assertEquals(Curve.`P-521`.toJson, """"P-521"""")
    assertEquals(Curve.`P-521`.toJson.fromJson[Curve], Right(Curve.`P-521`))
  }

  test("parse serialize Curve secp256k1") {
    assertEquals(Curve.secp256k1.toJson, """"secp256k1"""")
    assertEquals(Curve.secp256k1.toJson.fromJson[Curve], Right(Curve.secp256k1))
  }

  test("parse and stringify PrivateKey") {
    val ret = JWKExamples.senderKeySecp256k1.fromJson[PrivateKey]
    ret match {
      case Left(error) => fail(error)
      case Right(obj: ECPrivateKey) =>
        assertEquals(obj.kid, Some("did:example:alice#key-3"))
        assertEquals(obj.kty, KTY.EC)
        assertEquals(obj.crv, Curve.secp256k1)
        assertEquals(obj.d, "N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA")
        assertEquals(obj.x, "aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk")
        assertEquals(obj.y, "JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk")
        assertEquals(obj.toJsonAST, JWKExamples.senderKeySecp256k1.fromJson[Json]) // stringify ECPrivateKey
      case Right(obj: OKPPrivateKey) => fail("senderKeySecp256k1 is not a OKP key")
    }
    assertEquals(
      ret.map(_.toJsonAST),
      JWKExamples.senderKeySecp256k1.fromJson[Json].map(_.toJsonAST)
    ) // stringify PrivateKey
  }

  test("parse and stringify PublicKey") {
    val ret = JWKExamples.senderKeySecp256k1.fromJson[PublicKey]
    ret match {
      case Left(error) => fail(error)
      case Right(obj: ECPublicKey) =>
        assertEquals(obj.kid, Some("did:example:alice#key-3"))
        assertEquals(obj.kty, KTY.EC)
        assertEquals(obj.crv, Curve.secp256k1)
        assertEquals(obj.x, "aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk")
        assertEquals(obj.y, "JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk")
      case Right(obj: OKPPublicKey) => fail("senderKeySecp256k1 is not a OKP key")
    }
  }

  test("parse and stringify PublicKey with no kid") {
    val tmp = """{"kty":"OKP","crv":"X25519","x":"GFcMopJljf4pLZfch4a_GhTM_YAf6iNI1dWDGyVCaw0"}"""
    val ret = tmp.fromJson[PublicKey]

    ret match {
      case Left(error)              => fail(error)
      case Right(obj: OKPPublicKey) =>
        // assertEquals(obj.kid, None)
        assertEquals(obj.kty, KTY.OKP)
        assertEquals(obj.crv, Curve.X25519)
        assertEquals(obj.x, "GFcMopJljf4pLZfch4a_GhTM_YAf6iNI1dWDGyVCaw0")
        assertEquals(obj.toJson, tmp) // stringify OKPPublicKey
      case Right(obj: ECPublicKey) => fail("senderKeySecp256k1 is not a EC key")
    }
    assertEquals(ret.toOption.get.toJson, tmp) // stringify PublicKey
  }

  test("parse ECKey into ECPrivateKey".ignore) {} // TODO
  test("parse ECKey into ECPublicKey".ignore) {} // TODO

}
