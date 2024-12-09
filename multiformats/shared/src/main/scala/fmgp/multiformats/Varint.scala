package fmgp.multiformats

import scala.annotation.tailrec

opaque type Varint = Array[Byte]
// opaque type Varint = Seq[Byte] //TODO Maybe is more safe with Seq

/** Implementation of the unsigned-varint specification
  *
  * @see
  *   https://github.com/multiformats/unsigned-varint
  */
object Varint {
  def apply(value: Array[Byte]): Varint = value
  extension (varint: Varint)
    def bytes: Array[Byte] = varint
    def seq: Seq[Byte] = varint.toSeq
    def length: Int = varint.length
    def apply(index: Int): Byte = varint.bytes(index)

    def decodeInt = Varint.decodeToInt(varint)
    def unsafeDecodeInt = Varint.decodeToInt(varint) match
      case Left(error)  => throw new IllegalArgumentException(error)
      case Right(value) => value

    def decodeLong = Varint.decodeToLong(varint)
    def unsafeDecodeLong = Varint.decodeToLong(varint) match
      case Left(error)  => throw new IllegalArgumentException(error)
      case Right(value) => value

  private val MSB = 0x80
  private val LOW_7_BITS = 0x7f
  private val INT_REST_BITES = 0xffffff80
  private val LONG_REST_BITES = 0xffffffffffffff80L

  def encodeInt(num: Int): Varint = {
    @tailrec
    def rec(value: Int, acc: Array[Byte]): Array[Byte] =
      if ((value & INT_REST_BITES) == 0) acc :+ (value & LOW_7_BITS).toByte
      else rec(value >>> 7, acc :+ ((value & LOW_7_BITS) | MSB).toByte)

    Varint(rec(num, Array()))
  }

  def encodeLong(num: Long): Varint = {
    @tailrec
    def rec(value: Long, acc: Array[Byte]): Array[Byte] =
      if ((value & LONG_REST_BITES) == 0) acc :+ (value & LOW_7_BITS).toByte
      else rec(value >>> 7, acc :+ ((value & LOW_7_BITS) | MSB).toByte)

    Varint(rec(num, Array()))
  }

  def decodeToInt(bytes: Varint): Either[String, (Int, Int)] = decodeToInt(bytes, 0)
  def decodeToInt(bytes: Varint, offset: Int): Either[String, (Int, Int)] = {
    @tailrec
    def rec(index: Int, shift: Int, acc: Int): Either[String, (Int, Int)] =
      if (index >= bytes.length) Left("Cannot find the ending Byte")
      else if ((bytes(index) & MSB) == 0) Right(acc | (bytes(index) << shift), index + 1 - offset)
      else rec(index + 1, shift + 7, acc | ((bytes(index) & LOW_7_BITS) << shift))
    rec(offset, 0, 0)
  }

  def decodeToLong(bytes: Varint): Either[String, (Long, Int)] = decodeToLong(bytes, 0)
  def decodeToLong(bytes: Varint, offset: Int): Either[String, (Long, Int)] = {
    @tailrec
    def rec(index: Int, shift: Long, acc: Long): Either[String, (Long, Int)] =
      if (index >= bytes.length) Left("Cannot find the ending Byte")
      else if ((bytes(index) & MSB) == 0) Right(acc | (bytes(index).toLong << shift), index + 1 - offset)
      else rec(index + 1, shift + 7, acc | ((bytes(index).toLong & LOW_7_BITS) << shift))

    rec(offset, 0L, 0L)
  }

  def extractLength(bytes: Array[Byte], offset: Int): Int = {
    @tailrec
    def rec(index: Int): Int =
      if (index >= bytes.length) 0
      else if ((bytes(index) & MSB) == 0) index + 1 - offset
      else rec(index + 1)

    rec(offset)
  }

}
