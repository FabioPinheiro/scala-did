package fmgp.did.comm.protocol.pubsub

import zio.json._

import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toRequest: Either[String, RequestToSubscribe] =
    RequestToSubscribe.fromPlaintextMessage(msg)
  def toSetup: Either[String, SetupToSubscribe] =
    SetupToSubscribe.fromPlaintextMessage(msg)
  def toSubscribe: Either[String, Subscribe] =
    Subscribe.fromPlaintextMessage(msg)
  def toSubscription: Either[String, Subscription] =
    Subscription.fromPlaintextMessage(msg)

final case class RequestToSubscribe(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: FROM,
    thid: NotRequired[MsgID] = None,
    pthid: NotRequired[MsgID] = None,
    created_time: NotRequired[UTCEpoch] = None,
    // Body
    body: RequestToSubscribe.Body
) {
  def `type` = RequestToSubscribe.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(to),
      from = Some(from),
      // thid = thid,
      pthid = pthid,
      created_time = created_time,
      body = Some(body.toJSON_RFC7159),
    )
}

object RequestToSubscribe {
  def piuri = PIURI("https://fmgp.app/pubsub/v1/request")

  final case class Body() {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, RequestToSubscribe] = {
    if (msg.`type` != piuri)
      Left(s"No able to create RequestToSubscribe from a Message of the type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case tos   =>
          msg.from match
            case None       => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.body match
                case None    => Left(s"'$piuri' MUST have field 'body'")
                case Some(b) =>
                  b.as[Body].map { body =>
                    RequestToSubscribe(
                      id = msg.id,
                      to = tos.toSet,
                      from = from,
                      // thid = msg.thid,
                      pthid = msg.pthid,
                      created_time = msg.created_time,
                      body = body,
                    )
                  }
  }

}

final case class SetupToSubscribe(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: FROM,
    thid: NotRequired[MsgID] = None,
    pthid: NotRequired[MsgID] = None,
    created_time: NotRequired[UTCEpoch] = None,
    // Body
    body: SetupToSubscribe.Body
) {
  def `type` = SetupToSubscribe.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(to),
      from = Some(from),
      thid = thid,
      pthid = pthid,
      created_time = created_time,
      body = Some(body.toJSON_RFC7159),
    )
}

object SetupToSubscribe {
  def piuri = PIURI("https://fmgp.app/pubsub/v1/setup")

  final case class Body(publicKey: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, SetupToSubscribe] = {
    if (msg.`type` != piuri)
      Left(s"No able to create SetupToSubscribe from a Message of the type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case tos   =>
          msg.from match
            case None       => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.body match
                case None    => Left(s"'$piuri' MUST have field 'body'")
                case Some(b) =>
                  b.as[Body].map { body =>
                    SetupToSubscribe(
                      id = msg.id,
                      to = tos.toSet,
                      from = from,
                      thid = msg.thid,
                      pthid = msg.pthid,
                      created_time = msg.created_time,
                      body = body,
                    )
                  }
  }

}

final case class Subscribe(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: FROM,
    thid: NotRequired[MsgID] = None,
    pthid: NotRequired[MsgID] = None,
    created_time: NotRequired[UTCEpoch] = None,
    // Body
    body: Subscribe.Body
) {
  def `type` = Subscribe.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(to),
      from = Some(from),
      thid = thid,
      pthid = pthid,
      created_time = created_time,
      body = Some(body.toJSON_RFC7159),
    )
}

object Subscribe {
  def piuri = PIURI("https://fmgp.app/pubsub/v1/subscribe")

  // TODO rename since is not protected
  final case class Body(endpoint: String, keyP256DH: String, keyAUTH: String, id: Option[String]) {
    def name: String = id.getOrElse("?_?") // TODO

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Subscribe] = {
    if (msg.`type` != piuri)
      Left(s"No able to create Subscribe from a Message of the type '${msg.`type`}'")
    else
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case tos   =>
          msg.from match
            case None       => Left(s"'$piuri' MUST have field 'from'")
            case Some(from) =>
              msg.body match
                case None    => Left(s"'$piuri' MUST have field 'body'")
                case Some(b) =>
                  b.as[Body].map { body =>
                    Subscribe(
                      id = msg.id,
                      to = tos.toSet,
                      from = from,
                      thid = msg.thid,
                      pthid = msg.pthid,
                      created_time = msg.created_time,
                      body = body,
                    )
                  }
  }
}

final case class Subscription(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: FROM,
    thid: MsgID,
    pthid: NotRequired[MsgID] = None,
    created_time: NotRequired[UTCEpoch] = None,
    // Body
    body: Subscription.Body
) {
  def `type` = Subscription.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(to),
      from = Some(from),
      thid = Some(thid),
      pthid = pthid,
      created_time = created_time,
      body = Some(body.toJSON_RFC7159),
    )

}

object Subscription {
  def piuri = PIURI("https://fmgp.app/pubsub/v1/subscription")

  // TODO rename since is not protected
  final case class Body() {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Subscription] = {
    if (msg.`type` != piuri)
      Left(s"No able to create Subscription from a Message of the type '${msg.`type`}'")
    else
      msg.thid match
        case None       => Left(s"'$piuri' MUST have field 'thid'")
        case Some(thid) =>
          msg.to.toSeq.flatten match // Note: toSeq is from the match
            case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
            case tos   =>
              msg.from match
                case None       => Left(s"'$piuri' MUST have field 'from'")
                case Some(from) =>
                  msg.body match
                    case None    => Left(s"'$piuri' MUST have field 'body'")
                    case Some(b) =>
                      b.as[Body].map { body =>
                        Subscription(
                          id = msg.id,
                          to = tos.toSet,
                          from = from,
                          thid = thid,
                          pthid = msg.pthid,
                          created_time = msg.created_time,
                          body = body,
                        )
                      }
  }
}
