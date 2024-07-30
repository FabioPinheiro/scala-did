package fmgp.did.comm

import munit._
import zio.json._
import fmgp.did.DIDDocument

/** didJS/testOnly fmgp.did.comm.DIDCommSuite */
class DIDCommSuite extends FunSuite {

  test("Example parse plaintextMessage") {
    val ret = DIDCommExamples.plaintextMessage.fromJson[PlaintextMessageClass]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.id, MsgID("1234567890"))
        assertEquals(obj, DIDCommExamples.plaintextMessageObj)
    }
  }

  test("Example serialize plaintextMessage and parse back") {
    val original = DIDCommExamples.plaintextMessageObj
    val ret = original.toJson.fromJson[PlaintextMessageClass]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj, original)
    }
  }

  test("Example parse Sender DIDDocument") {
    val ret = DIDCommExamples.senderDIDDocument.fromJson[DIDDocument]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.id.did, "did:example:alice")
        assert(obj.authentication.isDefined)
        assertEquals(obj.allKeysTypeKeyAgreement.size, 3)
        assert(obj.keyAgreement.isDefined)
        assertEquals(obj.keyAgreement.get.size, 3)
    }
  }

  test("Example parse Recipient DIDDocument") {
    val ret = DIDCommExamples.recipientDIDDocument.fromJson[DIDDocument]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.id.did, "did:example:bob")
        assert(obj.keyAgreement.isDefined)
        assertEquals(obj.keyAgreement.get.size, 9)
    }
  }

  test("Example parse Sender secrets") {
    val ret = DIDCommExamples.senderSecrets.fromJson[Seq[Map[String, String]]]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.size, 6)
    }
  }

  test("Example parse Recipient Secrets") {
    val ret = DIDCommExamples.recipientSecrets.fromJson[Seq[Map[String, String]]]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.size, 9)
    }
  }

}
