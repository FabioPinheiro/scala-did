package fmgp.did.comm.protocol.auth

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._
import fmgp.crypto.SHA256

/** didCommProtocolsJVM/testOnly fmgp.did.comm.protocol.auth.AuthSuite */
class AuthSuite extends FunSuite {
  val msgAuthRequestExample =
    """{
      |  "id" : "MsgID-1",
      |  "type" : "https://fmgp.app/auth/0.1/request",
      |  "from" : "did:example:alice"
      |}""".stripMargin

  test("Parse an AuthRequest example") {
    val fMsg = msgAuthRequestExample
      .fromJson[PlaintextMessage] match
      case Left(error)  => fail(s"FAIL to parse PlaintextMessage: $error")
      case Right(value) => value

    (fMsg.toAuthRequest) match {
      case Right(msg) =>
        assertEquals(msg.id, MsgID("MsgID-1"))
        assertEquals(msg.from, FROM("did:example:alice"))
        assertEquals(fMsg.`type`, msg.`type`)
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgAuthExample =
    """{
      |  "id" : "MsgID-2",
      |  "type" : "https://fmgp.app/auth/0.1/auth",
      |  "thid": "MsgID-1",
      |  "from" : "did:example:bob",
      |  "to" : [ "did:example:alice" ]
      |}""".stripMargin
  val msgAuthExample_Missing_thid =
    """{
      |  "id" : "MsgID-2",
      |  "type" : "https://fmgp.app/auth/0.1/auth",
      |  "from" : "did:example:bob",
      |  "to" : [ "did:example:alice" ]
      |}""".stripMargin

  test("Parse a Auth example") {
    val fMsg = msgAuthExample
      .fromJson[PlaintextMessage] match
      case Left(error)  => fail(s"FAIL to parse PlaintextMessage: $error")
      case Right(value) => value

    (fMsg.toAuthMsg) match {
      case Right(msg) =>
        assertEquals(msg.id, MsgID("MsgID-2"))
        assertEquals(msg.thid, MsgID("MsgID-1"))
        assertEquals(msg.to, TO("did:example:alice"))
        assertEquals(msg.from, FROM("did:example:bob"))
        assertEquals(fMsg.`type`, msg.`type`)
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  test("Fail to parse an Auth example with missing thid") {
    val fMsg = msgAuthExample_Missing_thid
      .fromJson[PlaintextMessage] match
      case Left(error)  => fail(s"FAIL to parse PlaintextMessage: $error")
      case Right(value) => value

    (fMsg.toAuthMsg) match {
      case Right(msg)  => fail("fMsg MUST fail with missing a thid")
      case Left(error) => assertEquals(error, "'https://fmgp.app/auth/0.1/auth' MUST have field 'thid'")
    }
  }

  test("From an AuthRequest example to AuthMsg") {
    val fMsg = msgAuthRequestExample
      .fromJson[PlaintextMessage] match
      case Left(error)  => fail(s"FAIL to parse PlaintextMessage: $error")
      case Right(value) => value

    (fMsg.toAuthRequest) match {
      case Left(error)        => fail(s"fMsg MUST be Right but is ${Left(error)}")
      case Right(authRequest) =>
        val authMsg = authRequest.replyWithAuth(FROM("did:example:bob"))
        assertNotEquals(authMsg.id, MsgID("MsgID-1"))
        assertEquals(authMsg.to, TO("did:example:alice"))
        assertEquals(authMsg.from, FROM("did:example:bob"))
        assertEquals(authMsg.thid, authRequest.id)
        assertNotEquals(authMsg.`type`, authRequest.`type`)
      // println(authMsg.toPlaintextMessage.toJson)
    }
  }

}
