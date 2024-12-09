package fmgp.multiformats

case class Multihash(codec: Codec.AllHash, size: Int, hash: Array[Byte]) {
  def warp: MulticodecRaw = MulticodecRaw.unsafe(codec.varint.bytes ++ Varint.encodeInt(size).bytes ++ hash)
  def bytes = warp.bytes
}
object Multihash {

  def apply(codec: Codec.AllHash, hash: Array[Byte]): Multihash = new Multihash(codec, hash.length, hash)
  // private def apply(codec: Codec.AllHash, size: Int, hash: Array[Byte]) = ??? // FIXME REMOVE method

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
