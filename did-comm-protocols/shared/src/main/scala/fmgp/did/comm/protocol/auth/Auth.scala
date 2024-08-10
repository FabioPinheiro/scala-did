package fmgp.did.comm.protocol.auth

import zio.json._

import fmgp.did._
import fmgp.did.comm._

final case class Auth(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: NotRequired[FROM],
    thid: NotRequired[MsgID] = None,
    pthid: NotRequired[MsgID] = None,
    lang: NotRequired[String] = None,
    created_time: NotRequired[UTCEpoch] = None,
    // Body
    scopes: Seq[String],
    metaData: Map[String, String]
) {
  def `type` = Auth.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(to),
      from = from,
      thid = None,
      pthid = None,
      created_time = created_time,
      body = Some(
        Auth
          .Body(
            scopes = Some(scopes).filter(_.isEmpty),
            metaData = Some(metaData).filter(_.isEmpty)
          )
          .toJSON_RFC7159
      ),
    )

}

object Auth {
  def piuri = PIURI("https://fmgp.app/auth/0.1/auth")

  protected final case class Body(scopes: Option[Seq[String]], metaData: Option[Map[String, String]]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Auth] = {
    if (msg.`type` != piuri)
      Left(s"No able to create Auth from a Message of the type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case tos =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].map { body =>
                Auth(
                  id = msg.id,
                  to = tos.toSet,
                  from = msg.from,
                  thid = msg.thid,
                  pthid = msg.pthid,
                  lang = None,
                  created_time = msg.created_time,
                  scopes = body.scopes.getOrElse(Seq.empty),
                  metaData = body.metaData.getOrElse(Map.empty),
                )
              }

  }
}
