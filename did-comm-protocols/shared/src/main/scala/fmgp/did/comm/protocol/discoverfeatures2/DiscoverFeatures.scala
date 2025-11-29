package fmgp.did.comm.protocol.discoverfeatures2

import zio.json.*

import fmgp.did.*
import fmgp.did.comm.*

/** @see
  *   https://didcomm.org/discover-features/2.0/
  */
extension (msg: PlaintextMessage)
  def toFeatureQuery: Either[String, FeatureQuery] =
    FeatureQuery.fromPlaintextMessage(msg)
  def toFeatureDisclose: Either[String, FeatureDisclose] =
    FeatureDisclose.fromPlaintextMessage(msg)

/** {{{
  * {
  *   "type": "https://didcomm.org/discover-features/2.0/queries",
  *   "id": "yWd8wfYzhmuXX3hmLNaV5bVbAjbWaU",
  *   "body": {
  *     "queries": [
  *       { "feature-type": "protocol", "match": "https://didcomm.org/tictactoe/1.*" },
  *       { "feature-type": "goal-code", "match": "org.didcomm.*" }
  *     ]
  *   }
  * }
  * }}}
  *
  * @param id
  * @param to
  * @param from
  */
case class FeatureQuery(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: FROM,
    queries: Seq[FeatureQuery.Query],
) {
  def piuri = FeatureQuery.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(to),
      from = Some(from),
      body = Some(FeatureQuery.Body(queries = queries).toJSON_RFC7159),
    )
}

object FeatureQuery {
  def piuri = PIURI("https://didcomm.org/discover-features/2.0/queries")

  protected final case class Body(queries: Seq[Query]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  final case class Query(`feature-type`: String, `match`: String)
  object Query {
    given decoder: JsonDecoder[Query] = DeriveJsonDecoder.gen[Query]
    given encoder: JsonEncoder[Query] = DeriveJsonEncoder.gen[Query]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, FeatureQuery] = {
    if (msg.`type` != piuri)
      Left(s"No able to create FeatureQuery from a Message of the type '${msg.`type`}'")
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
                    FeatureQuery(
                      id = msg.id,
                      to = tos.toSet,
                      from = from,
                      queries = body.queries,
                    )
                  }
  }

}

/** {{{
  * {
  *   "type": "https://didcomm.org/discover-features/2.0/disclose",
  *   "thid": "yWd8wfYzhmuXX3hmLNaV5bVbAjbWaU",
  *   "body":{
  *     "disclosures": [
  *       {
  *         "feature-type": "protocol",
  *         "id": "https://didcomm.org/tictactoe/1.0",
  *         "roles": ["player"]
  *       },
  *       {
  *         "feature-type": "goal-code",
  *         "id": "org.didcomm.sell.goods.consumer"
  *       }
  *     ]
  *   }
  * }
  * }}}
  *
  * @param id
  * @param to
  * @param from
  */
case class FeatureDisclose(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: FROM,
    thid: Option[MsgID],
    disclosures: Seq[FeatureDisclose.Disclose],
) {
  def piuri = FeatureDisclose.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(to),
      from = Some(from),
      thid = thid,
      body = Some(FeatureDisclose.Body(disclosures = disclosures).toJSON_RFC7159),
    )
}

object FeatureDisclose {
  def piuri = PIURI("https://didcomm.org/discover-features/2.0/disclose")

  protected final case class Body(disclosures: Seq[Disclose]) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  final case class Disclose(`feature-type`: String, id: String, roles: Option[Seq[String]] = None)
  object Disclose {
    given decoder: JsonDecoder[Disclose] = DeriveJsonDecoder.gen[Disclose]
    given encoder: JsonEncoder[Disclose] = DeriveJsonEncoder.gen[Disclose]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, FeatureDisclose] = {
    if (msg.`type` != piuri)
      Left(s"No able to create FeatureDisclose from a Message of the type '${msg.`type`}'")
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
                    FeatureDisclose(
                      id = msg.id,
                      to = tos.toSet,
                      from = from,
                      thid = msg.thid,
                      disclosures = body.disclosures,
                    )
                  }
  }
}
