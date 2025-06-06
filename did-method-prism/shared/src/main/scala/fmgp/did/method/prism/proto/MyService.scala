package fmgp.did.method.prism.proto

import zio.json._
import proto.prism.Service

// TODO Rename
case class MyService(
    id: String, // (1)
    `type`: String, // (2)
    serviceEndpoint: String, // (3)
) {
  import fmgp.did._
  def toDIDService =
    DIDServiceGeneric(
      id = id,
      `type` = `type`,
      serviceEndpoint = ast.Json.Str(serviceEndpoint): ServiceEndpoint
    )
}
object MyService {
  given decoder: JsonDecoder[MyService] = DeriveJsonDecoder.gen[MyService]
  given encoder: JsonEncoder[MyService] = DeriveJsonEncoder.gen[MyService]
  def fromProto(service: Service) =
    service match
      case Service(id, type_, serviceEndpoint, unknownFields) =>
        MyService(id = id, `type` = type_, serviceEndpoint = serviceEndpoint)
}
