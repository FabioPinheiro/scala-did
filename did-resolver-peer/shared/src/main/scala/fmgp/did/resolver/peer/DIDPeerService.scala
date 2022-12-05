package fmgp.did.resolver.peer

import zio.json._
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
    r: Seq[String] = Seq.empty,
    a: Seq[String] = Seq("didcomm/v2")
) {
  def `type` = t
  def serviceEndpoint = s
  def routingKeys = r
  def accept = a

  def getDIDService(id: DIDSubject): DIDService = DIDServiceGeneric(
    id = id.string + "#didcommmessaging-0",
    `type` = if (this.t == "dm") "DIDCommMessaging" else this.t,
    serviceEndpoint = this.s,
    routingKeys = Some(this.r.toSet).filterNot(_.isEmpty),
    accept = Some(this.a.toSet).filterNot(_.isEmpty),
  )
}

object DIDPeerServiceEncoded {
  given decoder: JsonDecoder[DIDPeerServiceEncoded] = DeriveJsonDecoder.gen[DIDPeerServiceEncoded]
  given encoder: JsonEncoder[DIDPeerServiceEncoded] = DeriveJsonEncoder.gen[DIDPeerServiceEncoded]
  def apply(endpoint: String) = new DIDPeerServiceEncoded(s = endpoint)
}