package fmgp.did

import zio.json._
import scala.util.chaining._
import zio.json.ast.Json
import zio.json.ast.Json.Obj
import zio.json.ast.Json.Arr
import zio.json.ast.Json.Bool
import zio.json.ast.Json.Str
import zio.json.ast.Json.Num
import zio.json.ast.JsonCursor

/** DIDService
  *
  * https://w3c.github.io/did-core/#service-properties
  *
  * https://w3c.github.io/did-core/#services
  *
  * @param `type`
  *   https://www.w3.org/TR/did-spec-registries/#service-types
  * @param serviceeEndpoint
  *   A string that conforms to the rules of [RFC3986] for URIs, a map, or a set composed of a one or more strings that
  *   conform to the rules of [RFC3986] for URIs and/or maps.
  */
sealed trait DIDService {
  def id: Required[URI]
  def `type`: Required[SetU[String]]
  def serviceEndpoint: Required[ServiceEndpoint]
}

object DIDService {
  val TYPE_DecentralizedWebNode = "DecentralizedWebNode"
  val TYPE_DIDCommMessaging = "DIDCommMessaging"
  val TYPE_LinkedDomains = "LinkedDomains"

  given decoder: JsonDecoder[DIDService] =
    Json.Obj.decoder.mapOrFail { originalAst =>
      originalAst.get(JsonCursor.field("type")) match {
        case Right(Str(`TYPE_DecentralizedWebNode`)) =>
          DIDServiceDecentralizedWebNode.decoder.decodeJson(originalAst.toJson)
        case Right(Str(`TYPE_DIDCommMessaging`)) => DIDServiceDIDCommMessaging.decoder.decodeJson(originalAst.toJson)
        case Right(Str(`TYPE_LinkedDomains`))    => DIDServiceLinkedDomains.decoder.decodeJson(originalAst.toJson)
        case Right(Str(other))                   => DIDServiceGeneric.decoder.decodeJson(originalAst.toJson)
        case Right(value)                        => Left("Field 'type' MUST be a String")
        case Left(value)                         => Left(s"Field 'type' is missing: $value")
      }
    }
  given encoder: JsonEncoder[DIDService] = new JsonEncoder[DIDService] {
    override def unsafeEncode(b: DIDService, indent: Option[Int], out: zio.json.internal.Write): Unit = b match {
      case obj: DIDServiceDecentralizedWebNode => DIDServiceDecentralizedWebNode.encoder.unsafeEncode(obj, indent, out)
      case obj: DIDServiceDIDCommMessaging     => DIDServiceDIDCommMessaging.encoder.unsafeEncode(obj, indent, out)
      case obj: DIDServiceLinkedDomains        => DIDServiceLinkedDomains.encoder.unsafeEncode(obj, indent, out)
      case obj: DIDServiceGeneric              => DIDServiceGeneric.encoder.unsafeEncode(obj, indent, out)
    }
  }
}

/** @see
  *   https://www.w3.org/TR/did-spec-registries/#didcommmessaging
  *
  * @see
  *   https://identity.foundation/didcomm-messaging/spec/#service-endpoint
  */
final case class DIDServiceDIDCommMessaging(
    id: Required[URI],
    private val firstEndpoint: DIDCommMessagingServiceEndpoint,
    private val restEndpoints: DIDCommMessagingServiceEndpoint*,
) extends DIDService {
  override def `type`: Required[SetU[String]] =
    DIDService.TYPE_DIDCommMessaging // OR def `type` = Set(TYPE_DIDCommMessaging)
  def endpoints = firstEndpoint +: restEndpoints

  /** serviceEndpoint MUST not fail! */
  def serviceEndpoint: Required[ServiceEndpointNoStr] =
    if (restEndpoints.isEmpty)
      firstEndpoint.toJsonAST.flatMap(_.as[Json.Obj]).getOrElse(Json.Obj())
    else endpoints.toJsonAST.flatMap(_.as[Json.Arr]).getOrElse(Json.Arr())

  // util used by webapp
  def getServiceEndpointNextForward = endpoints.map(e => e.uri).flatMap {
    case uri @ s"did:$rest" =>
      fmgp.did.comm.FROMTO.either(uri) match
        case Left(_)       => None // uri
        case Right(fromto) => Some(fromto)
    case other => None // other
  }
}

object DIDServiceDIDCommMessaging {
  given decoder: JsonDecoder[DIDServiceDIDCommMessaging] = {
    val DIDCommMessaging = DIDService.TYPE_DIDCommMessaging
    DIDServiceGeneric.decoder.mapOrFail(g =>
      g.`type` match
        case `DIDCommMessaging` =>
          g.serviceEndpoint match {
            case s: Json.Str =>
              Left(s"$DIDCommMessaging the field 'serviceEndpoint' MUST be a json objects or a json array")
            case obj: Json.Obj =>
              DIDCommMessagingServiceEndpoint.decoder
                .fromJsonAST(obj)
                .map(endpoint => DIDServiceDIDCommMessaging(id = g.id, firstEndpoint = endpoint))
            case arr: Json.Arr =>
              arr.elements
                .foldLeft[Either[String, Seq[DIDCommMessagingServiceEndpoint]]](Right(Seq.empty)) {
                  case (left: Left[_, _], elem) => left
                  case (Right(acc), elem) =>
                    elem match {
                      case aux: Json.Obj =>
                        DIDCommMessagingServiceEndpoint.decoder.fromJsonAST(aux).map(newObj => acc :+ newObj)
                      case _ =>
                        Left(
                          s"$DIDCommMessaging the field 'serviceEndpoint' if is json array MUST only contem json objects"
                        )
                    }
                }
                .flatMap {
                  case head +: rest =>
                    Right(DIDServiceDIDCommMessaging(id = g.id, firstEndpoint = head, restEndpoints = rest*))
                  case _ =>
                    Left(
                      s"$DIDCommMessaging the field 'serviceEndpoint' if is json array MUST have at least one object"
                    )
                }
          }
        case otherType => Left(s"Field 'type' MUST be $DIDCommMessaging instead of '$otherType'")
    )
  }

  /** TODO fix the encoder opinionated and remove the array when just one element. Fix method serviceEndpoint */
  given encoder: JsonEncoder[DIDServiceDIDCommMessaging] =
    DIDServiceGeneric.encoder.contramap[DIDServiceDIDCommMessaging](e =>
      DIDServiceGeneric(id = e.id, `type` = e.`type`, serviceEndpoint = e.serviceEndpoint)
    )
}

/** @see
  *   https://github.com/decentralized-identity/didcomm-messaging/commit/74f62edface273985b61793814b58a6a46e63caf
  *
  * @param uri
  *   MUST contain a URI for a transport specified in the [transports] section of this spec, or a URI from Alternative
  *   Endpoints. It MAY be desirable to constraint endpoints from the [transports] section so that they are used only
  *   for the reception of DIDComm messages. This can be particularly helpful in cases where auto-detecting message
  *   types is inefficient or undesirable.
  * @param accept
  *   OPTIONAL. An array of media types in the order of preference for sending a message to the endpoint. These identify
  *   a profile of DIDComm Messaging that the endpoint supports. If accept is not specified, the sender uses its
  *   preferred choice for sending a message to the endpoint. Please see Negotiating Compatibility for details.
  * @param routingKeys
  *   OPTIONAL. An ordered array of strings referencing keys to be used when preparing the message for transmission as
  *   specified in Sender Process to Enable Forwarding, above.
  */
final case class DIDCommMessagingServiceEndpoint(
    uri: URI,
    accept: NotRequired[Set[String]] = None,
    routingKeys: NotRequired[Set[String]] = None,
)

object DIDCommMessagingServiceEndpoint {
  given decoder: JsonDecoder[DIDCommMessagingServiceEndpoint] = DeriveJsonDecoder.gen[DIDCommMessagingServiceEndpoint]
  given encoder: JsonEncoder[DIDCommMessagingServiceEndpoint] = DeriveJsonEncoder.gen[DIDCommMessagingServiceEndpoint]
}

/** DecentralizedWebNode is a type of DIDService
  *
  * @see
  *   https://identity.foundation/decentralized-web-node/spec/#service-endpoints
  *
  * {{{
  * "serviceEndpoint": {"nodes": ["https://dwn.example.com", "https://example.org/dwn"]}
  * }}}
  */
case class DIDServiceDecentralizedWebNode(
    id: Required[URI],
    serviceEndpoint: Required[ServiceEndpoint],
) extends DIDService {
  override def `type`: Required[SetU[String]] = DIDService.TYPE_LinkedDomains

  def getNodes: Seq[String] = serviceEndpoint match
    case Json.Str(str) => Seq.empty
    case Json.Arr(elements) =>
      elements.toSeq.flatMap {
        case obj: Json.Obj =>
          obj.get(JsonCursor.field("nodes")) match
            case Left(_)                   => Seq.empty
            case Right(Json.Arr(elements)) => elements.collect { case Str(v) => v }
            case Right(_)                  => Seq.empty
        case _ => None
      }
    case obj: Json.Obj =>
      obj.get(JsonCursor.field("nodes")) match
        case Left(_)                   => Seq.empty
        case Right(Json.Arr(elements)) => elements.collect { case Str(v) => v }
        case Right(_)                  => Seq.empty

}

object DIDServiceDecentralizedWebNode {
  given decoder: JsonDecoder[DIDServiceDecentralizedWebNode] = DIDServiceGeneric.decoder.mapOrFail { g =>
    val DecentralizedWebNode = DIDService.TYPE_DecentralizedWebNode
    g.`type` match
      case `DecentralizedWebNode` =>
        Right(DIDServiceDecentralizedWebNode(id = g.id, serviceEndpoint = g.serviceEndpoint))
      case otherType => Left(s"Field 'type' MUST be $DecentralizedWebNode instead of '$otherType'")
  }
  given encoder: JsonEncoder[DIDServiceDecentralizedWebNode] =
    DIDServiceGeneric.encoder.contramap[DIDServiceDecentralizedWebNode](e =>
      DIDServiceGeneric(id = e.id, `type` = e.`type`, serviceEndpoint = e.serviceEndpoint)
    )
}

/** https://www.w3.org/TR/did-spec-registries/#linkeddomains */
case class DIDServiceLinkedDomains(
    id: Required[URI],
    serviceEndpoint: Required[ServiceEndpoint],
) extends DIDService {
  override def `type`: String = DIDService.TYPE_LinkedDomains
  // TODO FIX "serviceEndpoint": {"origins": ["https://foo.example.com", "https://identity.foundation"]}
}

object DIDServiceLinkedDomains {
  given decoder: JsonDecoder[DIDServiceLinkedDomains] = DIDServiceGeneric.decoder.mapOrFail { g =>
    val LinkedDomains = DIDService.TYPE_LinkedDomains
    g.`type` match
      case `LinkedDomains` => Right(DIDServiceLinkedDomains(id = g.id, serviceEndpoint = g.serviceEndpoint))
      case otherType       => Left(s"Field 'type' MUST be $LinkedDomains instead of '$otherType'")
  }
  given encoder: JsonEncoder[DIDServiceLinkedDomains] =
    DIDServiceGeneric.encoder.contramap[DIDServiceLinkedDomains](e =>
      DIDServiceGeneric(id = e.id, `type` = e.`type`, serviceEndpoint = e.serviceEndpoint)
    )
}

final case class DIDServiceGeneric(
    id: Required[URI],
    `type`: Required[SetU[String]],
    serviceEndpoint: Required[ServiceEndpoint],
) extends DIDService

object DIDServiceGeneric {
  import SetU.{given}
  import ServiceEndpoint.{given}
  given decoder: JsonDecoder[DIDServiceGeneric] = DeriveJsonDecoder.gen[DIDServiceGeneric]
  given encoder: JsonEncoder[DIDServiceGeneric] = DeriveJsonEncoder.gen[DIDServiceGeneric]
}
