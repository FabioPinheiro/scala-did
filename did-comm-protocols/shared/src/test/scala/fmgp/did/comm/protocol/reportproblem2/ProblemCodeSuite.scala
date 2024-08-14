package fmgp.did.comm.protocol.reportproblem2

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._

/** didCommProtocolsJVM/testOnly fmgp.did.comm.protocol.reportproblem2.ProblemCodeSuite */
class ProblemCodeSuite extends FunSuite {

  // https://regex101.com/r/B0v1Jo/1
  val ex1 = "\"e.p.xfer.cant-use-endpoint\""
  val ex1_obj = ProblemCode.ErroFail("xfer", "cant-use-endpoint")
  val ex2 = "\"w.p.req.max-errors-exceeded\""
  val ex2_obj = ProblemCode.WarnFail("req", "max-errors-exceeded")
  val ex3 = "\"e.p.a.b.c.d.e.f\""
  val ex3_obj = ProblemCode.ErroFail("a", "b", "c", "d", "e", "f")
  val ex4 = "\"e.get-pay-details.payment-failed\""
  val ex4_obj = ProblemCode.ErroUndoToStep("get-pay-details", "payment-failed")
  val ex5 = "\"e.m.x\""
  val ex5_obj = ProblemCode.ErroUndo("x")
  val ex6 = "\"e.v.x\""
  val ex6_obj = ProblemCode.ErroUndoToStep("v", "x")
  val ex7 = "\"w.v.x\""
  val ex7_obj = ProblemCode.WarnUndoToStep("v", "x")

  val exFail1 = "\"e.x.with space\""
  val exFail2 = "\"x.p.x\""
  val exFail3 = "\"e.p.\""
  val exFail4 = "\"e.p\""

  val examples = Seq(
    (ex1, ex1_obj),
    (ex2, ex2_obj),
    (ex3, ex3_obj),
    (ex4, ex4_obj),
    (ex5, ex5_obj),
    (ex6, ex6_obj),
    (ex7, ex7_obj),
  )
  val examplesFail = Seq(
    exFail1,
    exFail2,
    exFail3,
    exFail4,
  )

  examples.foreach { (ex, expeted) =>
    test(s"Parse a problem-report code $ex") {
      ex.fromJson[ProblemCode] match {
        case Left(error) => fail(error)
        case Right(obj)  => assertEquals(obj, expeted)
      }
    }
  }
  // fails :
  examplesFail.foreach { (exfail) =>
    test(s"Unable to parse a problem-report code $exfail") {
      exfail.fromJson[ProblemCode] match {
        case Left(error) =>
          println(error)
          assert(error.contains("Not valid ProblemReport"))
        case Right(obj) => fail("Is not a valid code and MUST fail")
      }
    }
  }

}
