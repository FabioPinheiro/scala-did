package fmgp.multiformats

import munit._

class varintTest extends FunSuite {

  // 1 (0x01)        => 00000001 (0x01)
  // 127 (0x7f)      => 01111111 (0x7f)
  // 128 (0x80)      => 10000000 00000001 (0x8001)
  // 255 (0xff)      => 11111111 00000001 (0xff01)
  // 300 (0x012c)    => 10101100 00000010 (0xac02)
  // 16384 (0x4000)  => 10000000 10000000 00000001 (0x808001)

  test("encode 1 to Varint") {
    assertEquals(Varint.encodeInt(1).value.toSeq, Array[Byte](0x01).toSeq)
    assertEquals(Varint.encodeLong(1).value.toSeq, Array[Byte](0x01).toSeq)
  }
  test("encode 127 to Varint") {
    assertEquals(Varint.encodeInt(127).value.toSeq, Array[Byte](0x7f).toSeq)
    assertEquals(Varint.encodeLong(127).value.toSeq, Array[Byte](0x7f).toSeq)
  }
  test("encode 128 to Varint") {
    assertEquals(Varint.encodeInt(128).value.toSeq, Array[Byte](0x80.toByte, 0x01).toSeq)
    assertEquals(Varint.encodeLong(128).value.toSeq, Array[Byte](0x80.toByte, 0x01).toSeq)
  }
  test("encode 255 to Varint") {
    assertEquals(Varint.encodeInt(255).value.toSeq, Array[Byte](0xff.toByte, 0x01).toSeq)
    assertEquals(Varint.encodeLong(255).value.toSeq, Array[Byte](0xff.toByte, 0x01).toSeq)
  }
  test("encode 300 to Varint") {
    assertEquals(Varint.encodeInt(300).value.toSeq, Array[Byte](0xac.toByte, 0x02).toSeq)
    assertEquals(Varint.encodeLong(300).value.toSeq, Array[Byte](0xac.toByte, 0x02).toSeq)
  }
  test("encode 16384 to Varint") {
    assertEquals(Varint.encodeInt(16384).value.toSeq, Array[Byte](0x80.toByte, 0x80.toByte, 0x01).toSeq)
    assertEquals(Varint.encodeLong(16384).value.toSeq, Array[Byte](0x80.toByte, 0x80.toByte, 0x01).toSeq)
  }

  test("encode an Int to Varint and decode it back") {
    val rand = new scala.util.Random
    for (i <- 1 to 100) {
      val num = rand.nextInt
      val ret = Varint.decodeToInt(Varint.encodeInt(num))._1
      assertEquals(obtained = ret, expected = num)
    }
  }

  test("encode an Long to Varint and decode it back") {
    val rand = new scala.util.Random
    for (i <- 1 to 100) {
      val num = rand.nextLong
      val ret = Varint.decodeToLong(Varint.encodeLong(num))._1
      assertEquals(obtained = ret, expected = num)
    }
  }

  test("throw exception if the given bytes is not Varint bytes: empty") {
    interceptMessage[java.lang.IllegalArgumentException]("Cannot find the ending Byte") {
      Varint.decodeToInt(Varint.unsafe(Array.emptyByteArray))
    }
  }
  test("throw exception if the given bytes is not Varint bytes: 0xff") {
    interceptMessage[java.lang.IllegalArgumentException]("Cannot find the ending Byte") {
      Varint.decodeToInt(Varint.unsafe(Array(0xff.toByte)))
    }
  }
  test("throw exception if the given bytes is not Varint bytes: 0xff 0x8f") {
    interceptMessage[java.lang.IllegalArgumentException]("Cannot find the ending Byte") {
      Varint.decodeToInt(Varint.unsafe(Array(0xff.toByte, 0x8f.toByte)))
    }
  }
  test("throw exception if the given bytes is not Varint bytes: 0x8f") {
    interceptMessage[java.lang.IllegalArgumentException]("Cannot find the ending Byte") {
      Varint.decodeToInt(Varint.unsafe(Array(0x8f.toByte)))
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
  }

}
