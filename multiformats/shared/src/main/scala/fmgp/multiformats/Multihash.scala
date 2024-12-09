package fmgp.multiformats

case class Multihash(codec: Codec.AllHash, size: Int, hash: Array[Byte]) {
  def warp: MulticodecRaw = MulticodecRaw.unsafe(codec.varint.bytes ++ Varint.encodeInt(size).bytes ++ hash)
  def bytes = warp.bytes
}
object Multihash {

  // "SHA-1" ->  0x11, 20
  // "SHA-256" ->  0x12, 32
  // "SHA-512" ->  0x13, 64
  // "SHA-3" ->  0x14, 64
  // "BLAKE2b" ->  0x40, 64
  // "BLAKE2s" ->  0x41, 32

  def unsafeFromBytes(bytes: Array[Byte]): Multihash = {
    val multicodec = Multicodec.unsafeFromBytes(bytes)
    val (size, offset) = Varint(multicodec.dataBytes).unsafeDecodeInt
    Multihash(multicodec.codec, size, multicodec.dataBytes.drop(offset))
  }
  def fromBytes(bytes: Array[Byte]): Either[String, Multihash] = {
    Multicodec.fromBytes(bytes) match
      case Left(error) => Left(s"Multihash fail to parse Multicodec: $error")
      case Right(multicodec)
          if (multicodec.codec.tag == CodecTag.HASH || multicodec.codec.tag == CodecTag.MULTIHASH) => {
        Varint(multicodec.dataBytes).decodeInt match
          case Left(err) => Left(s"Multihash fail parse size: $err")
          case Right((size, offset)) =>
            Right(Multihash(multicodec.codec, size, multicodec.dataBytes.drop(offset).take(size)))
      }
      case Right(multicodec) =>
        Left(s"Multihash codec MUST be a ${CodecTag.HASH} or ${CodecTag.MULTIHASH} instade of ${multicodec.codec}")
  }
}
