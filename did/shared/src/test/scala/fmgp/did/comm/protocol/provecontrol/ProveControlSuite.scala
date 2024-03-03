package fmgp.did.comm.protocol.provecontrol

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._
import fmgp.crypto.SHA256

/** didJVM/testOnly fmgp.did.comm.protocol.provecontrol.ProveControlSuite */
class ProveControlSuite extends FunSuite {
  val msgRequestVerificationExample =
    """{
      |  "id" : "MsgID-1",
      |  "type" : "https://fmgp.app/provecontrol/0.1/requestverification",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "body" : {
      |    "verificationType" : "Email",
      |    "subject" : "fabio@fmgp.app"
      |  }
      |}""".stripMargin

  test("Parse a RequestVerification example") {
    val fMsg = msgRequestVerificationExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toRequestVerification

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id, MsgID("MsgID-1"))
        assertEquals(msg.to, TO("did:example:bob"))
        assertEquals(msg.from, FROM("did:example:alice"))
        assertEquals(msg.verificationType, VerificationType.Email)
        assertEquals(msg.subject, "fabio@fmgp.app")
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgVerificationChallengeExample =
    """{
      |  "id" : "MsgID-1",
      |  "type" : "https://fmgp.app/provecontrol/0.1/verificationchallenge",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "body" : {
      |    "verificationType" : "Email",
      |    "subject" : "fabio@fmgp.app",
      |    "secret" : "secret123"
      |  }
      |}""".stripMargin

  test("Parse a VerificationChallenge example") {
    val fMsg = msgVerificationChallengeExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toVerificationChallenge

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id, MsgID("MsgID-1"))
        assertEquals(msg.to, TO("did:example:bob"))
        assertEquals(msg.from, FROM("did:example:alice"))
        assertEquals(msg.verificationType, VerificationType.Email)
        assertEquals(msg.subject, "fabio@fmgp.app")
        assertEquals(msg.secret, "secret123")
        assertEquals(msg.calculateProof, "a9edac6918a7e8e78a1a3479b3a4120806b60fb2d95012a9a27820aa1ea282d5")
        assertEquals(
          SHA256.digestToHex("did:example:alice|did:example:bob|Email|fabio@fmgp.app|secret123"),
          "a9edac6918a7e8e78a1a3479b3a4120806b60fb2d95012a9a27820aa1ea282d5"
        )
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgProveExample =
    """{
      |  "id" : "MsgID-1",
      |  "type" : "https://fmgp.app/provecontrol/0.1/prove",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "thid": "MsgID-2",
      |  "body" : {
      |    "verificationType" : "Email",
      |    "subject" : "fabio@fmgp.app",
      |    "proof" : "hash(id|thid|from|to|verificationType|subject|secret123)"
      |  }
      |}""".stripMargin

  test("Parse a Prove example") {
    val fMsg = msgProveExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toProve

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id, MsgID("MsgID-1"))
        assertEquals(msg.to, TO("did:example:bob"))
        assertEquals(msg.from, FROM("did:example:alice"))
        assertEquals(msg.thid, MsgID("MsgID-2"))
        assertEquals(msg.verificationType, VerificationType.Email)
        assertEquals(msg.subject, "fabio@fmgp.app")
        assertEquals(msg.proof, "hash(id|thid|from|to|verificationType|subject|secret123)")
        assertEquals(msg.proof, "hash(id|thid|from|to|verificationType|subject|secret123)")
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }

  }

  val msgConfirmVerificationExample =
    """{
      |  "id" : "MsgID-1",
      |  "type" : "https://fmgp.app/provecontrol/0.1/confirmverification",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "thid": "MsgID-2",
      |  "body" : {
      |    "verificationType" : "Email",
      |    "subject" : "fabio@fmgp.app",
      |    "challenge" : "challenge123"
      |  }
      |}""".stripMargin

  test("Parse a ConfirmVerification example") {
    val fMsg = msgConfirmVerificationExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toConfirmVerification

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id, MsgID("MsgID-1"))
        assertEquals(msg.to, TO("did:example:bob"))
        assertEquals(msg.from, FROM("did:example:alice"))
        assertEquals(msg.thid, MsgID("MsgID-2"))
        assertEquals(msg.verificationType, VerificationType.Email)
        assertEquals(msg.subject, "fabio@fmgp.app")
      // TODO msg.attachments
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }
}
