package fmgp.did.method.prism.cardano

import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import zio.json.*

/** Transaction Hash */
opaque type TxHash = Array[Byte]

object TxHash:
  // def apply(hex: String): TxHash = fromHex(hash)
  // TODO Remove assert for optimization
  def fromBytes(hash: Array[Byte]): TxHash = { assert(hash.size == 32, "Hash256 MUST have 32 bytes"); hash }
  def fromHex(hex: String): TxHash = {
    assert(hex.size == 64, "Hash256 MUST 64 hex characters"); hex2bytes(hex)
  }

  given JsonDecoder[TxHash] = JsonDecoder.string.map(TxHash.fromHex(_))
  given JsonEncoder[TxHash] = JsonEncoder.string.contramap[TxHash](_.hex)

  extension (hash: TxHash)
    def hex: String = bytes2Hex(hash)
    def byteArray: Array[Byte] = hash
