package fmgp.multiformats

import munit._

/** multiformatsJVM/testOnly fmgp.multiformats.VarintTest */
class VarintTest extends FunSuite {

  // 1 (0x01)        => 00000001 (0x01)
  // 127 (0x7f)      => 01111111 (0x7f)
  // 128 (0x80)      => 10000000 00000001 (0x8001)
  // 255 (0xff)      => 11111111 00000001 (0xff01)
  // 300 (0x012c)    => 10101100 00000010 (0xac02)
  // 16384 (0x4000)  => 10000000 10000000 00000001 (0x808001)

  test("encode 1 to Varint") {
    assertEquals(Varint.encodeInt(1).seq, Seq[Byte](0x01))
    assertEquals(Varint.encodeLong(1).seq, Seq[Byte](0x01))
  }
  test("encode 127 to Varint") {
    assertEquals(Varint.encodeInt(127).seq, Seq[Byte](0x7f))
    assertEquals(Varint.encodeLong(127).seq, Seq[Byte](0x7f))
  }
  test("encode 128 to Varint") {
    assertEquals(Varint.encodeInt(128).seq, Seq[Byte](0x80.toByte, 0x01))
    assertEquals(Varint.encodeLong(128).seq, Seq[Byte](0x80.toByte, 0x01))
  }
  test("encode 255 to Varint") {
    assertEquals(Varint.encodeInt(255).seq, Seq[Byte](0xff.toByte, 0x01))
    assertEquals(Varint.encodeLong(255).seq, Seq[Byte](0xff.toByte, 0x01))
  }
  test("encode 300 to Varint") {
    assertEquals(Varint.encodeInt(300).seq, Seq[Byte](0xac.toByte, 0x02))
    assertEquals(Varint.encodeLong(300).seq, Seq[Byte](0xac.toByte, 0x02))
  }
  test("encode 16384 to Varint") {
    assertEquals(Varint.encodeInt(16384).seq, Seq[Byte](0x80.toByte, 0x80.toByte, 0x01))
    assertEquals(Varint.encodeLong(16384).seq, Seq[Byte](0x80.toByte, 0x80.toByte, 0x01))
  }

  test("encode 200 (Multicodec json) to Varint") {
    assertEquals(Varint.encodeInt(200).seq, Seq[Byte](0xc8.toByte, 0x01.toByte))
  }

  test("encode an Int to Varint and decode it back") {
    val rand = new scala.util.Random
    for (i <- 1 to 100) {
      val num = rand.nextInt
      val encoded = Varint.encodeInt(num)
      val ret = Varint.decodeToInt(encoded).getOrElse(???)
      assertEquals(obtained = ret._1, expected = num)
      assertEquals(obtained = ret._2, expected = encoded.length)
    }
  }

  test("encode an Long to Varint and decode it back") {
    val rand = new scala.util.Random
    for (i <- 1 to 100) {
      val num = rand.nextLong
      val encoded = Varint.encodeLong(num)
      val ret = Varint.decodeToLong(encoded).getOrElse(???)
      assertEquals(obtained = ret._1, expected = num)
      assertEquals(obtained = ret._2, expected = encoded.length)
    }
  }

  test("throw exception if the given bytes is not Varint bytes: empty") {
    interceptMessage[java.lang.IllegalArgumentException]("Cannot find the ending Byte") {
      Varint(Array.emptyByteArray).unsafeDecodeInt
    }
  }
  test("throw exception if the given bytes is not Varint bytes: 0xff") {
    interceptMessage[java.lang.IllegalArgumentException]("Cannot find the ending Byte") {
      Varint(Array(0xff.toByte)).unsafeDecodeInt
    }
  }
  test("throw exception if the given bytes is not Varint bytes: 0xff 0x8f") {
    interceptMessage[java.lang.IllegalArgumentException]("Cannot find the ending Byte") {
      Varint(Array(0xff.toByte, 0x8f.toByte)).unsafeDecodeInt
    }
  }
  test("throw exception if the given bytes is not Varint bytes: 0x8f") {
    interceptMessage[java.lang.IllegalArgumentException]("Cannot find the ending Byte") {
      Varint(Array(0x8f.toByte)).unsafeDecodeInt
    }
  }

  test("should be able to extract the valid length of Varint from bytes, given offset.") {
    assertEquals(Varint.extractLength(Array(), 0), 0)
    assertEquals(Varint.extractLength(Array(), 1), 0)
    assertEquals(Varint.extractLength(Array(0xff.toByte), 0), 0)
    assertEquals(Varint.extractLength(Array(0xff.toByte, 0x8f.toByte), 0), 0)
    assertEquals(Varint.extractLength(Array(0x8f.toByte), 0), 0)

    assertEquals(Varint.extractLength(Array(0xff.toByte, 0x0f.toByte), 0), 2)
    assertEquals(Varint.extractLength(Array(0xff.toByte, 0x0f.toByte), 1), 1)
    assertEquals(Varint.extractLength(Array(0xff.toByte, 0xff.toByte, 0x8f.toByte, 0x8f.toByte, 0x0f.toByte), 0), 5)
    assertEquals(Varint.extractLength(Array(0x80.toByte, 0x80.toByte, 0x8f.toByte, 0x82.toByte, 0x01.toByte), 2), 3)

    assertEquals(
      Varint.extractLength(
        Array(0x80.toByte, 0x80.toByte, 0x8f.toByte, 0x82.toByte, 0x01.toByte, 0x8f.toByte, 0x8f.toByte, 0x8f.toByte),
        0
      ),
      5
    )
  }

}
