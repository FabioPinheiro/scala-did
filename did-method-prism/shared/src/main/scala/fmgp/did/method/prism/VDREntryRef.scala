package fmgp.did.method.prism

import fmgp.did.method.prism.proto._
import fmgp.util.bytes2Hex
import fmgp.util.hex2bytes

import zio.json._

//FIXME rename to VDREntryRef {

opaque type RefVDR = String
object RefVDR:
  def apply(hash: String): RefVDR = hash
//   def fromEventHash(hash: Array[Byte]): RefVDR = bytes2Hex(hash)
  def fromEventHash(hash: EventHash): RefVDR = hash.hex
  def fromEvent(event: MySignedPrismOperation[OP]): RefVDR =
    event.operation match
      case _: CreateStorageEntryOP     => RefVDR.fromEventHash(event.eventHash)
      case _: UpdateStorageEntryOP     => RefVDR.fromEventHash(event.eventHash)
      case _: DeactivateStorageEntryOP => RefVDR.fromEventHash(event.eventHash)
      case _                           => ??? // FIXME

  extension (id: RefVDR)
    def value: String = id
    def byteArray: Array[Byte] = hex2bytes(id)
    def eventHash: EventHash = EventHash(byteArray)

  given decoder: JsonDecoder[RefVDR] = JsonDecoder.string.map(RefVDR(_))
  given encoder: JsonEncoder[RefVDR] = JsonEncoder.string.contramap[RefVDR](_.value)
  // These given are useful if we use the RefVDR as a Key (ex: Map[RefVDR , Value])
  given JsonFieldDecoder[RefVDR] = JsonFieldDecoder.string.map(s => RefVDR(s)) // TODO use either
  given JsonFieldEncoder[RefVDR] = JsonFieldEncoder.string.contramap(e => e.value)

// object RefVDR:
//   def apply(hash: String): RefVDR = hash
//   def fromEventHash(hash: Array[Byte]): RefVDR = bytes2Hex(hash)
//   def fromEvent(event: MySignedPrismOperation[OP]): RefVDR =
//     event.operation match
//       case _: CreateStorageEntryOP     => event.eventRef.eventHash
//       case _: UpdateStorageEntryOP     => event.eventRef.eventHash
//       case _: DeactivateStorageEntryOP => event.eventRef.eventHash
//       case _                           => ??? // FIXME

// extension (id: RefVDR)
//   def value: String = id
//   def byteArray: Array[Byte] = hex2bytes(id)

// given decoder: JsonDecoder[RefVDR] = JsonDecoder.string.map(RefVDR(_))
// given encoder: JsonEncoder[RefVDR] = JsonEncoder.string.contramap[RefVDR](_.value)
// // These given are useful if we use the RefVDR as a Key (ex: Map[RefVDR , Value])
// given JsonFieldDecoder[RefVDR] = JsonFieldDecoder.string.map(s => RefVDR(s)) // TODO use either
// given JsonFieldEncoder[RefVDR] = JsonFieldEncoder.string.contramap(e => e.value)
