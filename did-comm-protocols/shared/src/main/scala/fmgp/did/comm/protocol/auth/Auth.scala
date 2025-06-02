package fmgp.did.comm.protocol.auth

import zio.json._

import fmgp.did._
import fmgp.did.comm._

// RequestVerification VerificationChallenge Prove ConfirmVerification
extension (msg: PlaintextMessage)
  def toAuthRequest: Either[String, AuthRequest] =
    AuthRequest.fromPlaintextMessage(msg)
  def toAuthMsg: Either[String, AuthMsg] =
    AuthMsg.fromPlaintextMessage(msg)

final case class AuthRequest(
    id: MsgID = MsgID(),
    from: FROM,
    pthid: NotRequired[MsgID] = None,
) {
  def `type` = AuthRequest.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      from = Some(from),
    )

  def replyWithAuth(from: FROM): AuthMsg = AuthMsg.replyToAuthRequest(this, from)
}

object AuthRequest {
  def piuri = PIURI("https://fmgp.app/auth/0.1/request")

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, AuthRequest] = {
    if (msg.`type` != piuri)
      Left(s"No able to create AuthRequest from a Message of the type '${msg.`type`}'")
    else
      msg.from match
        case None => Left(s"'$piuri' MUST have field 'from'")
        case Some(from) =>
          Right(
            AuthRequest(
              id = msg.id,
              from = from,
              pthid = msg.pthid,
            )
          )
  }
}

final case class AuthMsg(
    id: MsgID = MsgID(),
    to: Required[TO],
    from: Required[FROM],
    thid: Required[MsgID],
    pthid: NotRequired[MsgID] = None,
    // lang: NotRequired[String] = None,
    created_time: NotRequired[UTCEpoch] = None,
) {
  def `type` = AuthMsg.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(Set(to)),
      from = Some(from),
      thid = Some(thid),
      pthid = pthid,
      created_time = created_time,
    )

}

object AuthMsg {
  def piuri = PIURI("https://fmgp.app/auth/0.1/msg")

  // protected final case class Body(scopes: Option[Seq[String]], metaData: Option[Map[String, String]]) {

  //   /** toJSON_RFC7159 MUST not fail! */
  //   def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  // }
  // protected object Body {
  //   given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
  //   given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  // }

  def replyToAuthRequest(msg: AuthRequest, from: FROM) =
    AuthMsg(
      id = MsgID(),
      to = msg.from.asTO,
      from = from,
      thid = msg.id,
      pthid = msg.pthid,
    )

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, AuthMsg] = {
    if (msg.`type` != piuri)
      Left(s"No able to create AuthMsg from a Message of the type '${msg.`type`}'")
    else
      msg.thid match
        case None => Left(s"'$piuri' MUST have field 'thid'")
        case Some(thid) =>
          msg.to.toSeq.flatten match // Note: toSeq is from the match
            case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
            case to +: Seq() =>
              msg.from match
                case None       => Left(s"'$piuri' MUST have field 'from'")
                case Some(from) =>
                  // msg.body match
                  //   case None => Left(s"'$piuri' MUST have field 'body'")
                  //   case Some(b) =>
                  //     b.as[Body].map { body =>
                  Right(
                    AuthMsg(
                      id = msg.id,
                      to = to,
                      from = from,
                      thid = thid,
                      pthid = msg.pthid,
                      created_time = msg.created_time,
                    )
                  )
                // }
            case tos => Left(s"'$piuri' MUST have field 'to' with only one element")

  }
}
