package fmgp.did.comm

import munit._

import fmgp.did._
import fmgp.crypto._

import zio._
import zio.json._
import zio.json.ast.Json

/** didJVM/testOnly fmgp.did.comm.EncryptedMessageSuite_SHA1 */
class EncryptedMessageSuite_SHA1 extends ZSuite {

  val expected = "c4b4c8ecc03bc2595593074d432e516aaf6cee72"
  val expectedSHA256 = "101183ce5019f4a7d4f1b14d61e64bf834df740d2d77aa2e535824533edc3b87"

  test("hash of the EncryptedMessage must be deterministic") {
    val obj = EncryptedMessageExamples.obj_encryptedMessage_ECDHES_X25519_XC20P
    assertEquals(obj.sha256, expectedSHA256)
  }

  test("hash of json string of the EncryptedMessage must be deterministic (and the same)") {
    val aux = EncryptedMessageExamples.encryptedMessage_ECDHES_X25519_XC20P.replaceAll(" ", "").replaceAll("\n", "")
    // assertEquals(SHA1.digestToHex(aux), expected)
    assertEquals(SHA256.digestToHex(aux), expectedSHA256)
  }

}
