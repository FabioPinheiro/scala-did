package fmgp.did.comm

import munit.*
import zio.json.*
import scala.util.chaining.*

import fmgp.did.*
import java.net.{URI, URL}
import fmgp.util.Base64

/** didJVM/testOnly fmgp.did.comm.OutOfBandSuite */
class OutOfBandSuite extends FunSuite {

  val oobMessage = """{
    |"type":"https://didcomm.org/out-of-band/2.0/invitation",
    |"id":"599f3638-b563-4937-9487-dfe55099d900",
    |"from":"did:example:verifier",
    |"body":{"goal_code":"streamlined-vp","accept":["didcomm/v2"]}
    |}""".stripMargin
  val oobMessageBase64 =
    "eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNTk5ZjM2MzgtYjU2My00OTM3LTk0ODctZGZlNTUwOTlkOTAwIiwiZnJvbSI6ImRpZDpleGFtcGxlOnZlcmlmaWVyIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJzdHJlYW1saW5lZC12cCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ=="
  val oobURI = s"""https://example.com/some/path?_oob=$oobMessageBase64"""

  test(s"OutOfBand invitation as PlaintextMessage") {
    assert(OutOfBand.parse(oobURI).isDefined)
    val ret = OutOfBand.oob(oobURI)
    val expeted = oobMessage.fromJson[PlaintextMessage].tap(o => assert(o.isRight)).getOrElse(fail("Must be Right"))

    assert(ret.isRight)
    ret match
      case Left(value)                                   => fail("Must be Right")
      case Right(OutOfBand(msg: PlaintextMessage, data)) => assertEquals(msg, expeted)
      case Right(OutOfBand(msg: SignedMessage, data))    => fail("Must be an OutOfBand with SignedMessage")
      case Right(OutOfBand(msg: EncryptedMessage, data)) => fail("Must be an OutOfBand with EncryptedMessage")
  }

  test(s"OutOfBand invitation as PlaintextMessage from Mediator") {
    val qrcode =
      "https://did.fmgp.app/#/?_oob=eyJpZCI6IjJkNDYwNTA5LTlhN2UtNGJmYi05ZWRjLTAwNTAyOTlkZWM4MSIsInR5cGUiOiJodHRwczovL2RpZGNvbW0ub3JnL291dC1vZi1iYW5kLzIuMC9pbnZpdGF0aW9uIiwiZnJvbSI6ImRpZDpwZWVyOjIuRXo2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eS5WejZNa2hoMWU1Q0VZWXE2SkJVY1RaNkNwMnJhbkNXUnJ2N1lheDNMZTRONTlSNmRkLlNleUowSWpvaVpHMGlMQ0p6SWpvaWFIUjBjSE02THk5aGJHbGpaUzVrYVdRdVptMW5jQzVoY0hBdklpd2ljaUk2VzEwc0ltRWlPbHNpWkdsa1kyOXRiUzkyTWlKZGZRIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJyZXF1ZXN0LW1lZGlhdGUiLCJnb2FsIjoiUmVxdWVzdE1lZGlhdGUiLCJhY2NlcHQiOlsiZGlkY29tbS92MiJdfSwidHlwIjoiYXBwbGljYXRpb24vZGlkY29tbS1wbGFpbitqc29uIn0"

    assert(OutOfBand.oob(qrcode).isRight)
  }

  val tmp = "e30=" // oobMessageBase64 '{}'
  val tmp2 = "eyJubyI6MH0=" // oobMessageBase64 '{"no":0}'
  Seq(
    (s"""?_oob=$tmp""", true), // +- undefined behavior => because is not really a complete url
    (s"""https://d?_oob=$tmp""", true),
    (s"""https://d?_oob=$tmp&none""", true),
    (s"""https://d?_oob=$tmp&_oob=""", true),
    (s"""https://d?_oob=$tmp&_oob=$tmp2""", true), //  finding the '_oob' must be greedy
    (s"""https://d/path?_oob=$tmp""", true),
    (s"""https://d/path?oob=$tmp""", false), // missing '_' from _oob
    (s"""https://d/path?_oob=""", false), // missing data for _oob
    (s"""https://d/path?_oob=$tmp#""", true),
    (s"""https://d/path?_oob=$tmp#f""", true),
    (s"""https://d/path?_oob=$tmp#""", true),
    (s"https://d?_oob=$tmp#_oob=$tmp2", true),

    // TODO Behavior may change if issues doesn't go through. See https://github.com/decentralized-identity/didcomm-messaging/issues/443
    (s"https://d/path#_oob=$tmp", true),
    (s"#_oob=$tmp", true),
    (s"https://d/path#_oob=$tmp", true),
    (s"https://d/path#_oob=$tmp&", true),
    (s"https://d/path#_oob=$tmp&_oob=$tmp2", true), // finding the '_oob' must be greedy
    (s"https://d/path#_oob=$tmp&test=1", true),
    (s"https://d/path#test=1&_oob=$tmp", true),
    (s"https://d/path#test=1&test2=$tmp", false),
  ).foreach {
    case (uri, true) =>
      test(s"OutOfBand oob will parse '$uri'") {
        assertEquals(OutOfBand.parse(uri).map(_.basicBase64), Some(tmp))
      }
    case (uri, false) =>
      test(s"OutOfBand oob will NOT parse '$uri'") {
        assert(OutOfBand.parse(uri).isEmpty)
      }
  }

}
