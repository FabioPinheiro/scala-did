package fmgp.did

import munit.*

import zio.*
import zio.json.*
import zio.prelude.{Hash, Equal}

import fmgp.did.*
import fmgp.did.comm.*
import fmgp.crypto.*

/** didFrameworkJVM/testOnly fmgp.did.HashSuite */
class HashSuite extends ZSuite with AssertionsHash {

  test(s"Compere the equals '==' of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      (example.fromJson[EncryptedMessage], example.fromJson[EncryptedMessage]) match
        case (Right(a), Right(b)) =>
          assertEquals(a, b)
        case _ => fail("Failed to parsing EncryptedMessage")
    }
  }

  test(s"Compere the hashCode of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      (example.fromJson[EncryptedMessage], example.fromJson[EncryptedMessage]) match
        case (Right(a), Right(b)) =>
          assertEquals(a.hashCode, b.hashCode)
        case _ => fail("Failed to parsing EncryptedMessage")
    }
  }

  test(s"Compere the Equal of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      (example.fromJson[EncryptedMessage], example.fromJson[EncryptedMessage]) match
        case (Right(a), Right(b)) =>
          assert(Hash[EncryptedMessage].equal(a, b))
          assert(Equal[EncryptedMessage].equal(a, b))
        case _ => fail("Failed to parsing EncryptedMessage")
    }
  }

  test(s"Compere the Hash of two Encrypted Messages") {
    EncryptedMessageExamples.allEncryptedMessage.foreach { example =>
      (example.fromJson[EncryptedMessage], example.fromJson[EncryptedMessage]) match
        case (Right(a), Right(b)) =>
          assertEquals(Hash[EncryptedMessage].hash(a), Hash[EncryptedMessage].hash(b))
        case _ => fail("Failed to parsing EncryptedMessage")
    }
  }

}
