package fmgp.did.comm.protocol.chatriqube.registry

import zio.json.*

import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.chatriqube.SubjectType

extension (msg: PlaintextMessage)
  def toEnroll: Either[String, Enroll] = Enroll.fromPlaintextMessage(msg)
  def toAccount: Either[String, Account] = Account.fromPlaintextMessage(msg)
  def toSetId: Either[String, SetId] = SetId.fromPlaintextMessage(msg)

// ##############
// ### Enroll ###
// ##############

final case class Enroll(id: MsgID = MsgID(), from: FROM, to: TO) {
  def piuri = Enroll.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      return_route = Some(ReturnRoute.all), // Protocol expect recipient to get reply on the same channel
    )
}

object Enroll {
  def piuri = PIURI("https://decentriqube.com/registry/1/enroll")

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Enroll] =
    if (msg.`type` != piuri) Left(s"No able to create Enroll from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq()            => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None       => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              Right(
                Enroll(
                  id = msg.id,
                  from = from,
                  to = firstTo,
                )
              )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

// ###############
// ### Account ###
// ###############

final case class Account(
    id: MsgID = MsgID(),
    thid: MsgID,
    from: FROM,
    to: TO,
    ids: Seq[Account.IdentityEntry]
) {
  def piuri = Account.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(Account.Body(ids = ids).toJSON_RFC7159)
    )
}

object Account {
  def piuri = PIURI("https://decentriqube.com/registry/1/account")

  @jsonMemberNames(SnakeCase)
  case class IdentityEntry(
      subjectType: SubjectType,
      subject: String
  )
  object IdentityEntry {
    given decoder: JsonDecoder[IdentityEntry] = DeriveJsonDecoder.gen[IdentityEntry]
    given encoder: JsonEncoder[IdentityEntry] = DeriveJsonEncoder.gen[IdentityEntry]
  }

  protected final case class Body(ids: Seq[IdentityEntry]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Account] =
    if (msg.`type` != piuri) Left(s"No able to create Account from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq()            => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None    => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.thid match
                  case None       => Left(s"'$piuri' MUST have field 'thid'")
                  case Some(thid) =>
                    msg.from match
                      case None       => Left(s"'$piuri' MUST have field 'from'")
                      case Some(from) =>
                        Right(
                          Account(
                            id = msg.id,
                            thid = thid,
                            from = from,
                            to = firstTo,
                            ids = body.ids,
                          )
                        )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

// #############
// ### SetId ###
// #############

final case class SetId(
    id: MsgID = MsgID(),
    from: FROM,
    to: TO,
    subjectType: SubjectType,
    subject: String,
    proof: String,
) { // FIXME add proof
  def piuri = SetId.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(SetId.Body(subjectType = subjectType, subject = subject, proof = proof).toJSON_RFC7159)
    )
}

object SetId {
  def piuri = PIURI("https://decentriqube.com/registry/1/set_id")

  @jsonMemberNames(SnakeCase)
  protected final case class Body(subjectType: SubjectType, subject: String, proof: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, SetId] =
    if (msg.`type` != piuri) Left(s"No able to create SetId from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq()            => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None    => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.from match
                  case None       => Left(s"'$piuri' MUST have field 'from'")
                  case Some(from) =>
                    Right(
                      SetId(
                        id = msg.id,
                        from = from,
                        to = firstTo,
                        subjectType = body.subjectType,
                        subject = body.subject,
                        proof = body.proof,
                      )
                    )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
