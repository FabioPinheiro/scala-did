package fmgp.util

import munit._
import zio.json._

// didJVM/testOnly fmgp.util.Base64Suite
class Base64Suite extends FunSuite {

  val examples = Seq(
    ("", "", ""),
    ("f", "Zg==", "Zg"),
    ("fo", "Zm8=", "Zm8"),
    ("foo", "Zm9v", "Zm9v"),
    ("foob", "Zm9vYg==", "Zm9vYg"),
    ("fooba", "Zm9vYmE=", "Zm9vYmE"),
    ("foobar", "Zm9vYmFy", "Zm9vYmFy"),
    ("""f{}">?L`{+""", "Znt9Ij4/TGB7Kw==", "Znt9Ij4_TGB7Kw"),
    (
      """{"id":"2d460509-9a7e-4bfb-9edc-0050299dec81","type":"https://didcomm.org/out-of-band/2.0/invitation","from":"did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ","body":{"goal_code":"request-mediate","goal":"RequestMediate","accept":["didcomm/v2"]},"typ":"application/didcomm-plain+json"}""",
      "eyJpZCI6IjJkNDYwNTA5LTlhN2UtNGJmYi05ZWRjLTAwNTAyOTlkZWM4MSIsInR5cGUiOiJodHRwczovL2RpZGNvbW0ub3JnL291dC1vZi1iYW5kLzIuMC9pbnZpdGF0aW9uIiwiZnJvbSI6ImRpZDpwZWVyOjIuRXo2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eS5WejZNa2hoMWU1Q0VZWXE2SkJVY1RaNkNwMnJhbkNXUnJ2N1lheDNMZTRONTlSNmRkLlNleUowSWpvaVpHMGlMQ0p6SWpvaWFIUjBjSE02THk5aGJHbGpaUzVrYVdRdVptMW5jQzVoY0hBdklpd2ljaUk2VzEwc0ltRWlPbHNpWkdsa1kyOXRiUzkyTWlKZGZRIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJyZXF1ZXN0LW1lZGlhdGUiLCJnb2FsIjoiUmVxdWVzdE1lZGlhdGUiLCJhY2NlcHQiOlsiZGlkY29tbS92MiJdfSwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1wbGFpbitqc29uIn0=",
      "eyJpZCI6IjJkNDYwNTA5LTlhN2UtNGJmYi05ZWRjLTAwNTAyOTlkZWM4MSIsInR5cGUiOiJodHRwczovL2RpZGNvbW0ub3JnL291dC1vZi1iYW5kLzIuMC9pbnZpdGF0aW9uIiwiZnJvbSI6ImRpZDpwZWVyOjIuRXo2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eS5WejZNa2hoMWU1Q0VZWXE2SkJVY1RaNkNwMnJhbkNXUnJ2N1lheDNMZTRONTlSNmRkLlNleUowSWpvaVpHMGlMQ0p6SWpvaWFIUjBjSE02THk5aGJHbGpaUzVrYVdRdVptMW5jQzVoY0hBdklpd2ljaUk2VzEwc0ltRWlPbHNpWkdsa1kyOXRiUzkyTWlKZGZRIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJyZXF1ZXN0LW1lZGlhdGUiLCJnb2FsIjoiUmVxdWVzdE1lZGlhdGUiLCJhY2NlcHQiOlsiZGlkY29tbS92MiJdfSwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1wbGFpbitqc29uIn0"
    ),
  )

  examples.foreach { case (input, expeted, expetedUrl) =>
    test("Check Base64 '" + input + "'") {
      assertEquals(Base64.encode(input).decodeToString, input, "encode and decode")
      assertEquals(Base64.encode(input).urlBase64, expetedUrl, "to url")
      assertEquals(Base64.encode(input).basicBase64, expeted, "to base64")

      assertEquals(Base64.fromBase64url(expetedUrl).decodeToString, input, "from url")
      assertEquals(Base64.fromBase64(expeted).decodeToString, input, "from base64")
    }
  }

  case class A(b: B)
  object A {
    given decoder: JsonDecoder[A] = DeriveJsonDecoder.gen[A]
    given encoder: JsonEncoder[A] = DeriveJsonEncoder.gen[A]
  }
  case class B(x: String, y: Int)
  object B {
    given decoder: JsonDecoder[B] = DeriveJsonDecoder.gen[B]
    given encoder: JsonEncoder[B] = DeriveJsonEncoder.gen[B]
  }

  test("Check Base64Obj") {
    val obj = Base64Obj(A(B("xx", 123)))
    val expected = """"eyJiIjp7IngiOiJ4eCIsInkiOjEyM319"""" // {"b":{"x":"xx","y":123}}
    assertEquals(obj.toJson, expected)

    expected.fromJson[Base64Obj[A]] match {
      case Left(error)  => fail(error)
      case Right(value) => assertEquals(value.obj, obj.obj)
    }
  }

  test("Keep origin encoding (do not change the order of encoded JSON)") {
    val obj = Base64Obj(A(B("xx", 123)))
    val expected1 = """"eyJiIjp7InkiOjEyMywieCI6Inh4In19"""" // {"b":{"y":123,"x":"xx"}}
    expected1.fromJson[Base64Obj[A]] match {
      case Left(error)  => fail(error)
      case Right(value) =>
        assertEquals(value.obj, obj.obj)
        assertEquals(value.toJson, expected1)
    }

    val expected2 = """"eyJiIjp7IngiOiJ4eCIsInkiOjEyM319"""" // {"b":{"x":"xx","y":123}}
    expected2.fromJson[Base64Obj[A]] match {
      case Left(error)  => fail(error)
      case Right(value) =>
        assertEquals(value.obj, obj.obj)
        assertEquals(value.toJson, expected2)
    }
  }

}
