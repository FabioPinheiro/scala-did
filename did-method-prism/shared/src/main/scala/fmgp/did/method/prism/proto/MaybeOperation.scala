package fmgp.did.method.prism.proto

import zio._
import zio.json._

import fmgp.crypto.SHA256

import proto.prism.PrismEvent
import proto.prism.PrismObject
import proto.prism.SignedPrismEvent
import fmgp.util.safeValueOf
import fmgp.util.bytes2Hex
import fmgp.did.method.prism._
import fmgp.did.method.prism.cardano.{PrismBlockIndex, PrismEventIndex, EventCursor}

// TODO Rename to MaybeEvent
sealed trait MaybeEvent[+T <: OP] // sealed abstract class MaybeEvent[+T <: OP]

case class InvalidPrismObject(
    tx: String,
    b: Int,
    reason: String,
) extends MaybeEvent[Nothing]
    with PrismBlockIndex

case class InvalidSignedPrismEvent(
    tx: String,
    b: Int,
    o: Int,
    reason: String,
) extends MaybeEvent[Nothing]
    with PrismEventIndex

case class MySignedPrismEvent[+T <: OP] private (
    tx: String,
    b: Int,
    o: Int,
    signedWith: String,
    signature: Array[Byte],
    protobuf: PrismEvent
) extends MaybeEvent[T] // SignedPrismEventTrait[T]
    with PrismEventIndex {
  def event: T = operation // TODO REMOVE
  def opHash = protobuf.eventHashStr // TODO REMOVE method
  def eventHash: EventHash = protobuf.getEventHash
  def eventRef = EventRef(b = b, o = o, eventHash = protobuf.getEventHash)
  def eventCursor = EventCursor(b = b, o = o)
  val operation: T = OP.fromPrismEvent(protobuf).asInstanceOf[T] // UNSAFE? //TODO maybe make it lazy val
  def view = SignedPrismEventView[T](tx, b, o, signedWith, signature, operation, protobuf)

  def asSignedPrismDIDEvent: Either[String, MySignedPrismEvent[OP.TypeDIDEvent]] =
    event.asDIDEvent.map(_ => this.asInstanceOf[MySignedPrismEvent[OP.TypeDIDEvent]])
  def asSignedPrismStorageEntryEvent: Either[String, MySignedPrismEvent[OP.TypeStorageEntryEvent]] =
    event.asDIDEvent.map(_ => this.asInstanceOf[MySignedPrismEvent[OP.TypeStorageEntryEvent]])
}

case class SignedPrismEventView[+T <: OP](
    tx: String,
    b: Int,
    o: Int,
    signedWith: String,
    signature: Array[Byte],
    operation: OP,
    protobuf: PrismEvent
) //extends SignedPrismEventTrait

object SignedPrismEventView {
  given decoder: JsonDecoder[SignedPrismEventView[OP]] = {
    import fmgp.util.decoderByteArray
    given decoderPrismEvent: JsonDecoder[PrismEvent] = // use mapOrFail
      decoderByteArray.map(e => PrismEvent.parseFrom(e)) // FIXME catch exceptions
    DeriveJsonDecoder.gen[SignedPrismEventView[OP]]
  }
  given encoder: JsonEncoder[SignedPrismEventView[OP]] = {
    import fmgp.util.encoderByteArray
    given encoderPrismEvent: JsonEncoder[PrismEvent] =
      encoderByteArray.contramap((e: PrismEvent) => e.toByteArray)
    DeriveJsonEncoder.gen[SignedPrismEventView[OP]]
  }

  given encoderTypeDIDEvent: JsonEncoder[SignedPrismEventView[OP.TypeDIDEvent]] =
    encoder.contramap(e => e)
  given encoderTypeStorageEntryEvent: JsonEncoder[SignedPrismEventView[OP.TypeStorageEntryEvent]] =
    encoder.contramap(e => e)
}

object InvalidPrismObject {
  given decoder: JsonDecoder[InvalidPrismObject] = DeriveJsonDecoder.gen[InvalidPrismObject]
  given encoder: JsonEncoder[InvalidPrismObject] = DeriveJsonEncoder.gen[InvalidPrismObject]
}
object InvalidSignedPrismEvent {
  given decoder: JsonDecoder[InvalidSignedPrismEvent] = DeriveJsonDecoder.gen[InvalidSignedPrismEvent]
  given encoder: JsonEncoder[InvalidSignedPrismEvent] = DeriveJsonEncoder.gen[InvalidSignedPrismEvent]
}

object MySignedPrismEvent {

  def apply[T <: OP](
      tx: String,
      b: Int,
      o: Int,
      signedWith: String,
      signature: Array[Byte],
      // operation: T,
      protobuf: PrismEvent
  ) = new MySignedPrismEvent(
    tx = tx,
    b = b,
    o = o,
    signedWith = signedWith,
    signature = signature,
    protobuf = protobuf,
  ).asInstanceOf[MySignedPrismEvent[T]]

  type SignedPrismEventSSI = MySignedPrismEvent[OP.TypeDIDEvent]
  type SignedPrismEventStorage = MySignedPrismEvent[OP.TypeStorageEntryEvent]

  given decoder: JsonDecoder[MySignedPrismEvent[OP]] = {
    import fmgp.util.decoderByteArray
    given decoderPrismEvent: JsonDecoder[PrismEvent] = // use mapOrFail
      decoderByteArray.map(e => PrismEvent.parseFrom(e)) // FIXME catch exceptions
    DeriveJsonDecoder.gen[MySignedPrismEvent[OP]]
  }
  given encoder: JsonEncoder[MySignedPrismEvent[OP]] = {
    import fmgp.util.encoderByteArray
    given encoderPrismEvent: JsonEncoder[PrismEvent] =
      encoderByteArray.contramap((e: PrismEvent) => e.toByteArray)
    DeriveJsonEncoder.gen[MySignedPrismEvent[OP]]
  }

  given encoderTypeDIDEvent: JsonEncoder[MySignedPrismEvent[OP.TypeDIDEvent]] =
    encoder.contramap(e => e)
  given encoderTypeStorageEntryEvent: JsonEncoder[MySignedPrismEvent[OP.TypeStorageEntryEvent]] =
    encoder.contramap(e => e)

}

object MaybeEvent {

  given decoder: JsonDecoder[MaybeEvent[OP]] =
    MySignedPrismEvent.decoder
      .widen[MaybeEvent[OP]]
      .orElseAndWarpErrors(InvalidSignedPrismEvent.decoder.widen[MaybeEvent[OP]])
      .orElseAndWarpErrors(InvalidPrismObject.decoder.widen[MaybeEvent[OP]])

  given encoder: JsonEncoder[MaybeEvent[OP]] = new JsonEncoder[MaybeEvent[OP]] {
    override def unsafeEncode(b: MaybeEvent[OP], indent: Option[Int], out: zio.json.internal.Write): Unit = {
      b match {
        case obj: MySignedPrismEvent[OP]  => MySignedPrismEvent.encoder.unsafeEncode(obj, indent, out)
        case obj: InvalidSignedPrismEvent => InvalidSignedPrismEvent.encoder.unsafeEncode(obj, indent, out)
        case obj: InvalidPrismObject      => InvalidPrismObject.encoder.unsafeEncode(obj, indent, out)
      }
    }
  }

  def fromProto(
      prismObject: PrismObject,
      tx: String,
      blockIndex: Int,
  ): Seq[MaybeEvent[OP]] =
    prismObject.blockContent match
      case None             => Seq(InvalidPrismObject(tx = tx, b = blockIndex, reason = "blockContent is missing"))
      case Some(prismBlock) =>
        prismBlock.events.zipWithIndex.map { case (signedPrismEvent, opIndex) =>
          fromProto(signedPrismEvent = signedPrismEvent, tx = tx, blockIndex = blockIndex, opIndex = opIndex)
        }

  def fromProto(
      signedPrismEvent: SignedPrismEvent,
      tx: String,
      blockIndex: Int,
      opIndex: Int,
  ): MaybeEvent[OP] =
    signedPrismEvent match
      case SignedPrismEvent(signedWith, signature, event, unknownFields) =>
        assert(unknownFields.serializedSize == 0, "SignedPrismEvent have unknownFields")
        event match
          case None             => InvalidSignedPrismEvent(tx = tx, b = blockIndex, o = opIndex, "event is missing")
          case Some(prismEvent) =>
            // FIXME OP.fromPrismEvent(prismOperation) is UNSAFE ! So the apply method is unsafe
            MySignedPrismEvent(
              tx = tx,
              b = blockIndex,
              o = opIndex,
              signedWith = signedWith,
              signature = signature.toByteArray(),
              // operation = OP.fromPrismEvent(prismEvent),
              protobuf = prismEvent,
            )
}
