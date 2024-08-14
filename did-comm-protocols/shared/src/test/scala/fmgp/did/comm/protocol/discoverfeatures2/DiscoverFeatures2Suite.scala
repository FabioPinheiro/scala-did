package fmgp.did.comm.protocol.discoverfeatures2

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._

/** didCommProtocolsJVM/testOnly fmgp.did.comm.protocol.discoverfeatures2.DiscoverFeatures2Suite */
class DiscoverFeatures2Suite extends FunSuite {
  val msgQueriesExample =
    """{
      |  "id" : "MsgID-1",
      |  "type" : "https://didcomm.org/discover-features/2.0/queries",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "body" : {
      |    "queries" : [
      |      { "feature-type": "protocol", "match": "https://didcomm.org/tictactoe/1.*" },
      |      { "feature-type": "goal-code", "match": "org.didcomm.*" }
      |    ]
      |  }
      |}""".stripMargin

  test("Parse a discover-features Queries example") {
    val fMsg = msgQueriesExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toFeatureQuery

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id.value, "MsgID-1")
        msg.queries match
          case Seq(query1, query2) =>
            assertEquals(query1, FeatureQuery.Query("protocol", "https://didcomm.org/tictactoe/1.*"))
            assertEquals(query2, FeatureQuery.Query("goal-code", "org.didcomm.*"))
          case _ => fail("body.updates must have two element")

      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgDiscloseExample =
    """{
      |  "id" : "MsgID-2",
      |  "thid" : "MsgID-1",
      |  "type" : "https://didcomm.org/discover-features/2.0/disclose",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "body" : {
      |    "disclosures" : [
      |      { "feature-type": "protocol", "id": "https://didcomm.org/tictactoe/1.0", "roles": ["player"] },
      |      { "feature-type": "goal-code", "id": "org.didcomm.sell.goods.consumer" }
      |    ]
      |  }
      |}""".stripMargin

  test("Parse a discover-features Disclose example") {
    val fMsg = msgDiscloseExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toFeatureDisclose

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.id.value, "MsgID-2")
        assertEquals(msg.thid.map(_.value), Some("MsgID-1"))
        msg.disclosures match
          case Seq(disclose1, disclose2) =>
            assertEquals(
              disclose1,
              FeatureDisclose.Disclose("protocol", "https://didcomm.org/tictactoe/1.0", Some(Seq("player")))
            )
            assertEquals(disclose2, FeatureDisclose.Disclose("goal-code", "org.didcomm.sell.goods.consumer", None))
          case _ => fail("body.updates must have two element")

      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }
}
