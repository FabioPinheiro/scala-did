package fmgp.did

import munit._
import zio._
import zio.json._
import zio.json.ast.{Json, JsonCursor}
import fmgp.did.DIDDocument

/** didJVM/testOnly fmgp.did.DIDServiceSuite */
class DIDServiceSuite extends FunSuite {

  test("Example 20 parse as DIDServiceGeneric") {
    val json = DIDExamples.EX20.fromJson[ast.Json]
    val cursor = JsonCursor.field("service").isArray.element(0)
    val jsonOneService = json.flatMap(_.get(cursor))
    jsonOneService match
      case Left(error) => fail(error)
      case Right(tmp)  =>
        val json2 = tmp.toJson
        json2.fromJson[DIDServiceGeneric] match {
          case Left(error) => fail(error)
          case Right(obj)  =>
            assertEquals(obj, DIDExamples.EX20_DIDServiceGeneric)
            assertEquals(obj.toJson, json2)
        }
  }

  test("Example 20 parse as DIDService") {
    val json = DIDExamples.EX20.fromJson[ast.Json]
    val cursor = JsonCursor.field("service").isArray.element(0)
    val jsonOneService = json.flatMap(_.get(cursor))
    jsonOneService match
      case Left(error) => fail(error)
      case Right(tmp)  =>
        val json2 = tmp.toJson
        json2.fromJson[DIDService] match {
          case Left(error) => fail(error)
          case Right(obj)  =>
            assertEquals(obj, DIDExamples.EX20_DIDService)
            assertEquals(obj.toJson, json2)
        }
  }

  test("DIDCommMessaging with URI on the serviceEndpoint") {
    val service =
      """{"id": "did:example:123#didcomm-1","type": "DIDCommMessaging",
        |"serviceEndpoint": {"uri":"https://fmgp.app/"}
        |}""".stripMargin
    val ret = service.fromJson[DIDService]
    ret match {
      case Left(error)                            => fail(error)
      case Right(obj: DIDServiceDIDCommMessaging) =>
        assertEquals(obj.id, "did:example:123#didcomm-1")
        assertEquals(
          obj.serviceEndpoint.as[Json],
          DIDCommMessagingServiceEndpoint(uri = "https://fmgp.app/").toJsonAST
        )
      case Right(obj) => fail("Not a DIDServiceDIDCommMessaging")
    }
  }

  test("DIDCommMessaging with an array of serviceEndpoint URI") {
    val service =
      """{"id": "did:example:123#didcomm-1","type": "DIDCommMessaging",
        |"serviceEndpoint": [{"uri":"https://fmgp.app/"}, {"uri":"https://did.fmgp.app/test"}]
        |}""".stripMargin
    val ret = service.fromJson[DIDService]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  =>
        assertEquals(
          obj,
          DIDServiceDIDCommMessaging(
            id = "did:example:123#didcomm-1",
            DIDCommMessagingServiceEndpoint(uri = "https://fmgp.app/"),
            DIDCommMessagingServiceEndpoint(uri = "https://did.fmgp.app/test")
          )
        )
        assertEquals(
          obj.toJson,
          DIDServiceGeneric(
            id = "did:example:123#didcomm-1",
            `type` = "DIDCommMessaging",
            serviceEndpoint = Json.Arr(
              Json.Obj(("uri", Json.Str("https://fmgp.app/"))),
              Json.Obj(("uri", Json.Str("https://did.fmgp.app/test")))
            )
          ).toJson
        )

    }
  }

  // For DIDCommV2.1
  test("DIDCommV2.1 (new)- DIDCommMessaging with a single serviceEndpoint") {
    val service =
      """{
        |  "id": "did:example:123456789abcdefghi#didcomm-1",
        |  "type": "DIDCommMessaging",
        |  "serviceEndpoint": {
        |    "uri": "https://example.com/path",
        |    "accept": ["didcomm/v2","didcomm/aip2;env=rfc587"],
        |    "routingKeys": ["did:example:somemediator#somekey"]
        |  }
        |}""".stripMargin
    val ret = service.fromJson[DIDService]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  =>
        assertEquals(
          obj,
          DIDServiceDIDCommMessaging(
            id = "did:example:123456789abcdefghi#didcomm-1",
            DIDCommMessagingServiceEndpoint(
              uri = "https://example.com/path",
              accept = Some(Set("didcomm/v2", "didcomm/aip2;env=rfc587")),
              routingKeys = Some(Set("did:example:somemediator#somekey"))
            ),
          )
        )
    }
  }

  test("DIDCommMessaging with an array of serviceEndpoint objects") {
    val service =
      """{
        |  "id": "did:example:123456789abcdefghi#didcomm-1",
        |  "type": "DIDCommMessaging",
        |  "serviceEndpoint": [{
        |    "uri": "https://example.com/path",
        |    "accept": ["didcomm/v2","didcomm/aip2;env=rfc587"],
        |    "routingKeys": ["did:example:somemediator#somekey"]
        |  }]
        |}""".stripMargin
    val ret = service.fromJson[DIDService]
    ret match {
      case Left(error) => fail(error)
      case Right(obj)  =>
        val serviceExpected = DIDServiceGeneric(
          id = "did:example:123456789abcdefghi#didcomm-1",
          `type` = "DIDCommMessaging",
          serviceEndpoint = // Json.Arr(
            Json.Obj(
              Chunk(
                ("uri", Json.Str("https://example.com/path")),
                ("accept", Json.Arr(Json.Str("didcomm/v2"), Json.Str("didcomm/aip2;env=rfc587"))),
                ("routingKeys", Json.Arr(Json.Str("did:example:somemediator#somekey")))
              )
            )
          // )
        )
        assertEquals(obj.toJson, serviceExpected.toJson)
    }
  }
}
