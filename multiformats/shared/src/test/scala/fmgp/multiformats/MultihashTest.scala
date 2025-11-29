package fmgp.multiformats

import munit.*

/** multiformatsJVM/testOnly fmgp.multiformats.MultihashTest */
class MultihashTest extends FunSuite {
  test("Encode SHA2-256") {
    val hashHex = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    val hashBytes = hashHex.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toSeq
    // println(hashBytes.map(b => String.format("%02X", b)).mkString)
    assertEquals(hashBytes.length, 32)
    val multihash = Multihash(Codec.sha2_256, hashBytes.toArray)
    assertEquals(multihash.bytes.toSeq, Seq[Byte](0x12, 0x20) ++ hashBytes)
  }

}
