package fmgp.prism

import zio.json._
import proto.prism.PrismObject

case class CardanoTransactionMetadataPrismContent(c: Seq[String], v: Int)
object CardanoTransactionMetadataPrismContent {
  given decoder: JsonDecoder[CardanoTransactionMetadataPrismContent] =
    DeriveJsonDecoder.gen[CardanoTransactionMetadataPrismContent]
  given encoder: JsonEncoder[CardanoTransactionMetadataPrismContent] =
    DeriveJsonEncoder.gen[CardanoTransactionMetadataPrismContent]
  def fromJson(content: String) = {
    content.fromJson[CardanoTransactionMetadataPrismContent] match
      case Left(error)  => Left(s"Fail to parse CardanoTransactionMetadataPrismContent: $error")
      case Right(value) =>
        // Unclear encoding in the transaction metadata '0x' https://github.com/input-output-hk/prism-did-method-spec/issues/66
        val hex = if (value.c.forall(_.startsWith("0x"))) value.c.map(_.drop(2)).mkString else value.c.mkString
        val bytes = hex.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toArray
        Right(PrismObject.parseFrom(bytes))
  }
}
