package fmgp.multiformats

case class Multicodec(codec: Codec, dataBytes: Array[Byte]) {
  def warp: MulticodecRaw = MulticodecRaw.unsafe(codec.varint.bytes ++ dataBytes)
  def bytes = warp.bytes
}
object Multicodec {
  def unsafeFromBytes(bytes: Array[Byte]): Multicodec = fromBytes(bytes) match
    case Left(error)  => throw new IllegalArgumentException(error)
    case Right(value) => value

  def fromBytes(bytes: Array[Byte]): Either[String, Multicodec] = {
    Varint(bytes).decodeInt.flatMap((code, offset) =>
      Codec.values.find(_.code == code) match
        case None        => Left(s"Multicodec unknown '$code'")
        case Some(codec) => Right(Multicodec(codec, bytes.drop(offset)))
    )
  }
}

opaque type MulticodecRaw = Array[Byte]
object MulticodecRaw {
  def unsafe(bytes: Array[Byte]): MulticodecRaw = bytes
  def apply(bytes: Array[Byte]): Either[String, MulticodecRaw] = Multicodec.fromBytes(bytes).map(_.warp)
  extension (data: MulticodecRaw)
    def bytes: Array[Byte] = data
    def unwrap: Multicodec = Multicodec.unsafeFromBytes(data)
    def length: Int = data.length
}
