package fmgp.did.method.peer

import zio.json._
import zio.json.ast.Json
import fmgp.did._
import scala.util.chaining._
import fmgp.util.Base64
import fmgp.did.comm.Payload.content
import zio.json.ast.Json.Obj
import zio.json.ast.Json.Arr
import zio.json.ast.Json.Str
import zio.json.ast.JsonCursor

// /** https://identity.foundation/peer-did-method-spec/#multi-key-creation */
// final case class DIDPeerService(
//     id: Required[URI],
//     `type`: Required[SetU[String]],
//     serviceEndpoint: Required[SetU[URI]],
//     routingKeys: Set[String],
//     accept: Set[String],
// ) extends DIDService {
//   // val (namespace, specificId) = DID.getNamespaceAndSpecificId(id)
//   val (namespace, specificId) = DIDSubject(id).pipe(did => (did.namespace, did.specificId))
// }

// object DIDPeerService {
//   import SetU.{given}
//   given decoder: JsonDecoder[DIDPeerService] = DeriveJsonDecoder.gen[DIDPeerService]
//   given encoder: JsonEncoder[DIDPeerService] = DeriveJsonEncoder.gen[DIDPeerService]
// }

sealed trait DIDPeerServiceEncoded {
  def getDIDService(id: DIDSubject, previouslyNumberOfService: Int): Seq[DIDService]
}

object DIDPeerServiceEncoded {
  def makeOldFormat(s: String): DIDPeerServiceEncoded = new DIDPeerServiceEncodedOld(s = s)
  def fromMediator(did: DID): DIDPeerServiceEncoded = fromEndpoint(did.string)
  def fromEndpoint(endpoint: String): DIDPeerServiceEncoded = new DIDPeerServiceEncodedNew(
    Base64.encode(s"""{"t":"dm","s":{"uri":"$endpoint","a":["didcomm/v2"]}}""")
  )

  given decoder: JsonDecoder[DIDPeerServiceEncoded] = DIDPeerServiceEncodedNew.decoder.widen
  given encoder: JsonEncoder[DIDPeerServiceEncoded] = new JsonEncoder[DIDPeerServiceEncoded] {
    override def unsafeEncode(b: DIDPeerServiceEncoded, indent: Option[Int], out: zio.json.internal.Write): Unit =
      b match {
        case obj: DIDPeerServiceEncodedNew => DIDPeerServiceEncodedNew.encoder.unsafeEncode(obj, indent, out)
        case obj: DIDPeerServiceEncodedOld => DIDPeerServiceEncodedOld.encoder.unsafeEncode(obj, indent, out)
      }
  }

  def abbreviation(str: String): String = str match
    case "t"      => "type"
    case "s"      => "serviceEndpoint"
    case "r"      => "routingKeys"
    case "a"      => "accept"
    case "dm"     => "DIDCommMessaging"
    case identity => identity // identity funtions

  val abbreviationPartial: PartialFunction[Json, Json] = {
    case Obj(fields) => Obj(fields.map { case (name, value) => (abbreviation(name), value) })
    case Str(value)  => Str(abbreviation(value))
  }

  def abbreviation(json: Json): Json = json.transformDownSome(abbreviationPartial)
}

case class DIDPeerServiceEncodedNew(base64: Base64) extends DIDPeerServiceEncoded {
// Json.decoder.decodeJson(base64.decodeToString)

  def readService(json: Json.Obj, didSubject: DIDSubject, index: Int): Either[String, DIDService] = {
    val DIDCommMessaging = DIDService.TYPE_DIDCommMessaging
    def serviceId(typeName: String) = {
      s"${didSubject.string}#" + {
        if (typeName.equalsIgnoreCase(DIDCommMessaging)) "service"
        else typeName.toLowerCase().replace(" ", "")
      } + { if (index == 0) "" else s"-$index" }
    }

    json.fields.collectFirst {
      case ("type", value) => value
      case ("t", value)    => value // see abbreviation
    } match
      case None => Left("Field 'type' MUST exist")
      case Some(Json.Str(`DIDCommMessaging`)) =>
        json.fields.collectFirst {
          case ("serviceEndpoint", value) => value
          case ("s", value)               => value // see abbreviation
        } match
          case None => Left("The field 'serviceEndpoint' MUST exist")
          case Some(Json.Str(uri)) => // old format
            Right(
              DIDServiceDIDCommMessaging(
                id = serviceId(`DIDCommMessaging`),
                DIDCommMessagingServiceEndpoint(
                  uri = uri,
                  accept = json.fields
                    .collectFirst {
                      case ("accept", value) => value
                      case ("a", value)      => value // see abbreviation
                    }
                    .flatMap { _.as[Set[String]].toOption },
                  routingKeys = json.fields
                    .collectFirst {
                      case ("routingKeys", value) => value
                      case ("r", value)           => value // see abbreviation
                    }
                    .flatMap { _.as[Set[String]].toOption },
                )
              )
            )
          case Some(Json.Obj(_)) => // new format
            Obj(json.fields :+ ("id", Json.Str(serviceId(`DIDCommMessaging`)))).as[DIDService] match {
              case Left(error)       => Left(s"DIDPeerServiceEncoded fail to parse service: $error")
              case Right(newService) => Right(newService)
            }
          case Some(anyJson) => Left("Field 'serviceEndpoint' MUST be a json object or string")
      case Some(Json.Str(otherType)) => Obj(json.fields :+ ("id", Json.Str(serviceId(otherType)))).as[DIDService]
      case Some(anyJson)             => Left("Field 'type' MUST be a json string")
  }

  def getDIDService(didSubject: DIDSubject, previouslyNumberOfService: Int): Seq[DIDService] = {
    base64.decodeToString.fromJson[Json] match
      case Left(error) => Seq() // TODO ERROR
      case Right(json) =>
        DIDPeerServiceEncoded
          .abbreviation(json) match {
          case obj: Json.Obj =>
            readService(obj, didSubject, index = previouslyNumberOfService).toOption.toSeq
          case Arr(elements) =>
            elements.zipWithIndex.foldLeft[Seq[fmgp.did.DIDService]](Seq.empty) {
              case (services, (obj: Json.Obj, index)) =>
                services ++ readService(obj, didSubject, previouslyNumberOfService + index).toOption
              case (services, (anyJson, index)) => services
            }
          case _ => Seq()
        }
  }
}
object DIDPeerServiceEncodedNew {
  given decoder: JsonDecoder[DIDPeerServiceEncodedNew] =
    Base64.decoder.map(DIDPeerServiceEncodedNew(_))
  given encoder: JsonEncoder[DIDPeerServiceEncodedNew] =
    Base64.encoder.contramap[DIDPeerServiceEncodedNew] { _.base64 }
}

/** PeerDid Service Endpoint
  * @see
  *   https://identity.foundation/peer-did-method-spec/#multi-key-creation
  *
  * @param t
  *   type - the value 'dm' means DIDCommMessaging
  * @param s
  *   serviceEndpoint
  * @param r
  *   routingKeys (OPTIONAL)
  * @param a
  *   accept
  */
case class DIDPeerServiceEncodedOld(
    t: String = "dm",
    s: String,
    r: Option[Seq[String]] = Some( // TODO use None. This was the support some integration problem on other libraries
      Seq.empty
    ),
    a: Option[Seq[String]] = Some(Seq("didcomm/v2"))
) extends DIDPeerServiceEncoded {
  def `type` = t
  def serviceEndpoint = s
  def routingKeys = r
  def accept = a

  def getDIDService(id: DIDSubject, previouslyNumberOfService: Int): Seq[DIDService] =
    Seq(getDIDServiceAux(id = id, index = previouslyNumberOfService))

  def getDIDServiceAux(id: DIDSubject, index: Int): DIDService =
    if (this.t == "dm" || this.t == DIDService.TYPE_DIDCommMessaging)
      DIDServiceDIDCommMessaging(
        // before the id was s"${id.string}#didcommmessaging-$index"
        id = if (index == 0) s"${id.string}#service" else s"${id.string}#service-$index",
        DIDCommMessagingServiceEndpoint(
          uri = this.s,
          routingKeys = Some(this.r.toSet.flatten).filterNot(_.isEmpty),
          accept = Some(this.a.toSet.flatten).filterNot(_.isEmpty),
        )
      )
    else
      DIDServiceGeneric(
        id = id.string + "#" + this.t.toLowerCase() + "-" + index,
        `type` = this.t,
        serviceEndpoint = Json.Str(this.s),
      )
}

object DIDPeerServiceEncodedOld {
  given decoder: JsonDecoder[DIDPeerServiceEncodedOld] = DeriveJsonDecoder.gen[DIDPeerServiceEncodedOld]
  given encoder: JsonEncoder[DIDPeerServiceEncodedOld] = DeriveJsonEncoder.gen[DIDPeerServiceEncodedOld]
  def apply(endpoint: String): DIDPeerServiceEncodedOld = new DIDPeerServiceEncodedOld(s = endpoint)
  def apply(did: DID): DIDPeerServiceEncodedOld = new DIDPeerServiceEncodedOld(s = did.string)
}
