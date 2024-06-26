package fmgp.did.comm.protocol.mediatorcoordination3
/* https://didcomm.org/mediator-coordination/3.0/ */

import zio.json._
import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toMediateRequest: Either[String, MediateRequest] = MediateRequest.fromPlaintextMessage(msg)
  def toMediateDeny: Either[String, MediateDeny] = MediateDeny.fromPlaintextMessage(msg)
  def toMediateGrant: Either[String, MediateGrant] = MediateGrant.fromPlaintextMessage(msg)
  def toMediateGrantOrDeny: Either[String, MediateGrant | MediateDeny] =
    val TYPE_mediate_grant = MediateGrant.piuri
    val TYPE_mediate_deny = MediateDeny.piuri
    msg.`type` match
      case `TYPE_mediate_grant` => MediateGrant.fromPlaintextMessage(msg)
      case `TYPE_mediate_deny`  => MediateDeny.fromPlaintextMessage(msg)
      case typeOther => Left(s"No able to create MediateGrant/MediateDeny from a Message of type '$typeOther'")

/** This message serves as a request from the recipient to the mediator, asking for the permission (and routing
  * information) to publish the endpoint as a mediator.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/coordinate-mediation/3.0/mediate-request",
  * }
  * }}}
  */
final case class MediateRequest(id: MsgID = MsgID(), from: FROM, to: TO) {
  def piuri = MediateRequest.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(Set(to)),
      from = Some(from),
      return_route = Some(ReturnRoute.all), // Protocol expect recipient to get reply on the same channel
    )
  def makeRespondMediateGrant =
    MediateGrant(
      thid = id,
      to = from.asTO,
      from = to.asFROM,
      routing_did = Seq(to.asFROMTO)
    )
  def makeRespondMediateDeny = MediateDeny(thid = id, to = from.asTO, from = to.asFROM)
}
object MediateRequest {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/3.0/mediate-request")

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, MediateRequest] =
    if (msg.`type` != piuri) Left(s"No able to create MediateRequest from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.from match
            case None => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              Right(
                MediateRequest(
                  id = msg.id,
                  from = from,
                  to = firstTo,
                )
              )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

/** This message serves as notification of the mediator denying the recipient's request for mediation.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/coordinate-mediation/3.0/mediate-deny",
  * }
  * }}}
  */
final case class MediateDeny(id: MsgID = MsgID(), thid: MsgID, from: FROM, to: TO) {
  def piuri = MediateDeny.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      thid = Some(thid),
      to = Some(Set(to)),
      from = Some(from),
    )
}
object MediateDeny {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/3.0/mediate-deny")

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, MediateDeny] =
    if (msg.`type` != piuri) Left(s"No able to create MediateDeny from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.thid match
            case None => Left(s"'$piuri' MUST have field 'thid'")
            case Some(thid) =>
              msg.from match
                case None => Left(s"'$piuri' MUST have field 'from'")
                case Some(from) =>
                  Right(
                    MediateDeny(
                      id = msg.id,
                      thid = thid,
                      from = from,
                      to = firstTo,
                    )
                  )
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}

/** A mediate grant message is a signal from the mediator to the recipient that permission is given to distribute the
  * included information as an inbound route.
  *
  * @param routing_did
  *   DID of the mediator where forwarded messages should be sent. The recipient may use this DID as an enpoint as
  *   explained in Using a DID as an endpoint section of the specification.
  *
  * {{{
  * {
  *   "id": "123456780",
  *   "type": "https://didcomm.org/coordinate-mediation/3.0/mediate-grant",
  *   "body": {"routing_did": ["did:peer:z6Mkfriq1MqLBoPWecGoDLjguo1sB9brj6wT3qZ5BxkKpuP6"]}
  * }
  * }}}
  */
final case class MediateGrant(id: MsgID = MsgID(), thid: MsgID, from: FROM, to: TO, routing_did: Seq[FROMTO]) {
  def piuri = MediateGrant.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = piuri,
      id = id,
      thid = Some(thid),
      to = Some(Set(to)),
      from = Some(from),
      body = Some(MediateGrant.Body(routing_did).toJSON_RFC7159) // FIXME FIX Body
    )
}
object MediateGrant {
  def piuri = PIURI("https://didcomm.org/coordinate-mediation/3.0/mediate-grant")
  protected final case class Body(routing_did: Seq[FROMTO]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, MediateGrant] =
    if (msg.`type` != piuri) Left(s"No able to create MediateGrant from a Message of type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.thid match
                  case None => Left(s"'$piuri' MUST have field 'thid'")
                  case Some(thid) =>
                    msg.from match
                      case None => Left(s"'$piuri' MUST have field 'from'")
                      case Some(from) =>
                        Right(
                          MediateGrant(
                            id = msg.id,
                            thid = thid,
                            from = from,
                            to = firstTo,
                            routing_did = body.routing_did
                          )
                        )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
}
