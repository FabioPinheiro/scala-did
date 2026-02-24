package fmgp.did.method.prism.proto

import zio.json.*
import proto.prism.Service
import zio.json.ast.Json

// TODO Rename PrismDidService
case class MyService(
    id: String, // (1)
    `type`: String, // (2)
    serviceEndpoint: String, // (3)
) {
  import fmgp.did.*
  def toDIDService =
    DIDServiceGeneric(
      id = id,
      `type` = `type`,
      serviceEndpoint = MyService.decodeProtoServiceEndpoint(serviceEndpoint)
    )
  def toProto: Service = Service(id = id, `type` = `type`, serviceEndpoint = serviceEndpoint)
}
object MyService {
  // https://www.w3.org/TR/did-extensions-properties/#service-types
  val DIDCOMM = "DIDCommMessaging"

  given decoder: JsonDecoder[MyService] = DeriveJsonDecoder.gen[MyService]
  given encoder: JsonEncoder[MyService] = DeriveJsonEncoder.gen[MyService]
  def fromProto(service: Service) =
    service match
      case Service(id, type_, serviceEndpoint, unknownFields) =>
        MyService(id = id, `type` = type_, serviceEndpoint = serviceEndpoint)

  def encodeProtoServiceEndpoint(json: fmgp.did.ServiceEndpoint): String =
    json match
      case o: ast.Json.Obj => o.toJson
      case o: ast.Json.Arr => o.toJson
      case o: ast.Json.Str => o.toJson

  /** Fix for https://github.com/FabioPinheiro/scala-did/issues/558 */
  def decodeProtoServiceEndpoint(strOrJsonSTR: String): fmgp.did.ServiceEndpoint /* Json.Str | Json.Obj | Json.Arr */ =
    strOrJsonSTR
      .fromJson[ast.Json.Str] // TRY json string
      .getOrElse {
        strOrJsonSTR
          .fromJson[ast.Json.Arr] // TRY array of strings
          .flatMap { arr =>
            val test: Either[String, Seq[String]] =
              arr.elements.toSeq.foldLeft[Either[String, Seq[String]]](Right(Seq.empty)) {
                case (Right(acc), item) =>
                  item.asString match
                    case Some(itemAsString) => Right(acc :+ itemAsString)
                    case None               => Left(s"Only support Arr of string ('$item' is not a string)")
                case (Left(error), _) => Left(error)
              }
            test match
              case Left(value)  => Left(value) // ERROR
              case Right(value) => Right(arr) // return Json.Arr
          }
          .getOrElse {
            strOrJsonSTR
              .fromJson[ast.Json.Obj] // TRY json Obj
              .getOrElse {
                ast.Json.Str(strOrJsonSTR) // if its not encoded json then is simple string
              }
          }
      }

  /** @see
    *   https://identity.foundation/didcomm-messaging/spec/v2.1/#using-a-did-as-an-endpoint
    * @see
    *   https://www.w3.org/TR/did-extensions-properties/#didcommmessaging
    */
  def didCommSimpleEndpoint(id: String, simpleEndpoint: String) =
    MyService(
      id = id,
      `type` = DIDCOMM,
      serviceEndpoint = encodeProtoServiceEndpoint(
        ast.Json.Obj("uri" -> ast.Json.Str(simpleEndpoint))
      )
    )
}
