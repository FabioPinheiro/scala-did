package fmgp.did.comm.protocol.reportproblem2

import munit.*
import zio.json.*
import zio.json.ast.Json

import fmgp.did.comm.*

/** didCommProtocolsJVM/testOnly fmgp.did.comm.protocol.reportproblem2.ReportProblem2Suite */
class ReportProblem2Suite extends FunSuite {
  val msgQueriesExample =
    """{
      |  "type": "https://didcomm.org/report-problem/2.0/problem-report",
      |  "id": "7c9de639-c51c-4d60-ab95-103fa613c805",
      |  "pthid": "1e513ad4-48c9-444e-9e7e-5b8b45c5e325",
      |  "ack": ["1e513ad4-48c9-444e-9e7e-5b8b45c5e325"],
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "body": {
      |    "code": "e.p.xfer.cant-use-endpoint",
      |    "comment": "Unable to use the {1} endpoint for {2}.",
      |    "args": [
      |      "https://agents.r.us/inbox",
      |      "did:sov:C805sNYhMrjHiqZDTUASHg"
      |    ],
      |    "escalate_to": "mailto:admin@foo.org"
      |  }
      |}""".stripMargin

  test("Parse a problem-report example") {
    val fMsg = msgQueriesExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toProblemReport

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id.value, "7c9de639-c51c-4d60-ab95-103fa613c805")
        assertEquals(msg.code, ProblemCode.ErroFail("xfer", "cant-use-endpoint"))
        assertEquals(msg.comment, Some("Unable to use the {1} endpoint for {2}."))
        assertEquals(msg.args, Some(Seq("https://agents.r.us/inbox", "did:sov:C805sNYhMrjHiqZDTUASHg")))
        assertEquals(msg.escalate_to, Some("mailto:admin@foo.org"))
        assertEquals(
          msg.commentWithArgs,
          Some("Unable to use the https://agents.r.us/inbox endpoint for did:sov:C805sNYhMrjHiqZDTUASHg.")
        )

      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

}
