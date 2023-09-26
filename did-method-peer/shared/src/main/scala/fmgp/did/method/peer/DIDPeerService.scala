package fmgp.did.method.peer

import zio.json._
import zio.json.ast.Json
import fmgp.did._
import scala.util.chaining._

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
case class DIDPeerServiceEncoded(
    t: String = "dm",
    s: String,
    r: Option[Seq[String]] = Some( // TODO use None. This was the support some integration problem on other libraries
      Seq.empty
    ),
    a: Option[Seq[String]] = Some(Seq("didcomm/v2"))
) {
  def `type` = t
  def serviceEndpoint = s
  def routingKeys = r
  def accept = a

  def getDIDService(id: DIDSubject, index: Int): DIDService =
    if (this.t == "dm" || this.t == DIDService.TYPE_DIDCommMessaging)
      DIDServiceDIDCommMessaging(
        id = s"${id.string}#didcommmessaging-$index",
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

object DIDPeerServiceEncoded {
  given decoder: JsonDecoder[DIDPeerServiceEncoded] = DeriveJsonDecoder.gen[DIDPeerServiceEncoded]
  given encoder: JsonEncoder[DIDPeerServiceEncoded] = DeriveJsonEncoder.gen[DIDPeerServiceEncoded]
  def apply(endpoint: String): DIDPeerServiceEncoded = new DIDPeerServiceEncoded(s = endpoint)
  def apply(did: DID): DIDPeerServiceEncoded = new DIDPeerServiceEncoded(s = did.string)
}
