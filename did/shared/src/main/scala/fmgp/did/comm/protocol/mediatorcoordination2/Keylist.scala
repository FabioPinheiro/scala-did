package fmgp.did.comm.protocol.mediatorcoordination2

import zio.json._
import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toKeylistUpdate: Either[String, KeylistUpdate] = KeylistUpdate.fromPlaintextMessage(msg)
  def toKeylistResponse: Either[String, KeylistResponse] = KeylistResponse.fromPlaintextMessage(msg)
  def toKeylistQuery: Either[String, KeylistQuery] = KeylistQuery.fromPlaintextMessage(msg)
  def toKeylist: Either[String, Keylist] = Keylist.fromPlaintextMessage(msg)

enum KeylistAction:
  case add extends KeylistAction
  case remove extends KeylistAction

object KeylistAction {
  given decoder: JsonDecoder[KeylistAction] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(KeylistAction.valueOf(e)))
  given encoder: JsonEncoder[KeylistAction] =
    JsonEncoder.string.contramap((e: KeylistAction) => e.toString)
}

enum KeylistResult:
  case client_error extends KeylistResult
  case server_error extends KeylistResult
  case no_change extends KeylistResult
  case success extends KeylistResult

object KeylistResult:
  given decoder: JsonDecoder[KeylistResult] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(KeylistResult.valueOf(e)))
  given encoder: JsonEncoder[KeylistResult] = JsonEncoder.string.contramap((e: KeylistResult) => e.toString)

final case class KeylistUpdate(id: MsgID = MsgID(), from: FROM, to: TO, updates: Seq[(FROMTO, KeylistAction)]) {
  def piuri = KeylistUpdate.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        KeylistUpdate
          .Body(updates =
            updates.map(e =>
              KeylistUpdate.Update(
                recipient_did = e._1,
                action = e._2
              )
            )
          )
          .toJSON_RFC7159
      ),
      return_route = Some(ReturnRoute.all), // Protocol expect recipient to get reply on the same channel
    )
  def makeKeylistResponse(updated: Seq[(FROMTO, KeylistAction, KeylistResult)]) =
    KeylistResponse(thid = id, to = from.asTO, from = to.asFROM, updated)
}

/** TODO we don't believe this behavior is correct or secure. But ismimic the behavior of RootsID mediator
  *
  * https://identity.foundation/didcomm-messaging/spec/#routing-protocol-20:~:text=rfc587%22%0A%20%20%[…]le%3Asomemediator%23somekey%22%5D,-%7D%5D%0A%7D
  */
object KeylistUpdate {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-update")

  protected final case class Update(recipient_did: FROMTO, action: KeylistAction) {

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

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, KeylistUpdate] =
    if (msg.`type` != piuri) Left(s"No able to create KeylistUpdate from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.body.map(_.as[Body]) match
                case None              => Left(s"'$piuri' MUST have a 'body'")
                case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                case Some(Right(body)) =>
                  Right(
                    KeylistUpdate(
                      id = msg.id,
                      from = from,
                      to = firstTo,
                      updates = body.updates.map(e => (e.recipient_did, e.action))
                    )
                  )

        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

final case class KeylistResponse(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    updated: Seq[(FROMTO, KeylistAction, KeylistResult)]
) {
  def piuri = KeylistResponse.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      thid = Some(thid),
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        KeylistResponse
          .Body(updated =
            updated.map(e =>
              KeylistResponse.Updated(
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

object KeylistResponse {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-update-response")

  protected final case class Updated(recipient_did: FROMTO, action: KeylistAction, result: KeylistResult) {

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

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, KeylistResponse] =
    if (msg.`type` != piuri) Left(s"No able to create KeylistResponse from a Message of type '${msg.`type`}'")
    else
      msg.thid match
        case None => Left(s"'$piuri' MUST have field 'thid'")
        case Some(thid) =>
          msg.to.toSeq.flatten match // Note: toSeq is from the match
            case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
            case firstTo +: Seq() =>
              msg.from match
                case None => Left(s"'$piuri' MUST have field 'from'")
                case Some(from) =>
                  msg.body.map(_.as[Body]) match
                    case None              => Left(s"'$piuri' MUST have a 'body'")
                    case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                    case Some(Right(body)) =>
                      Right(
                        KeylistResponse(
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
  *   "type": "https://didcomm.org/coordinate-mediation/2.0/keylist-query",
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
final case class KeylistQuery(
    id: MsgID = MsgID(),
    from: FROM,
    to: TO,
    paginate: Option[KeylistQuery.Paginate],
) {
  def piuri = KeylistQuery.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        KeylistQuery
          .Body(paginate = paginate)
          .toJSON_RFC7159
      ),
      return_route = Some(ReturnRoute.all), // Protocol expect recipient to get reply on the same channel
    )
}
object KeylistQuery {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist-query")

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

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, KeylistQuery] =
    if (msg.`type` != piuri) Left(s"No able to create KeylistQuery from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.body.map(_.as[Body]) match
                case None              => Left(s"'$piuri' MUST have a 'body'")
                case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                case Some(Right(body)) =>
                  Right(
                    KeylistQuery(
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
  *   "type": "https://didcomm.org/coordinate-mediation/2.0/keylist",
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
final case class Keylist(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    keys: Seq[Keylist.RecipientDID],
    pagination: Option[Keylist.Pagination],
    // count: Int,
    // offset: Int,
    // remaining: Int,
) {
  def piuri = Keylist.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      thid = Some(thid),
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        Keylist
          .Body(
            keys = keys,
            pagination = pagination,
          )
          .toJSON_RFC7159
      ),
    )
}

object Keylist {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/2.0/keylist")

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

  protected final case class Body(keys: Seq[RecipientDID], pagination: Option[Pagination]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Keylist] =
    if (msg.`type` != piuri) Left(s"No able to create Keylist from a Message of type '${msg.`type`}'")
    else
      msg.thid match
        case None => Left(s"'$piuri' MUST have field 'thid'")
        case Some(thid) =>
          msg.to.toSeq.flatten match // Note: toSeq is from the match
            case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
            case firstTo +: Seq() =>
              msg.from match
                case None => Left(s"'$piuri' MUST have field 'from'")
                case Some(from) =>
                  msg.body.map(_.as[Body]) match
                    case None              => Left(s"'$piuri' MUST have a 'body'")
                    case Some(Left(value)) => Left(s"'$piuri' MUST have valid 'body'. Fail due: $value")
                    case Some(Right(body)) =>
                      Right(
                        Keylist(
                          id = msg.id,
                          thid = thid,
                          from = from,
                          to = firstTo,
                          keys = body.keys,
                          pagination = body.pagination,
                        )
                      )
            case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
