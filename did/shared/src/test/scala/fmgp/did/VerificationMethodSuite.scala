package fmgp.did

import munit._
import zio.json._
import zio.json.JsonCodec.option
import zio.json.ast.{Json, JsonCursor}

class VerificationMethodSuite extends FunSuite {

  test("VerificationMethodReferenced serialize parse") {
    val aux = VerificationMethodReferenced("1234")
    val expected = "\"1234\""
    assertEquals(expected, aux.toJson)

    val ret = expected.fromJson[VerificationMethodReferenced]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, aux)
    }
  }

  test("VerificationMethod serialize parse REF") {
    val aux: VerificationMethod = VerificationMethodReferenced("1234")
    val expected = "\"1234\""
    assertEquals(expected, aux.toJson)

    val ret = expected.fromJson[VerificationMethod]
    ret match {
      case Left(error)                              => fail(error)
      case Right(obj: VerificationMethodReferenced) => assertEquals(obj, aux)
      case Right(obj)                               => fail("Expeting a VerificationMethodReferenced")
    }
  }

  test("Example 13 (verificationMethod_0) parse") {
    val json = DIDExamples.EX13.fromJson[ast.Json]
    val cursor0 = JsonCursor.field("verificationMethod").isArray.element(0)
    // println(json.flatMap(_.get(cursor0)))
    val ret = json.flatMap(_.get(cursor0)).flatMap(_.toJson.fromJson[VerificationMethod])
    ret match {
      case Left(error)                               => fail(error)
      case Right(obj: VerificationMethodEmbeddedJWK) => assertEquals(obj, DIDExamples.EX13_VerificationMethod_0)
      case Right(obj)                                => fail("Expeting a VerificationMethodEmbeddedJWK")
    }
  }

  test("Example 13 (verificationMethod_1) parse") {
    val json = DIDExamples.EX13.fromJson[ast.Json]
    val cursor1 = JsonCursor.field("verificationMethod").isArray.element(1)
    val ret = json.flatMap(_.get(cursor1)).flatMap(_.toJson.fromJson[VerificationMethod])
    ret match {
      case Left(error)                                     => fail(error)
      case Right(obj: VerificationMethodEmbeddedMultibase) => assertEquals(obj, DIDExamples.EX13_VerificationMethod_1)
      case Right(obj)                                      => fail("Expeting a VerificationMethodEmbeddedMultibase")
    }
  }
}
