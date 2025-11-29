package fmgp.util

import munit.*
import zio.json.*

// didJVM/testOnly fmgp.util.HexSuite
class HexSuite extends FunSuite {

  val testVector = Seq(
    ("1a", Seq(26), "A simple two-digit hex value is converted to a single byte."),
    ("00", Seq(0), "Handles the case of a leading zero in the hex representation."),
    ("ff", Seq(255), "A byte with the maximum value in the range 0-255."),
    ("12345678", Seq(18, 52, 86, 120), "Converts a longer hex string into multiple bytes."),
    ("0A0B0C0D", Seq(10, 11, 12, 13), "Demonstrates the conversion of hex values A-F."),
    ("", Seq(), "Handles the edge case of an empty input string."),
    ("AABBCC", Seq(170, 187, 204), "Tests mixed-case hex characters (A-F and a-f are treated the same)."),
    ("123", Seq(1, 35), "Handles odd-length hex strings by padding with a leading zero."),
    ("FFFF", Seq(255, 255), "Represents the maximum value that can be stored in two bytes."),
    ("xftfyffz", Seq(255, 255), "Ignore all no Hex characters.")
  )

  testVector.foreach { case (input, expeted, explanation) =>
    test(s"hex2bytes '$input' ($explanation)") {
      assertEquals(
        hex2bytes(input).toSeq,
        expeted.map(_.toByte),
        "decode hex"
      )
    }
  }
  testVector.foreach { case (input, expeted, explanation) =>
    test(s"hex2bytes&hex2bytes hex format will converge '$input'") {
      assertEquals(
        hex2bytes(bytes2Hex(hex2bytes(input))).toSeq,
        expeted.map(_.toByte),
      )
      assertEquals(
        bytes2Hex(hex2bytes(input)),
        bytes2Hex(hex2bytes(bytes2Hex(hex2bytes(input)))),
      )
    }
  }
}
