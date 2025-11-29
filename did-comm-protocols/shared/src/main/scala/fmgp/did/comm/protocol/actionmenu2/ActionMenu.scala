package fmgp.did.comm.protocol.actionmenu2

import zio.json.*

import fmgp.did.*
import fmgp.did.comm.*

// https://didcomm.org/action-menu/2.0/
extension (msg: PlaintextMessage)
  def toMenu: Either[String, Menu] =
    Menu.fromPlaintextMessage(msg)
  // def toMenuRequest: Either[String, MenuRequest] =
  //   MenuRequest.fromPlaintextMessage(msg)
  def toPerform: Either[String, Perform] =
    Perform.fromPlaintextMessage(msg)

sealed trait MenuOptionFrom {
  def name: String
  def title: String
}
object MenuOptionFrom {
  given decoder: JsonDecoder[MenuOptionFrom] = ast.Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst.get(ast.JsonCursor.field("form")) match {
      case Left(value) /* "No such field: 'form' */ => MenuOption.decoder.decodeJson(originalAst.toJson)
      case Right(value)                             => MenuForm.decoder.decodeJson(originalAst.toJson)
    }
  } // MenuOption.decoder.orElse(MenuForm.decoder.widen)
  given encoder: JsonEncoder[MenuOptionFrom] = new JsonEncoder[MenuOptionFrom] {
    override def unsafeEncode(b: MenuOptionFrom, indent: Option[Int], out: zio.json.internal.Write): Unit =
      b match {
        case obj: MenuOption => MenuOption.encoder.unsafeEncode(obj, indent, out)
        case obj: MenuForm   => MenuForm.encoder.unsafeEncode(obj, indent, out)
      }
  }
}

final case class MenuOption(
    override val name: String,
    override val title: String,
    description: String,
    disabled: Option[Boolean] = None // Some(false)
) extends MenuOptionFrom
object MenuOption {
  given decoder: JsonDecoder[MenuOption] = DeriveJsonDecoder.gen[MenuOption]
  given encoder: JsonEncoder[MenuOption] = DeriveJsonEncoder.gen[MenuOption]
}

final case class MenuForm(
    override val name: String,
    override val title: String,
    form: Form,
    // disabled: Option[Boolean] = None // Some(false)
) extends MenuOptionFrom
object MenuForm {
  given decoder: JsonDecoder[MenuForm] = DeriveJsonDecoder.gen[MenuForm]
  given encoder: JsonEncoder[MenuForm] = DeriveJsonEncoder.gen[MenuForm]
}

final case class Form(
    description: String,
    params: Seq[FromParam],
    `submit-label`: String,
)
object Form {
  given decoder: JsonDecoder[Form] = DeriveJsonDecoder.gen[Form]
  given encoder: JsonEncoder[Form] = DeriveJsonEncoder.gen[Form]
}

final case class FromParam(
    name: String,
    title: String,
    default: String, // type? this is the deault value
    description: Option[String],
    required: Option[Boolean] = Some(true),
    `type`: Option[String] = Some("text"),
)
object FromParam {
  given decoder: JsonDecoder[FromParam] = DeriveJsonDecoder.gen[FromParam]
  given encoder: JsonEncoder[FromParam] = DeriveJsonEncoder.gen[FromParam]
}

/** A requester is expected to display only one active menu per connection when action menus are employed by the
  * responder. A newly received menu is not expected to interrupt a user, but rather be made available for the user to
  * inspect possible actions related to the responder.
  *
  * {{{
  * {
  *   "type": "https://didcomm.org/action-menu/2.0/menu",
  *   "id": "5678876542344",
  *   "to" : [ "did:example:bob" ],
  *   "from" : "did:example:alice",
  *   "body": {
  *     "title": "Welcome to IIWBook",
  *     "description": "IIWBook facilitates connections between attendees by verifying attendance and distributing connection invitations.",
  *     "errormsg": "No IIWBook names were found.",
  *     "options": [
  *       {
  *         "name": "obtain-email-cred",
  *         "title": "Obtain a verified email credential",
  *         "description": "Connect with the BC email verification service to obtain a verified email credential"
  *       },
  *       {
  *         "name": "verify-email-cred",
  *         "title": "Verify your participation",
  *         "description": "Present a verified email credential to identify yourself"
  *       },
  *       {
  *         "name": "search-introductions",
  *         "title": "Search introductions",
  *         "description": "Your email address must be verified to perform a search",
  *         "disabled": true
  *       }
  *     ]
  *   }
  * }
  * }}}
  *
  * Quick forms:
  * {{{
  * {
  *   "type": "https://didcomm.org/action-menu/2.0/menu",
  *   "id": "5678876542347",
  *   "thid": "5678876542344",
  *   "to" : [ "did:example:bob" ],
  *   "from" : "did:example:alice",
  *   "title": "Attendance Verified",
  *   "body": {
  *     "description": "",
  *     "options": [
  *         {
  *           "name": "submit-invitation",
  *           "title": "Submit an invitation",
  *           "description": "Send an invitation for IIWBook to share with another participant"
  *         },
  *         {
  *           "name": "search-introductions",
  *           "title": "Search introductions",
  *           "form": {
  *             "description": "Enter a participant name below to perform a search.",
  *             "params": [
  *               {
  *                 "name": "query",
  *                 "title": "Participant name",
  *                 "default": "",
  *                 "description": "",
  *                 "required": true,
  *                 "type": "text"
  *               }
  *             ],
  *             "submit-label": "Search"
  *           }
  *         }
  *     ]
  *   }
  * }
  * }}}
  */
final case class Menu(
    id: MsgID = MsgID(),
    thid: NotRequired[MsgID] = None,
    to: Set[TO],
    from: Option[FROM],
    // lang: NotRequired[String] = None,
    title: String,
    description: String,
    errormsg: Option[String],
    options: Seq[MenuOptionFrom],
) {
  def `type` = Menu.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      thid = thid,
      to = Some(to),
      from = from,
      body = Some(
        Menu.Body(title = title, description = description, errormsg = errormsg, options = options).toJSON_RFC7159
      ),
    )

}

object Menu {
  def piuri = PIURI("https://didcomm.org/action-menu/2.0/menu")

  protected final case class Body(
      title: String,
      description: String,
      errormsg: Option[String],
      options: Seq[MenuOptionFrom],
  ) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Menu] = {
    if (msg.`type` != piuri)
      Left(s"No able to create Menu from a Message of the type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case tos   =>
          msg.body match
            case None    => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].map { body =>
                Menu(
                  id = msg.id,
                  thid = msg.thid,
                  to = tos.toSet,
                  from = msg.from,
                  title = body.title,
                  description = body.description,
                  errormsg = body.errormsg,
                  options = body.options,
                )
              }

  }
}

//This is unclear from the specs https://didcomm.org/action-menu/2.0/menu-request
// final case class MenuRequest(
//     id: MsgID = MsgID(),
//     to: Set[TO],
//     from: Option[FROM],
//     lang: NotRequired[String] = None,
// )

/** {{{
  * {
  *   "type": "https://didcomm.org/action-menu/2.0/perform",
  *   "id": "5678876542346",
  *   "thid": "5678876542344",
  *   "to" : [ "did:example:alice" ],
  *   "from" : "did:example:bob",
  *   "body":{
  *     "name": "obtain-email-cred",
  *     "params": {}
  *   }
  * }
  * }}}
  */
final case class Perform(
    id: MsgID = MsgID(),
    thid: MsgID,
    to: Set[TO],
    from: Option[FROM],
    lang: NotRequired[String] = None,
    name: String,
    params: Option[Map[String, String]],
) {
  def `type` = Perform.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      thid = Some(thid),
      to = Some(to),
      from = from,
      body = Some(
        Perform
          .Body(
            name = name,
            params = params,
          )
          .toJSON_RFC7159
      )
    )

}

object Perform {
  def piuri = PIURI("https://didcomm.org/action-menu/2.0/perform")

  protected final case class Body(name: String, params: Option[Map[String, String]]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Perform] = {
    if (msg.`type` != piuri)
      Left(s"No able to create Perform from a Message of the type '${msg.`type`}'")
    else
      msg.thid match
        case None       => Left(s"'$piuri' MUST have field 'thid'")
        case Some(thid) =>
          msg.to.toSeq.flatten match // Note: toSeq is from the match
            case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
            case tos   =>
              msg.body match
                case None    => Left(s"'$piuri' MUST have field 'body'")
                case Some(b) =>
                  b.as[Body].map { body =>
                    Perform(
                      id = msg.id,
                      thid = thid,
                      to = tos.toSet,
                      from = msg.from,
                      name = body.name,
                      params = body.params,
                    )
                  }

  }
}
