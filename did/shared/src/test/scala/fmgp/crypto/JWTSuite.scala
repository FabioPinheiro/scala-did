package fmgp.crypto

import munit.*
import zio.json.*

import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import fmgp.util.Base64

// didJS/testOnly fmgp.crypto.JWTSuite
// didJVM/testOnly fmgp.crypto.JWTSuite
class JWTSuite extends ZSuite {

  // Test Vectors
  val headerStr = s"""{"typ":"JWT",\r\n "alg":"HS256"}"""
  val headerBytes = Seq(123, 34, 116, 121, 112, 34, 58, 34, 74, 87, 84, 34, 44, 13, 10, 32, 34, 97, 108, 103, 34, 58,
    34, 72, 83, 50, 53, 54, 34, 125).map(_.toByte)
  val headerBase64 = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9"

  val payloadStr = s"""{"iss":"joe",\r\n "exp":1300819380,\r\n "http://example.com/is_root":true}"""
  val payloadBytes =
    Seq(123, 34, 105, 115, 115, 34, 58, 34, 106, 111, 101, 34, 44, 13, 10, 32, 34, 101, 120, 112, 34, 58, 49, 51, 48,
      48, 56, 49, 57, 51, 56, 48, 44, 13, 10, 32, 34, 104, 116, 116, 112, 58, 47, 47, 101, 120, 97, 109, 112, 108, 101,
      46, 99, 111, 109, 47, 105, 115, 95, 114, 111, 111, 116, 34, 58, 116, 114, 117, 101, 125).map(_.toByte)
  val payloadBase64 = "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"

  val expetedBase64JWTFormatWithNoSignature =
    "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"
  val expetedBase64JWTFormatWithNoSignatureBytes = Seq(101, 121, 74, 48, 101, 88, 65, 105, 79, 105, 74, 75, 86, 49, 81,
    105, 76, 65, 48, 75, 73, 67, 74, 104, 98, 71, 99, 105, 79, 105, 74, 73, 85, 122, 73, 49, 78, 105, 74, 57, 46, 101,
    121, 74, 112, 99, 51, 77, 105, 79, 105, 74, 113, 98, 50, 85, 105, 76, 65, 48, 75, 73, 67, 74, 108, 101, 72, 65, 105,
    79, 106, 69, 122, 77, 68, 65, 52, 77, 84, 107, 122, 79, 68, 65, 115, 68, 81, 111, 103, 73, 109, 104, 48, 100, 72,
    65, 54, 76, 121, 57, 108, 101, 71, 70, 116, 99, 71, 120, 108, 76, 109, 78, 118, 98, 83, 57, 112, 99, 49, 57, 121,
    98, 50, 57, 48, 73, 106, 112, 48, 99, 110, 86, 108, 102, 81).map(_.toByte)

  //  HMACs are generated using keys.  This example uses the symmetric key
  //  represented in JSON Web Key [JWK] format below (with line breaks
  //  within values for display purposes only):

  //    {"kty":"oct",
  //     "k":"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75
  //          aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
  //    }

  val expeteHmacSignature = Seq(116, 24, 223, 180, 151, 153, 224, 37, 79, 250, 96, 125, 216, 173, 187, 186, 22, 212, 37,
    77, 105, 214, 191, 240, 91, 88, 5, 88, 83, 132, 141, 121).map(_.toByte)

  val expeteHmacSignatureBase64 = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

  test(s"JWTUnsigned setup checks") {
    assertEquals(headerStr.getBytes().toSeq, headerBytes)
    assertEquals(Base64.encode(headerStr), Base64.fromBase64url(headerBase64))
    assertEquals(payloadStr.getBytes().toSeq, payloadBytes)
    assertEquals(Base64.encode(payloadStr), Base64.fromBase64url(payloadBase64))
  }

  test(s"JWTUnsigned create") {
    JWTUnsigned.fromBase64(headerBase64, payloadBase64) match
      case Left(value)        => fail(value)
      case Right(jwtUnsigned) =>
        assertEquals(jwtUnsigned.base64JWTFormatWithNoSignature, expetedBase64JWTFormatWithNoSignature)
  }

  // test(s"JWTUnsigned HMAC") {
  //   /// TODO
  //   val aaa =
  //   // jwtUnsigned.hmacSha256("{AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow}")
  //   jwtUnsigned.hmacSha256(
  //     """"{"kty":"oct","k":"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"}""""
  //   )
  // assertEquals(aaa.toSeq, expeteHmacSignature)
  // }

}
