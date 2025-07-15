package fmgp.did.method.prism.proto

import zio._
import zio.json._

import fmgp.crypto.SHA256

import proto.prism.PrismOperation
import proto.prism.PrismObject
import proto.prism.SignedPrismOperation
import fmgp.util.safeValueOf
import fmgp.util.bytes2Hex
import fmgp.did.method.prism._
import fmgp.did.method.prism.cardano.{PrismBlockIndex, PrismOperationIndex, EventCursor}

// TODO Rename to MaybeEvent
sealed trait MaybeOperation[+T <: OP] // sealed abstract class MaybeOperation[+T <: OP]

case class InvalidPrismObject(
    tx: String,
    b: Int,
    reason: String,
) extends MaybeOperation[Nothing]
    with PrismBlockIndex

case class InvalidSignedPrismOperation(
    tx: String,
    b: Int,
    o: Int,
    reason: String,
) extends MaybeOperation[Nothing]
    with PrismOperationIndex

case class MySignedPrismOperation[+T <: OP](
    tx: String,
    b: Int,
    o: Int,
    signedWith: String,
    signature: Array[Byte],
    operation: T, // TODO REMOVE when encoding
    protobuf: PrismOperation
) extends MaybeOperation[T]
    with PrismOperationIndex {
  def opHash = protobuf.eventHashStr // TODO REMOVE method
  def eventHash: EventHash = protobuf.getEventHash
  def eventRef = EventRef(b = b, o = o, eventHash = protobuf.getEventHash)
  def eventCursor = EventCursor(b = b, o = o)
}

// TODO replace MySignedPrismOperation
// case class CompresedSignedPrismOperation[+T <: OP](
//     tx: String,
//     b: Int,
//     o: Int,
//     signedWith: String,
//     signature: Array[Byte],
//     protobuf: PrismOperation,
// ) extends MaybeOperation[OP]
//     with PrismOperationIndex {
//   lazy val operation: T = OP.fromPrismOperation(protobuf).asInstanceOf[T] //FIXME
// }

object InvalidPrismObject {
  given decoder: JsonDecoder[InvalidPrismObject] = DeriveJsonDecoder.gen[InvalidPrismObject]
  given encoder: JsonEncoder[InvalidPrismObject] = DeriveJsonEncoder.gen[InvalidPrismObject]
}
object InvalidSignedPrismOperation {
  given decoder: JsonDecoder[InvalidSignedPrismOperation] = DeriveJsonDecoder.gen[InvalidSignedPrismOperation]
  given encoder: JsonEncoder[InvalidSignedPrismOperation] = DeriveJsonEncoder.gen[InvalidSignedPrismOperation]
}

object MySignedPrismOperation {

  given decoder: JsonDecoder[MySignedPrismOperation[OP]] = {
    import fmgp.util.decoderByteArray
    given decoderPrismOperation: JsonDecoder[PrismOperation] = // use mapOrFail
      decoderByteArray.map(e => PrismOperation.parseFrom(e)) // FIXME catch exceptions
    DeriveJsonDecoder.gen[MySignedPrismOperation[OP]]
  }
  given encoder: JsonEncoder[MySignedPrismOperation[OP]] = {
    import fmgp.util.encoderByteArray
    given encoderPrismOperation: JsonEncoder[PrismOperation] =
      encoderByteArray.contramap((e: PrismOperation) => e.toByteArray)
    DeriveJsonEncoder.gen[MySignedPrismOperation[OP]]
  }

  given encoderTypeDidEvent: JsonEncoder[MySignedPrismOperation[OP.TypeDidEvent]] =
    encoder.contramap(e => e)
  given encoderTypeStorageEntryEvent: JsonEncoder[MySignedPrismOperation[OP.TypeStorageEntryEvent]] =
    encoder.contramap(e => e)

}

object MaybeOperation {

  given decoder: JsonDecoder[MaybeOperation[OP]] =
    MySignedPrismOperation.decoder
      .widen[MaybeOperation[OP]]
      .orElseAndWarpErrors(InvalidSignedPrismOperation.decoder.widen[MaybeOperation[OP]])
      .orElseAndWarpErrors(InvalidPrismObject.decoder.widen[MaybeOperation[OP]])

  given encoder: JsonEncoder[MaybeOperation[OP]] = new JsonEncoder[MaybeOperation[OP]] {
    override def unsafeEncode(b: MaybeOperation[OP], indent: Option[Int], out: zio.json.internal.Write): Unit = {
      b match {
        case obj: MySignedPrismOperation[OP]  => MySignedPrismOperation.encoder.unsafeEncode(obj, indent, out)
        case obj: InvalidSignedPrismOperation => InvalidSignedPrismOperation.encoder.unsafeEncode(obj, indent, out)
        case obj: InvalidPrismObject          => InvalidPrismObject.encoder.unsafeEncode(obj, indent, out)
      }
    }
  }

  def fromProto(
      prismObject: PrismObject,
      tx: String,
      blockIndex: Int,
  ): Seq[MaybeOperation[OP]] =
    prismObject.blockContent match
      case None => Seq(InvalidPrismObject(tx = tx, b = blockIndex, reason = "blockContent is missing"))
      case Some(prismBlock) =>
        prismBlock.operations.zipWithIndex.map { case (signedPrismOperation, opIndex) =>
          fromProto(signedPrismOperation = signedPrismOperation, tx = tx, blockIndex = blockIndex, opIndex = opIndex)
        }

  def fromProto(
      signedPrismOperation: SignedPrismOperation,
      tx: String,
      blockIndex: Int,
      opIndex: Int,
  ): MaybeOperation[OP] =
    signedPrismOperation match
      case SignedPrismOperation(signedWith, signature, operation, unknownFields) =>
        assert(unknownFields.serializedSize == 0, "SignedPrismOperation have unknownFields")
        operation match
          case None => InvalidSignedPrismOperation(tx = tx, b = blockIndex, o = opIndex, "operation is missing")
          case Some(prismOperation) =>
            MySignedPrismOperation(
              tx = tx,
              b = blockIndex,
              o = opIndex,
              signedWith = signedWith,
              signature = signature.toByteArray(),
              operation = OP.fromPrismOperation(prismOperation),
              protobuf = prismOperation,
            )
}
