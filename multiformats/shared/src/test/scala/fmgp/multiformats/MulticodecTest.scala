package fmgp.multiformats

import munit._

/** multiformatsJVM/testOnly fmgp.multiformats.MulticodecTest */
class MulticodecTest extends FunSuite {
  test("Codec Json is 0x80 0x04") {
    assertEquals(Codec.json.varint.seq, Seq[Byte](-128, 4))
  }

  test("Json warp/unwarp to/from multicodec") {
    val jsonData = """{"a":1}""".getBytes()
    val multicodecParts = Codec.json.multicodec(jsonData)
    assertEquals(multicodecParts.codec, Codec.json)
    assertEquals(multicodecParts.dataBytes.toSeq, jsonData.toSeq)
    val multicodec = multicodecParts.warp
    assertEquals(multicodec.unwrap.codec, Codec.json)
    assertEquals(multicodec.unwrap.dataBytes.toSeq, jsonData.toSeq)
  }
}
