package fmgp.did.comm

import munit._

import fmgp.did._
import fmgp.crypto._

import zio._
import zio.json._
import zio.json.ast.Json

/** didJVM/testOnly fmgp.did.comm.EncryptedMessageSuite_Parse */
class EncryptedMessageSuite_Parse extends ZSuite {

  test("Parse and check hashCode encryptedMessage_ECDHES_X25519_XC20P") {
    val ret = EncryptedMessageExamples.encryptedMessage_ECDHES_X25519_XC20P.fromJson[EncryptedMessage]
    ret match {
      case Left(error) => fail(error)
      case Right(obj) =>
        assertEquals(obj, EncryptedMessageExamples.obj_encryptedMessage_ECDHES_X25519_XC20P)
        assertEquals(obj.hashCode, EncryptedMessageExamples.obj_encryptedMessage_ECDHES_X25519_XC20P.hashCode)
    }
  }

  test("Example parse encryptedMessage_ECDH1PU_X25519_A256CBCHS512") {
    val ret = EncryptedMessageExamples.encryptedMessage_ECDH1PU_X25519_A256CBCHS512.fromJson[EncryptedMessage]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => // ok
    }
  }

  EncryptedMessageExamples.allEncryptedMessage.zipWithIndex.foreach((example, index) =>
    test(s"Example parse Encrypted Messages (index $index)") {
      val ret = example.fromJson[EncryptedMessage]
      ret match {
        case Left(error) => fail(error)
        case Right(obj)  => assert(!obj.recipients.isEmpty)
      }
    }
  )

  test(s"Compere the hashCode of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      val ret1 = example.fromJson[EncryptedMessage]
      val ret2 = example.fromJson[EncryptedMessage]
      assertEquals(ret1.hashCode(), ret2.hashCode())
    }
  }

  // ###############
  // ### decrypt ###
  // ###############

  val expeted = PlaintextMessageClass(
    id = MsgID("1234567890"),
    `type` = PIURI("http://example.com/protocols/lets_do_lunch/1.0/proposal"),
    to = Some(Set(TO("did:example:bob"))),
    from = Some(FROM("did:example:alice")),
    thid = None,
    pthid = None,
    ack = None,
    created_time = Some(1516269022),
    expires_time = Some(1516385931),
    body = Some(Json.Obj("messagespecificattribute" -> Json.Str("and its value"))),
    attachments = None,
    from_prior = None,
    return_route = None,
    `accept-lang` = None,
    lang = None,
    l10n = None,
    sender_order = None,
    sent_count = None,
    received_orders = None,
  )

}
