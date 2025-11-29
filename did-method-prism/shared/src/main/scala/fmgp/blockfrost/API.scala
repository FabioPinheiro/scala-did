package fmgp.blockfrost

import zio.json.*
import fmgp.did.method.prism.cardano.CardanoNetwork

case class BlockfrostErrorResponse(
    status_code: Int,
    error: String,
    message: String
) // {"status_code":404,"error":"Not Found","message":"The requested component has not been found."}

object BlockfrostErrorResponse {
  given decoder: JsonDecoder[BlockfrostErrorResponse] = DeriveJsonDecoder.gen[BlockfrostErrorResponse]
  given encoder: JsonEncoder[BlockfrostErrorResponse] = DeriveJsonEncoder.gen[BlockfrostErrorResponse]
}

// case class MetadataLabel(label: String, cip10: Option[String], count: String)
case class MetadataContentJson(tx_hash: String, json_metadata: ast.Json)
case class MetadataContentCBOR(tx_hash: String, metadata: String) //, cbor_metadata: Option[String])

object MetadataContentJson {
  given decoder: JsonDecoder[MetadataContentJson] = DeriveJsonDecoder.gen[MetadataContentJson]
  given encoder: JsonEncoder[MetadataContentJson] = DeriveJsonEncoder.gen[MetadataContentJson]
  given decoderSeqOrError: JsonDecoder[Either[BlockfrostErrorResponse, Seq[MetadataContentJson]]] =
    BlockfrostErrorResponse.decoder.orElseEither(JsonDecoder.seq[MetadataContentJson])
}

object MetadataContentCBOR {
  given decoder: JsonDecoder[MetadataContentCBOR] = DeriveJsonDecoder.gen[MetadataContentCBOR]
  given encoder: JsonEncoder[MetadataContentCBOR] = DeriveJsonEncoder.gen[MetadataContentCBOR]

  given decoderSeqOrError: JsonDecoder[Either[BlockfrostErrorResponse, Seq[MetadataContentCBOR]]] =
    BlockfrostErrorResponse.decoder.orElseEither(JsonDecoder.seq[MetadataContentCBOR])
}

/** Blockfrost API
  *
  * @see
  *   https://docs.blockfrost.io/#tag/cardano--metadata
  */
object API {

  //  ApiResponse[Seq[MetadataLabel]]
  def metadataLabels(network: CardanoNetwork) = s"${network.blockfrostURL}/metadata/txs/labels"

  /** @param label
    *   Metadata label
    * @param page
    *   The page number for listing the results. (min: 1 max: 21474836 default: 1)
    * @param count
    *   The number of results displayed on one page. (min: 1 max: 100 default: 100)
    * @return
    *   MetadataContentJson
    */
  def metadataContentJson(network: CardanoNetwork, label: Int, page: Int, count: Int = 100) =
    s"${network.blockfrostURL}/metadata/txs/labels/$label?page=$page&count=$count&order=asc"

  /** @param label
    *   Metadata label
    * @param page
    *   The page number for listing the results. (min: 1 max: 21474836 default: 1)
    * @param count
    *   The number of results displayed on one page. (min: 1 max: 100 default: 100)
    * @return
    *   MetadataContentCBOR
    */
  def metadataContentCBOR(network: CardanoNetwork, label: Int, page: Int, count: Int = 100) =
    s"${network.blockfrostURL}/metadata/txs/labels/$label/cbor?page=$page&count=$count&order=asc"

  // SortedPageRequest

  // def addressesTotal(network: CardanoNetwork, addresses: String) =
  //   s"${network.blockfrostURL}/addresses/$addresses/total"
}
