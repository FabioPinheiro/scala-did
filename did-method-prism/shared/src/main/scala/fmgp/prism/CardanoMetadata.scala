package fmgp.prism

import zio.json._

case class CardanoMetadata(index: Int, tx: String, content: ast.Json) extends PrismBlockIndex {
  def b = index
  def toCardanoPrismEntry: Either[String, CardanoPrismEntry] =
    CardanoTransactionMetadataPrismContent
      .fromJson(content.toJson)
      .map(prismObject => CardanoPrismEntry(prismBlockIndex, tx, prismObject))
}
object CardanoMetadata {
  given decoder: JsonDecoder[CardanoMetadata] = DeriveJsonDecoder.gen[CardanoMetadata]
  given encoder: JsonEncoder[CardanoMetadata] = DeriveJsonEncoder.gen[CardanoMetadata]
}
