package fmgp.did.comm

import munit._

import fmgp.did._
import fmgp.crypto._
// import fmgp.crypto.RawOperations._

import zio._
import zio.json._
import zio.json.ast.JsonCursor
import zio.json.ast.Json

class EncryptedMessageSuite_Parse extends ZSuite {

  test("Example parse plaintextMessage") {
    val ret = EncryptedMessageExamples.plaintextMessage.fromJson[PlaintextMessageClass]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj.`type`, "https://example.com/protocols/lets_do_lunch/1.0/proposal")
    }
  }

  test("Example parse encryptedMessage_ECDH1PU_X25519_A256CBCHS512") {
    val ret = EncryptedMessageExamples.encryptedMessage_ECDH1PU_X25519_A256CBCHS512.fromJson[EncryptedMessageGeneric]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => // ok
    }
  }

  EncryptedMessageExamples.allEncryptedMessage.zipWithIndex.foreach((example, index) =>
    test(s"Example parse Encrypted Messages (index $index)") {
      val ret = example.fromJson[EncryptedMessageGeneric]
      ret match {
        case Left(error) => fail(error)
        case Right(obj)  => assert(!obj.recipients.isEmpty)
      }
    }
  )

  // ###############
  // ### decrypt ###
  // ###############

  val expeted = PlaintextMessageClass(
    "1234567890",
    "http://example.com/protocols/lets_do_lunch/1.0/proposal",
    Some(Set("did:example:bob")),
    Some("did:example:alice"),
    None,
    Some(1516269022),
    Some(1516385931),
    Map("messagespecificattribute" -> "and its value")
  )


}