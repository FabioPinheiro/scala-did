package fmgp.crypto

import munit._
import zio.json._

import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex

// didJS/testOnly fmgp.crypto.SHASuite
// didJVM/testOnly fmgp.crypto.SHASuite
class SHASuite extends ZSuite {

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

  val testVectorsFromBytes = Seq(
    (
      Array.emptyByteArray,
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    ),
    (
      Array(211.toByte, 212.toByte),
      "182889f925ae4e5cc37118ded6ed87f7bdc7cab5ec5e78faef2e50048999473f"
    ),
    (
      hex2bytes("123"),
      "3d73c0c831c942c1996ca667b639970e571d58c6b7b996e4082a6d091be0b956"
    ),
    (
      hex2bytes("1234567891"),
      "1ce98679f2f7a245044742f558e89d8e2adf0ac80e2d4a2449f49c614b25e9ed"
    ),
    (
      hex2bytes(
        "0a3f0a3d123b0a076d61737465723010014a2e0a09736563703235366b311221021456f5dd7bcddca7a3e48edad8cc68d0ce6a7a5991492cb48bac817c2e4d9adc"
      ),
      "00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae" // "did:prism:"
    )
  )

  // FIXME
  // testVectors.foreach { (input, sha1out, _) =>
  //   val n = 20
  //   test(s"SHA-1 digest '${if (input.size > 20) input.slice(0, n) ++ "..." else input}'") {
  //     assertEquals(SHA1.digestToHex(input), sha1out)
  //     assertEquals(SHA1.digestToHex(input.getBytes()), sha1out)
  //     assertEquals(SHA1.digest(input).toSeq, hex2bytes(sha1out).toSeq)
  //     assertEquals(SHA1.digest(input.getBytes()).toSeq, hex2bytes(sha1out).toSeq)
  //   }
  // }

  testVectors.foreach { (input, _, sha256out) =>
    val n = 20
    test(s"SHA-256 Basic digest '${if (input.size > 20) input.slice(0, n) ++ "..." else input}'") {
      assertEquals(SHA256.digestToHex(input), sha256out)
      assertEquals(SHA256.digestToHex(input.getBytes()), sha256out)
      assertEquals(SHA256.digest(input).toSeq, hex2bytes(sha256out).toSeq)
      assertEquals(SHA256.digest(input.getBytes()).toSeq, hex2bytes(sha256out).toSeq)
    }
  }

  testVectors.foreach { (input, _, sha256out) =>
    val n = 20
    testZ(s"SHA-256 ZIO digest '${if (input.size > 20) input.slice(0, n) ++ "..." else input}'".ignore) {
      for {
        a <- SHA256ZIO.digestToHex(input)
        _ = assertEquals(a, sha256out)
        b <- SHA256ZIO.digestToHex(input.getBytes())
        _ = assertEquals(b, sha256out)
        c <- SHA256ZIO.digest(input)
        _ = assertEquals(c.toSeq, hex2bytes(sha256out).toSeq)
        d <- SHA256ZIO.digest(input.getBytes())
        _ = assertEquals(d.toSeq, hex2bytes(sha256out).toSeq)
      } yield ()
    }
  }

  testVectorsFromBytes.foreach { (input, sha256out) =>
    val hex = bytes2Hex(input)
    val n = 20
    test(s"SHA-256 Basic digest from bytes:'${if (hex.size > 20) hex.slice(0, n) ++ "..." else hex}'") {
      assertEquals(SHA256.digestToHex(input), sha256out)
      assertEquals(SHA256.digest(input).toSeq, hex2bytes(sha256out).toSeq)
    }
  }

}
