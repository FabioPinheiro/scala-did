package fmgp.did.comm.protocol.chatriqube.discovery

import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.chatriqube.SubjectType

extension (msg: PlaintextMessage)
  def toAskIntroduction: Either[String, AskIntroduction] = AskIntroduction.fromPlaintextMessage(msg)
  def toForwardRequest: Either[String, ForwardRequest] = ForwardRequest.fromPlaintextMessage(msg)
  def toRequest: Either[String, Request] = Request.fromPlaintextMessage(msg)
  def toAnswer: Either[String, Answer] = Answer.fromPlaintextMessage(msg)
  def toHandshake: Either[String, Handshake] = Handshake.fromPlaintextMessage(msg)

// ########################
// ### Ask Introduction ###
// ########################

final case class AskIntroduction(id: MsgID = MsgID(), from: FROM, to: TO, request: SignedMessage) {
  def piuri = AskIntroduction.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      return_route = Some(ReturnRoute.all), // Protocol expect recipient to get reply on the same channel
      attachments = Some(Seq(Attachment.fromMessage(request)))
    )
}

object AskIntroduction {
  def piuri = PIURI("https://decentriqube.com/discovery/1/ask_introduction")

  // protected final case class Body(content: String) {

  //   /** toJSON_RFC7159 MUST not fail! */
  //   def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  // }
  // protected object Body {
  //   given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
  //   given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  // }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, AskIntroduction] =
    if (msg.`type` != piuri) Left(s"No able to create AskIntroduction from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.attachments match
                case None        => Left(s"'$piuri' MUST have 'attachments'")
                case Some(Seq()) => Left(s"'$piuri' MUST have one 'attachments'")
                case Some(firstAttachment +: Seq()) => {
                  firstAttachment.getAsMessage
                    .flatMap {
                      case pMsg: PlaintextMessage => Left("Expecting SignedMessage instead of PlaintextMessage")
                      case eMsg: EncryptedMessage => Left("Expecting SignedMessage instead of EncryptedMessage")
                      case sMsg: SignedMessage    => Right(sMsg)
                    }
                    .flatMap(sMsg =>
                      Right(
                        AskIntroduction(
                          id = msg.id,
                          from = from,
                          to = firstTo,
                          request = sMsg,
                        )
                      )
                    )

                }
                case Some(_) => Left(s"'$piuri' MUST have only one 'attachments'")
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

// #######################
// ### Forward Request ###
// #######################

final case class ForwardRequest(id: MsgID = MsgID(), thid: MsgID, from: FROM, to: TO, request: SignedMessage) {
  def piuri = ForwardRequest.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      attachments = Some(Seq(Attachment.fromMessage(request)))
    )
}

object ForwardRequest {
  def piuri = PIURI("https://decentriqube.com/discovery/1/forward_request")

  protected final case class Body(Scope: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, ForwardRequest] =
    if (msg.`type` != piuri) Left(s"No able to create ForwardRequest from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.thid match
                case None => Left(s"'$piuri' MUST have field 'thid'")
                case Some(thid) =>
                  msg.body match
                    case None => Left(s"'$piuri' MUST have field 'body'")
                    case Some(b) =>
                      b.as[Body].flatMap { body =>
                        msg.attachments match
                          case None        => Left(s"'$piuri' MUST have 'attachments'")
                          case Some(Seq()) => Left(s"'$piuri' MUST have one 'attachments'")
                          case Some(firstAttachment +: Seq()) => {
                            firstAttachment.getAsMessage
                              .flatMap {
                                case pMsg: PlaintextMessage =>
                                  Left("Expecting SignedMessage instead of PlaintextMessage")
                                case eMsg: EncryptedMessage =>
                                  Left("Expecting SignedMessage instead of EncryptedMessage")
                                case sMsg: SignedMessage => Right(sMsg)
                              }
                              .flatMap(sMsg =>
                                Right(
                                  ForwardRequest(
                                    id = msg.id,
                                    thid = thid,
                                    from = from,
                                    to = firstTo,
                                    request = sMsg,
                                  )
                                )
                              )
                          }
                          case Some(_) => Left(s"'$piuri' MUST have only one 'attachments'")
                      }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

// ###############
// ### Request ###
// ###############

final case class Request(id: MsgID = MsgID(), from: FROM, subjectType: SubjectType, subject: String) {
  def piuri = Request.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = None,
      from = Some(from),
      body = Some(
        Request
          .Body(
            subjectType = subjectType,
            subject = subject,
          )
          .toJSON_RFC7159
      )
    )
}

object Request {
  def piuri = PIURI("https://decentriqube.com/discovery/1/request")

  protected final case class Body(subjectType: SubjectType, subject: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Request] =
    if (msg.`type` != piuri) Left(s"No able to create Request from a Message of type '${msg.`type`}'")
    else
      // msg.to.toSeq.flatten match // Note: toSeq is from the match
      //   case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
      //   case firstTo +: Seq() =>
      msg.from match
        case None => Left(s"'$piuri' MUST have field 'from'")
        case Some(from) =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                // msg.thid match
                //   case None => Left(s"'$piuri' MUST have field 'thid'")
                //   case Some(thid) =>
                Right(
                  Request(
                    id = msg.id,
                    from = from,
                    subjectType = body.subjectType,
                    subject = body.subject,
                  )
                )
              }
    // case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

// ##############
// ### Answer ###
// ##############

final case class Answer(
    id: MsgID = MsgID(),
    thid: MsgID,
    pthid: Option[MsgID],
    from: FROM,
    to: TO,
    subjectType: SubjectType,
    subject: String
) {
  def piuri = Answer.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      thid = Some(thid),
      pthid = pthid,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(
        Answer
          .Body(
            subjectType = subjectType,
            subject = subject,
          )
          .toJSON_RFC7159
      )
    )
}

object Answer {
  def piuri = PIURI("https://decentriqube.com/discovery/1/answer")

  protected final case class Body(subjectType: SubjectType, subject: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Answer] =
    if (msg.`type` != piuri) Left(s"No able to create Answer from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.thid match
                case None => Left(s"'$piuri' MUST have field 'thid'")
                case Some(thid) =>
                  msg.body match
                    case None => Left(s"'$piuri' MUST have field 'body'")
                    case Some(b) =>
                      b.as[Body].flatMap { body =>
                        Right(
                          Answer(
                            id = msg.id,
                            thid = thid,
                            pthid = msg.pthid,
                            from = from,
                            to = firstTo,
                            subjectType = body.subjectType,
                            subject = body.subject,
                          )
                        )
                      }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

// #################
// ### Handshake ###
// #################

final case class Handshake(id: MsgID = MsgID(), thid: MsgID, from: FROM, to: TO) {
  def piuri = Handshake.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      thid = Some(thid),
      to = Some(Set(to)),
      from = Some(from),
    )
}

object Handshake {
  def piuri = PIURI("https://decentriqube.com/discovery/1/handshake")

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Handshake] =
    if (msg.`type` != piuri) Left(s"No able to create Handshake from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.thid match
                case None => Left(s"'$piuri' MUST have field 'thid'")
                case Some(thid) =>
                  Right(
                    Handshake(
                      id = msg.id,
                      thid = thid,
                      from = from,
                      to = firstTo,
                    )
                  )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
