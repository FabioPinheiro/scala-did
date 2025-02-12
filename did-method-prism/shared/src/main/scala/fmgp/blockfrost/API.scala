package fmgp.blockfrost

import zio.json._

// case class MetadataLabel(label: String, cip10: Option[String], count: String)
case class MetadataContentJson(tx_hash: String, json_metadata: ast.Json)
case class MetadataContentCBOR(tx_hash: String, metadata: String) //, cbor_metadata: Option[String])

object MetadataContentJson {
  given decoder: JsonDecoder[MetadataContentJson] = DeriveJsonDecoder.gen[MetadataContentJson]
  given encoder: JsonEncoder[MetadataContentJson] = DeriveJsonEncoder.gen[MetadataContentJson]
}

object MetadataContentCBOR {
  given decoder: JsonDecoder[MetadataContentCBOR] = DeriveJsonDecoder.gen[MetadataContentCBOR]
  given encoder: JsonEncoder[MetadataContentCBOR] = DeriveJsonEncoder.gen[MetadataContentCBOR]
}

/** Blockfrost API
  *
  * @see
  *   https://docs.blockfrost.io/#tag/cardano--metadata
  */
object API {

  //  ApiResponse[Seq[MetadataLabel]]
  def metadataLabels(network: String) = s"$network/metadata/txs/labels"

  /** @param label
    *   Metadata label
    * @param page
    *   The page number for listing the results. (min: 1 max: 21474836 default: 1)
    * @param count
    *   The number of results displayed on one page. (min: 1 max: 100 default: 100)
    * @return
    *   MetadataContentJson
    */
  def metadataContentJson(network: String, label: String, page: Int, count: Int = 100) =
    s"$network/metadata/txs/labels/$label?page=$page&count=$count&order=asc"

  /** @param label
    *   Metadata label
    * @param page
    *   The page number for listing the results. (min: 1 max: 21474836 default: 1)
    * @param count
    *   The number of results displayed on one page. (min: 1 max: 100 default: 100)
    * @return
    *   MetadataContentCBOR
    */
  def metadataContentCBOR(network: String, label: String, page: Int, count: Int = 100) =
    s"$network/metadata/txs/labels/$label/cbor?page=$page&count=$count&order=asc"

  // SortedPageRequest

}
