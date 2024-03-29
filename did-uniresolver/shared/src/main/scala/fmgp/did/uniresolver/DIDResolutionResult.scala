package fmgp.did.uniresolver

import zio.json._
import zio.json.ast.Json
import fmgp.did.DIDDocument

/** https://w3c-ccg.github.io/did-resolution/#did-resolution-result
  *
  * https://github.com/decentralized-identity/universal-resolver/
  */
case class DIDResolutionResult(
    `@context`: String = "https://w3id.org/did-resolution/v1",
    didDocument: DIDDocument,
    didResolutionMetadata: Json,
    didDocumentMetadata: Json
)

object DIDResolutionResult {
  given encoder: JsonEncoder[DIDResolutionResult] = DeriveJsonEncoder.gen[DIDResolutionResult]
  given decoder: JsonDecoder[DIDResolutionResult] = DeriveJsonDecoder.gen[DIDResolutionResult]
}
