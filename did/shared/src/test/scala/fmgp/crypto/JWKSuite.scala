package fmgp.crypto

import munit.*
import zio.json.*
import fmgp.did.DIDDocument

class JWKSuite extends FunSuite {

  test("JwkCruve test") {
    val ret = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKeyWithKid]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  =>
        assertEquals(obj.kid, "did:example:alice#key-3")
        assertEquals(obj.kty, KTY.EC)
        assertEquals(obj.d, "N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA")
        assertEquals(obj.x, "aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk")
        assertEquals(obj.y, "JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk")

    }
  }

}
