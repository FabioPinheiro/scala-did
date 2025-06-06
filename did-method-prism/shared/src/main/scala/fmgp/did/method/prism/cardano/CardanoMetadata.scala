package fmgp.did.method.prism.cardano

import zio.json._
import io.bullet.borer.Decoder
import io.bullet.borer.Cbor

trait CardanoMetadata extends PrismBlockIndex {
  def index: Int
  def b = index
  def tx: String
  def toCardanoPrismEntry: Either[String, CardanoPrismEntry]
}

case class CardanoMetadataJson(index: Int, tx: String, content: ast.Json) extends CardanoMetadata {

  def toCardanoPrismEntry: Either[String, CardanoPrismEntry] =
    CardanoTransactionMetadataPrismContent
      .fromJson(content.toJson)
      .map(prismObject => CardanoPrismEntry(prismBlockIndex, tx, prismObject))
}
object CardanoMetadataJson {
  given decoder: JsonDecoder[CardanoMetadataJson] = DeriveJsonDecoder.gen[CardanoMetadataJson]
  given encoder: JsonEncoder[CardanoMetadataJson] = DeriveJsonEncoder.gen[CardanoMetadataJson]
}

case class CardanoMetadataCBOR(index: Int, tx: String, cbor: String) extends CardanoMetadata {
  def contentBytes = fmgp.util.hex2bytes(cbor)
  def contentCBOR = Cbor.decode(contentBytes)

  def toCardanoPrismEntry: Either[String, CardanoPrismEntry] =
    contentCBOR.to[CardanoTransactionMetadataPrismCBOR].valueEither match
      case Left(error)              => Left(error.getMessage)
      case Right(metadataPrismCBOR) => Right(CardanoPrismEntry(prismBlockIndex, tx, metadataPrismCBOR.toPrismObject))

}

object CardanoMetadataCBOR {
  given decoder: JsonDecoder[CardanoMetadataCBOR] = DeriveJsonDecoder.gen[CardanoMetadataCBOR]
  given encoder: JsonEncoder[CardanoMetadataCBOR] = DeriveJsonEncoder.gen[CardanoMetadataCBOR]
}
