package fmgp.did.method.prism.cardano

import zio.json._
import proto.prism.PrismObject
import io.bullet.borer.Decoder

case class CardanoTransactionMetadataPrismContent(c: Seq[String], v: Int)
object CardanoTransactionMetadataPrismContent {
  given decoder: JsonDecoder[CardanoTransactionMetadataPrismContent] =
    DeriveJsonDecoder.gen[CardanoTransactionMetadataPrismContent]
  given encoder: JsonEncoder[CardanoTransactionMetadataPrismContent] =
    DeriveJsonEncoder.gen[CardanoTransactionMetadataPrismContent]
  def fromJson(content: String) = {
    content.fromJson[CardanoTransactionMetadataPrismContent] match
      case Left(error) => Left(s"Fail to parse CardanoTransactionMetadataPrismContent: $error")
      case Right(value) if value.c.forall(_.startsWith("0x")) =>
        val hex = value.c.map(_.drop(2)).mkString
        val bytes = hex.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toArray
        Right(PrismObject.parseFrom(bytes))
      case Right(value) =>
        Left(
          "PRISM Block must the encode in ByteString - https://github.com/input-output-hk/prism-did-method-spec/issues/66"
        )

  }
}

case class CardanoTransactionMetadataPrismCBOR(version: Int, protoBytes: Array[Byte]) {
  def toPrismObject = PrismObject.parseFrom(protoBytes)
}
object CardanoTransactionMetadataPrismCBOR {
  given Decoder[CardanoTransactionMetadataPrismCBOR] = Decoder { reader =>
    if (reader.hasMapHeader) {
      reader.readMapHeader(1)
      if (reader.hasInt & reader.readInt() == 21325) {
        if (reader.hasMapHeader(2)) {
          val readerPrismStr = reader.readMapHeader(2)
          reader.readString() match
            case "v" => {
              val version = reader.readInt() // PRISM version
              if (reader.hasString & (reader.readString() == "c"))
                val protoBytes = reader.read[Array[Array[Byte]]]().flatten // read protobuf bytes!
                CardanoTransactionMetadataPrismCBOR(version = version, protoBytes = protoBytes)
              else reader.unexpectedDataItem(expected = "the map key 'c'")
            }
            case "c" => {
              val protoBytes = reader.read[Array[Array[Byte]]]().flatten // read protobuf bytes!
              if (reader.hasString & (reader.readString() == "v")) {
                val version = reader.readInt() // PRISM version'
                CardanoTransactionMetadataPrismCBOR(version = version, protoBytes = protoBytes)
              } else reader.unexpectedDataItem(expected = "the map key 'v'")
            }
            case _ => reader.unexpectedDataItem("the map key 'v' or 'c'")
        } else reader.unexpectedDataItem(expected = "`MapHeader` with the PRISM structure")
      } else reader.unexpectedDataItem(expected = "`Int` with value 21325")
    } else reader.unexpectedDataItem(expected = "Cardano Metadata MUST start with a MapHeader with the label")
  }
}
