package fmgp.did.comm.protocol.mediatorcoordination3

import zio.json.*
import fmgp.did.*
import fmgp.did.comm.*

extension (msg: PlaintextMessage)
  def toRecipientUpdate: Either[String, RecipientUpdate] = RecipientUpdate.fromPlaintextMessage(msg)
  def toRecipientResponse: Either[String, RecipientResponse] = RecipientResponse.fromPlaintextMessage(msg)
  def toRecipientQuery: Either[String, RecipientQuery] = RecipientQuery.fromPlaintextMessage(msg)
  def toRecipient: Either[String, Recipient] = Recipient.fromPlaintextMessage(msg)

enum RecipientAction:
  case add extends RecipientAction
  case remove extends RecipientAction

object RecipientAction {
  given decoder: JsonDecoder[RecipientAction] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(RecipientAction.valueOf(e)))
  given encoder: JsonEncoder[RecipientAction] =
    JsonEncoder.string.contramap((e: RecipientAction) => e.toString)
}

enum RecipientResult:
  case client_error extends RecipientResult
  case server_error extends RecipientResult
  case no_change extends RecipientResult
  case success extends RecipientResult

object RecipientResult:
  given decoder: JsonDecoder[RecipientResult] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(RecipientResult.valueOf(e)))
  given encoder: JsonEncoder[RecipientResult] = JsonEncoder.string.contramap((e: RecipientResult) => e.toString)

final case class RecipientUpdate(id: MsgID = MsgID(), from: FROM, to: TO, updates: Seq[(FROMTO, RecipientAction)]) {
  def piuri = RecipientUpdate.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        RecipientUpdate
          .Body(updates =
            updates.map(e =>
              RecipientUpdate.Update(
                recipient_did = e._1,
                action = e._2
              )
            )
          )
          .toJSON_RFC7159
      ),
      return_route = Some(ReturnRoute.all), // Protocol expect recipient to get reply on the same channel
    )
  def makeRecipientResponse(updated: Seq[(FROMTO, RecipientAction, RecipientResult)]) =
    RecipientResponse(thid = id, to = from.asTO, from = to.asFROM, updated = updated)
}

/** TODO we don't believe this behavior is correct or secure. But ismimic the behavior of RootsID mediator
  *
  * https://identity.foundation/didcomm-messaging/spec/#routing-protocol-20:~:text=rfc587%22%0A%20%20%[…]le%3Asomemediator%23somekey%22%5D,-%7D%5D%0A%7D
  */
object RecipientUpdate {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/3.0/recipient-update")

  protected final case class Update(recipient_did: FROMTO, action: RecipientAction) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Update {
    given decoder: JsonDecoder[Update] = DeriveJsonDecoder.gen[Update]
    given encoder: JsonEncoder[Update] = DeriveJsonEncoder.gen[Update]
  }

  protected final case class Body(updates: Seq[Update]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, RecipientUpdate] =
    if (msg.`type` != piuri) Left(s"No able to create RecipientUpdate from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq()            => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None       => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.body.map(_.as[Body]) match
                case None              => Left(s"'$piuri' MUST have a 'body'")
                case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                case Some(Right(body)) =>
                  Right(
                    RecipientUpdate(
                      id = msg.id,
                      from = from,
                      to = firstTo,
                      updates = body.updates.map(e => (e.recipient_did, e.action))
                    )
                  )

        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

final case class RecipientResponse(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    updated: Seq[(FROMTO, RecipientAction, RecipientResult)]
) {
  def piuri = RecipientResponse.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      thid = Some(thid),
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        RecipientResponse
          .Body(updated =
            updated.map(e =>
              RecipientResponse.Updated(
                recipient_did = e._1,
                action = e._2,
                result = e._3
              )
            )
          )
          .toJSON_RFC7159
      ),
    )
}

object RecipientResponse {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/3.0/recipient-update-response")

  protected final case class Updated(recipient_did: FROMTO, action: RecipientAction, result: RecipientResult) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Updated {
    given decoder: JsonDecoder[Updated] = DeriveJsonDecoder.gen[Updated]
    given encoder: JsonEncoder[Updated] = DeriveJsonEncoder.gen[Updated]
  }

  protected final case class Body(updated: Seq[Updated]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, RecipientResponse] =
    if (msg.`type` != piuri) Left(s"No able to create RecipientResponse from a Message of type '${msg.`type`}'")
    else
      msg.thid match
        case None       => Left(s"'$piuri' MUST have field 'thid'")
        case Some(thid) =>
          msg.to.toSeq.flatten match // Note: toSeq is from the match
            case Seq()            => Left(s"'$piuri' MUST have field 'to' with one element")
            case firstTo +: Seq() =>
              msg.from match
                case None       => Left(s"'$piuri' MUST have field 'from'")
                case Some(from) =>
                  msg.body.map(_.as[Body]) match
                    case None              => Left(s"'$piuri' MUST have a 'body'")
                    case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                    case Some(Right(body)) =>
                      Right(
                        RecipientResponse(
                          id = msg.id,
                          thid = thid,
                          from = from,
                          to = firstTo,
                          updated = body.updated.map(e => (e.recipient_did, e.action, e.result))
                        )
                      )
            case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

/** {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/coordinate-mediation/3.0/recipient-query",
  *   "body": {"paginate": {"limit": 30,"offset": 0}}
  * }
  * }}}
  *
  * @param id
  * @param from
  * @param to
  * @param paginate
  *   is optional, and if present must include limit and offset.
  */
final case class RecipientQuery(
    id: MsgID = MsgID(),
    from: FROM,
    to: TO,
    paginate: Option[RecipientQuery.Paginate],
) {
  def piuri = RecipientQuery.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        RecipientQuery
          .Body(paginate = paginate)
          .toJSON_RFC7159
      ),
      return_route = Some(ReturnRoute.all), // Protocol expect recipient to get reply on the same channel
    )
}
object RecipientQuery {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/3.0/recipient-query")

  protected final case class Paginate(limit: Int, offset: Int) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Paginate {
    given decoder: JsonDecoder[Paginate] = DeriveJsonDecoder.gen[Paginate]
    given encoder: JsonEncoder[Paginate] = DeriveJsonEncoder.gen[Paginate]
  }

  protected final case class Body(paginate: Option[Paginate]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, RecipientQuery] =
    if (msg.`type` != piuri) Left(s"No able to create RecipientQuery from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq()            => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None       => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.body.map(_.as[Body]) match
                case None              => Left(s"'$piuri' MUST have a 'body'")
                case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                case Some(Right(body)) =>
                  Right(
                    RecipientQuery(
                      id = msg.id,
                      from = from,
                      to = firstTo,
                      paginate = body.paginate
                    )
                  )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

/** {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/coordinate-mediation/3.0/recipient",
  *   "body": {
  *     "keys": [{"recipient_did": ...}]
  *     "pagination": {"count": 30,"offset": 30,"remaining": 100}
  *   }
  * }
  * }}}
  *
  * @param id
  * @param thid
  * @param from
  * @param to
  * @param keys
  * @param pagination
  *   is optional, and if present must include count, offset and remaining
  */
final case class Recipient(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    dids: Seq[Recipient.RecipientDID],
    pagination: Option[Recipient.Pagination],
    // count: Int,
    // offset: Int,
    // remaining: Int,
) {
  def piuri = Recipient.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      thid = Some(thid),
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        Recipient
          .Body(
            dids = dids,
            pagination = pagination,
          )
          .toJSON_RFC7159
      ),
    )
}

object Recipient {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/3.0/recipient")

  protected final case class Pagination(count: Int, offset: Int, remaining: Int) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Pagination {
    given decoder: JsonDecoder[Pagination] = DeriveJsonDecoder.gen[Pagination]
    given encoder: JsonEncoder[Pagination] = DeriveJsonEncoder.gen[Pagination]
  }

  protected final case class RecipientDID(recipient_did: FROMTO) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object RecipientDID {
    given decoder: JsonDecoder[RecipientDID] = DeriveJsonDecoder.gen[RecipientDID]
    given encoder: JsonEncoder[RecipientDID] = DeriveJsonEncoder.gen[RecipientDID]
  }

  protected final case class Body(dids: Seq[RecipientDID], pagination: Option[Pagination]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Recipient] =
    if (msg.`type` != piuri) Left(s"No able to create Recipient from a Message of type '${msg.`type`}'")
    else
      msg.thid match
        case None       => Left(s"'$piuri' MUST have field 'thid'")
        case Some(thid) =>
          msg.to.toSeq.flatten match // Note: toSeq is from the match
            case Seq()            => Left(s"'$piuri' MUST have field 'to' with one element")
            case firstTo +: Seq() =>
              msg.from match
                case None       => Left(s"'$piuri' MUST have field 'from'")
                case Some(from) =>
                  msg.body.map(_.as[Body]) match
                    case None              => Left(s"'$piuri' MUST have a 'body'")
                    case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                    case Some(Right(body)) =>
                      Right(
                        Recipient(
                          id = msg.id,
                          thid = thid,
                          from = from,
                          to = firstTo,
                          dids = body.dids,
                          pagination = body.pagination,
                        )
                      )
            case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
