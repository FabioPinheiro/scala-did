package fmgp.did.method.prism

import zio.json._

import fmgp.util.bytes2Hex
import fmgp.util.hex2bytes

opaque type EventHash = Array[Byte]

object EventHash:
  def apply(hash: Array[Byte]): EventHash = fromBytes(hash) // REMOVE in favor of fromBytes
  // TODO Remove assert for optimization
  def fromBytes(hash: Array[Byte]): EventHash = { assert(hash.size == 32, "Hash256 MUST have 32 bytes"); hash }
  def fromHex(hex: String): EventHash = { assert(hex.size == 64, "Hash256 MUST 64 hex characters"); hex2bytes(hex) }

  given JsonDecoder[EventHash] = JsonDecoder.string.map(EventHash.fromHex(_))
  given JsonEncoder[EventHash] = JsonEncoder.string.contramap[EventHash](_.hex)

  extension (hash: EventHash)
    def hex: String = bytes2Hex(hash)
    def byteArray: Array[Byte] = hash
