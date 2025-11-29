package fmgp.did.comm

import fmgp.did.DIDDocument
import fmgp.crypto.*

// import fmgp.crypto.RawOperations._
import munit.*
import zio.json.*
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import concurrent.ExecutionContext.Implicits.global

class SignedMessageSuite_Parse extends FunSuite {

  // ### parse ###

  test("parse SignedMessage") {
    val str = SignedMessageExamples.exampleSignatureEdDSA_json.fromJson[SignedMessage] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, SignedMessageExamples.exampleSignatureEdDSA_obj)
    }
  }
}
