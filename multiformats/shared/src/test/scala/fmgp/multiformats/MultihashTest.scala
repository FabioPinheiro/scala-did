package fmgp.multiformats

import munit._

/** multiformatsJVM/testOnly fmgp.multiformats.MultihashTest */
class MultihashTest extends FunSuite {
  test("Encode SHA2-256") {
    val hashHex = "12209cbc07c3f991725836a3aa2a581ca2029198aa420b9d99bc0e131d9f3e2cbe47"
    val hashBytes = hashHex.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toSeq
    val multihash = Multihash(Codec.sha2_256, 32, hashBytes.toArray)
    assertEquals(multihash.bytes.toSeq, Seq[Byte](0x12, 0x20) ++ hashBytes)
  }

}
