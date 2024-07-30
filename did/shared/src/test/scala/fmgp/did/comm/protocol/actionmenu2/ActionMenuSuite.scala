package fmgp.did.comm.protocol.actionmenu2

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._

/** didJVM/testOnly fmgp.did.comm.protocol.actionmenu2.ActionMenuSuite */
class ActionMenuSuite extends FunSuite {
  val msgMenuExample =
    """{
      |  "type": "https://didcomm.org/action-menu/2.0/menu",
      |  "id": "5678876542344",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "body": {
      |    "title": "Welcome to IIWBook",
      |    "description": "IIWBook facilitates connections between attendees by verifying attendance and distributing connection invitations.",
      |    "errormsg": "No IIWBook names were found.",
      |    "options": [
      |      {
      |        "name": "obtain-email-cred",
      |        "title": "Obtain a verified email credential",
      |        "description": "Connect with the BC email verification service to obtain a verified email credential"
      |      },
      |      {
      |        "name": "verify-email-cred",
      |        "title": "Verify your participation",
      |        "description": "Present a verified email credential to identify yourself"
      |      },
      |      {
      |        "name": "search-introductions",
      |        "title": "Search introductions",
      |        "description": "Your email address must be verified to perform a search",
      |        "disabled": true
      |      }
      |    ]
      |  }
      |}""".stripMargin

  test("Parse a action-menu menu example 1") {
    val fMsg = msgMenuExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toMenu

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.`type`.value, "https://didcomm.org/action-menu/2.0/menu")
        assertEquals(msg.title, "Welcome to IIWBook")
        assertEquals(
          msg.description,
          "IIWBook facilitates connections between attendees by verifying attendance and distributing connection invitations."
        )
        assertEquals(msg.errormsg, Some("No IIWBook names were found."))
        msg.options match
          case Seq(e1: MenuOption, e2: MenuOption, e3: MenuOption) =>
            assertEquals(e1.name, "obtain-email-cred")
            assertEquals(e1.title, "Obtain a verified email credential")
            assertEquals(
              e1.description,
              "Connect with the BC email verification service to obtain a verified email credential"
            )
            assertEquals(e3.disabled, Some(true))
          case _ =>
            fail(s"The list of options MUST have size 3")

        assertEquals(
          msg.toPlaintextMessage.toJsonPretty,
          """{
            |  "id" : "5678876542344",
            |  "type" : "https://didcomm.org/action-menu/2.0/menu",
            |  "to" : [
            |    "did:example:bob"
            |  ],
            |  "from" : "did:example:alice",
            |  "body" : {
            |    "title" : "Welcome to IIWBook",
            |    "description" : "IIWBook facilitates connections between attendees by verifying attendance and distributing connection invitations.",
            |    "errormsg" : "No IIWBook names were found.",
            |    "options" : [
            |      {
            |        "name" : "obtain-email-cred",
            |        "title" : "Obtain a verified email credential",
            |        "description" : "Connect with the BC email verification service to obtain a verified email credential"
            |      },
            |      {
            |        "name" : "verify-email-cred",
            |        "title" : "Verify your participation",
            |        "description" : "Present a verified email credential to identify yourself"
            |      },
            |      {
            |        "name" : "search-introductions",
            |        "title" : "Search introductions",
            |        "description" : "Your email address must be verified to perform a search",
            |        "disabled" : true
            |      }
            |    ]
            |  }
            |}""".stripMargin
        )
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgMenuQuickFormsExample =
    """{
      |  "type": "https://didcomm.org/action-menu/2.0/menu",
      |  "id": "5678876542347",
      |  "thid": "5678876542344",
      |  "to" : [ "did:example:bob" ],
      |  "from" : "did:example:alice",
      |  "body": {
      |    "title": "Attendance Verified",
      |    "description": "",
      |    "options": [
      |        {
      |          "name": "submit-invitation",
      |          "title": "Submit an invitation",
      |          "description": "Send an invitation for IIWBook to share with another participant"
      |        },
      |        {
      |          "name": "search-introductions",
      |          "title": "Search introductions",
      |          "form": {
      |            "description": "Enter a participant name below to perform a search.",
      |            "params": [
      |              {
      |                "name": "query",
      |                "title": "Participant name",
      |                "default": "",
      |                "description": "",
      |                "required": true,
      |                "type": "text"
      |              }
      |            ],
      |            "submit-label": "Search"
      |          }
      |        }
      |    ]
      |  }
      |}""".stripMargin

  test("Parse a action-menu menu example 2 (QuickForms)") {
    val fMsg = msgMenuQuickFormsExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toMenu

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.`type`.value, "https://didcomm.org/action-menu/2.0/menu")
        assertEquals(msg.title, "Attendance Verified")
        assertEquals(msg.description, "")
        assertEquals(msg.errormsg, None)
        msg.options match
          case Seq(e1: MenuOption, e2: MenuForm) =>
            assertEquals(e1.name, "submit-invitation")
            assertEquals(e1.title, "Submit an invitation")
            assertEquals(e1.description, "Send an invitation for IIWBook to share with another participant")
            assertEquals(e2.name, "search-introductions")
            assertEquals(e2.title, "Search introductions")
            assertEquals(e2.form.description, "Enter a participant name below to perform a search.")
            assertEquals(e2.form.`submit-label`, "Search")
            assertEquals(e2.form.params.head.name, "query")
            assertEquals(e2.form.params.head.title, "Participant name")
            assertEquals(e2.form.params.head.default, "")
            assertEquals(e2.form.params.head.description, Some(""))
            assertEquals(e2.form.params.head.required, Some(true))
            assertEquals(e2.form.params.head.`type`, Some("text"))
          case _ =>
            fail(s"The list of options MUST have size 3")
        assertEquals(
          msg.toPlaintextMessage.toJsonPretty,
          """{
            |  "id" : "5678876542347",
            |  "type" : "https://didcomm.org/action-menu/2.0/menu",
            |  "to" : [
            |    "did:example:bob"
            |  ],
            |  "from" : "did:example:alice",
            |  "thid" : "5678876542344",
            |  "body" : {
            |    "title" : "Attendance Verified",
            |    "description" : "",
            |    "options" : [
            |      {
            |        "name" : "submit-invitation",
            |        "title" : "Submit an invitation",
            |        "description" : "Send an invitation for IIWBook to share with another participant"
            |      },
            |      {
            |        "name" : "search-introductions",
            |        "title" : "Search introductions",
            |        "form" : {
            |          "description" : "Enter a participant name below to perform a search.",
            |          "params" : [
            |            {
            |              "name" : "query",
            |              "title" : "Participant name",
            |              "default" : "",
            |              "description" : "",
            |              "required" : true,
            |              "type" : "text"
            |            }
            |          ],
            |          "submit-label" : "Search"
            |        }
            |      }
            |    ]
            |  }
            |}""".stripMargin
        )
      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  val msgPerformExample =
    """{
      |  "type": "https://didcomm.org/action-menu/2.0/perform",
      |  "id": "5678876542346",
      |  "thid": "5678876542344",
      |  "to" : [ "did:example:alice" ],
      |  "from" : "did:example:bob",
      |  "body":{
      |    "name": "obtain-email-cred",
      |    "params": {}
      |  }
      |}""".stripMargin

  test("Parse a action-menu perform example") {
    val fMsg = msgPerformExample
      .fromJson[PlaintextMessage]
      .getOrElse(fail("FAIL to parse PlaintextMessage"))
      .toPerform

    (fMsg) match {
      case Right(msg) =>
        assertEquals(msg.`type`.value, "https://didcomm.org/action-menu/2.0/perform")
        assertEquals(msg.id.value, "5678876542346")
        assertEquals(msg.thid.value, "5678876542344")
        assertEquals(msg.name, "obtain-email-cred")
        assertEquals(msg.params, Some(Map()))
        assertEquals(
          msg.toPlaintextMessage.toJsonPretty,
          """{
            |  "id" : "5678876542346",
            |  "type" : "https://didcomm.org/action-menu/2.0/perform",
            |  "to" : [
            |    "did:example:alice"
            |  ],
            |  "from" : "did:example:bob",
            |  "thid" : "5678876542344",
            |  "body" : {
            |    "name" : "obtain-email-cred",
            |    "params" : {}
            |  }
            |}""".stripMargin
        )

      case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
    }
  }

  // test("Parse a discover-features Disclose example") {
  //   val fMsg = msgDiscloseExample
  //     .fromJson[PlaintextMessage]
  //     .getOrElse(fail("FAIL to parse PlaintextMessage"))
  //     .toFeatureDisclose

  //   (fMsg) match {
  //     case Right(msg) =>
  //       assertEquals(msg.id.value, "MsgID-2")
  //       assertEquals(msg.thid.map(_.value), Some("MsgID-1"))
  //       msg.disclosures match
  //         case Seq(disclose1, disclose2) =>
  //           assertEquals(
  //             disclose1,
  //             FeatureDisclose.Disclose("protocol", "https://didcomm.org/tictactoe/1.0", Some(Seq("player")))
  //           )
  //           assertEquals(disclose2, FeatureDisclose.Disclose("goal-code", "org.didcomm.sell.goods.consumer", None))
  //         case _ => fail("body.updates must have two element")

  //     case Left(error) => fail(s"fMsg MUST be Right but is ${Left(error)}")
  //   }
  // }
}
