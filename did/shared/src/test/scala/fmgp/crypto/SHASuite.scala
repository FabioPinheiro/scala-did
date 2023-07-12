package fmgp.crypto

import munit._
import zio.json._

import fmgp.util.hex2bytes

// fmgp.crypto.SHASuite
class SHASuite extends FunSuite {

  val testVectors = Seq(
    (
      "abc",
      "a9993e364706816aba3e25717850c26c9cd0d89d",
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    ),
    (
      "",
      "da39a3ee5e6b4b0d3255bfef95601890afd80709",
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    ),
    (
      "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
      "84983e441c3bd26ebaae4aa1f95129e5e54670f1",
      "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1"
    ),
    (
      "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu",
      "a49b2446a02c645bf419f995b67091253a04a259",
      "cf5b16a778af8380036ce59e7b0492370b249b11e8f07a51afac45037afee9d1"
    ),
    (
      "a" * 1000000,
      "34aa973cd4c4daa4f61eeb2bdbad27316534016f",
      "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0"
    ),
  )

  testVectors.foreach { (input, sha1out, _) =>
    val n = 20
    test(s"SHA-1 digest '${if (input.size > 20) input.slice(0, n) ++ "..." else input}'") {
      assertEquals(SHA1.digestToHex(input), sha1out)
      assertEquals(SHA1.digestToHex(input.getBytes()), sha1out)
      assertEquals(SHA1.digest(input).toSeq, hex2bytes(sha1out).toSeq)
      assertEquals(SHA1.digest(input.getBytes()).toSeq, hex2bytes(sha1out).toSeq)
    }
  }

  testVectors.foreach { (input, _, sha256out) =>
    val n = 20
    test(s"SHA-256 digest '${if (input.size > 20) input.slice(0, n) ++ "..." else input}'") {
      assertEquals(SHA256.digestToHex(input), sha256out)
      assertEquals(SHA256.digestToHex(input.getBytes()), sha256out)
      assertEquals(SHA256.digest(input).toSeq, hex2bytes(sha256out).toSeq)
      assertEquals(SHA256.digest(input.getBytes()).toSeq, hex2bytes(sha256out).toSeq)
    }
  }

}
