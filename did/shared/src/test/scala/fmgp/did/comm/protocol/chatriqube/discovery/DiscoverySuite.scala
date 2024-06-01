package fmgp.did.comm.protocol.chatriqube.discovery

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._
import fmgp.did.comm.protocol.chatriqube._

/** didJVM/testOnly fmgp.did.comm.protocol.chatriqube.discovery.DiscoverySuite */
class DiscoverySuite extends FunSuite {
  val msgRequestWithToExample =
    """{
      |  "id" : "MsgID-1",
      |  "type" : "https://decentriqube.com/discovery/1/request",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "body" : {
      |    "subject_type" : "Email",
      |    "subject" : "test@fmgp.app"
      |  }
      |}""".stripMargin

  test("Parse a Chatriqube Discovery Request (without TO) example") {
    val fMsg = msgRequestWithToExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toRequest

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id.value, "MsgID-1")
        assertEquals(msg.piuri.value, "https://decentriqube.com/discovery/1/request")
        assertEquals(msg.to, Some(TO("did:example:bob")))
        assertEquals(msg.from, FROM("did:example:alice"))
        assertEquals(msg.subjectType, SubjectType.Email)
        assertEquals(msg.subject, "test@fmgp.app")
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgRequestWithoutExample =
    """{
      |  "id" : "MsgID-1",
      |  "type" : "https://decentriqube.com/discovery/1/request",
      |  "from" : "did:example:alice",
      |  "body" : {
      |    "subject_type" : "Email",
      |    "subject" : "test@fmgp.app"
      |  }
      |}""".stripMargin

  test("Parse a Chatriqube Discovery Request (with TO) example") {
    val fMsg = msgRequestWithoutExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toRequest

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id.value, "MsgID-1")
        assertEquals(msg.piuri.value, "https://decentriqube.com/discovery/1/request")
        assertEquals(msg.to, None)
        assertEquals(msg.from, FROM("did:example:alice"))
        assertEquals(msg.subjectType, SubjectType.Email)
        assertEquals(msg.subject, "test@fmgp.app")
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgAnswerExample =
    """{
      |  "id" : "MsgID-2",
      |  "type" : "https://decentriqube.com/discovery/1/answer",
      |  "to" : ["did:example:alice"],
      |  "from" : "did:example:bob",
      |  "thid" : "MsgID-1",
      |  "pthid" : "MsgID-x",
      |  "body" : {
      |    "subject_type" : "Email",
      |    "subject" : "test@fmgp.app"
      |  }
      |}""".stripMargin

  test("Parse a Chatriqube Discovery Awswer example") {
    val fMsg = msgAnswerExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toAnswer

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id.value, "MsgID-2")
        assertEquals(msg.piuri.value, "https://decentriqube.com/discovery/1/answer")
        assertEquals(msg.to, TO("did:example:alice"))
        assertEquals(msg.from, FROM("did:example:bob"))
        assertEquals(msg.thid.value, "MsgID-1")
        assertEquals(msg.pthid, Some(MsgID("MsgID-x")))
        assertEquals(msg.subjectType, SubjectType.Email)
        assertEquals(msg.subject, "test@fmgp.app")
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgHandshakeExample =
    """{
      |  "id" : "MsgID-3",
      |  "type" : "https://decentriqube.com/discovery/1/handshake",
      |  "to" : ["did:example:bob"],
      |  "from" : "did:example:alice",
      |  "thid" : "MsgID-1",
      |  "pthid" : "MsgID-x",
      |  "body" : {}
      |}""".stripMargin

  test("Parse a Chatriqube Discovery Handshake example") {
    val fMsg = msgHandshakeExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toHandshake

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id.value, "MsgID-3")
        assertEquals(msg.piuri.value, "https://decentriqube.com/discovery/1/handshake")
        assertEquals(msg.to, TO("did:example:bob"))
        assertEquals(msg.from, FROM("did:example:alice"))
        assertEquals(msg.thid.value, "MsgID-1")
      // assertEquals(msg.pthid, Some(MsgID("MsgID-x")))
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }
}
