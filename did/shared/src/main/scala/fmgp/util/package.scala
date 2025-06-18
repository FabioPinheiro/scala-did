package fmgp

import zio.json._
package object util {

  /** Use call valueOf of a enum inside of safeValueOf (and ONLY valueOf!)
    *
    * Use like this!!!
    * {{{
    *   fmgp.util.safeValueOf(A.valueOf(str))
    * }}}
    *
    * TODO make this a inline macro of Enum[A]
    */
  inline def safeValueOf[A](block: => A): Either[String, A] =
    scala.util.Try(block).toEither match
      case Right(value)                                       => Right(value)
      case Left(ex /*: java.lang.IllegalArgumentException*/ ) => Left(ex.getMessage)

  /** bytes to Hex String */
  inline def bytes2Hex(bytes: Array[Byte]): String = bytes.map { b =>
    String.format("%02x", Integer.valueOf(b & 0xff))
  }.mkString

  inline def hex2bytes(hex: String): Array[Byte] = {
    val tmp = hex.replaceAll("[^0-9A-Fa-f]", "")
    (if (tmp.size % 2 == 0) tmp else "0" + tmp)
      .sliding(2, 2)
      .toArray
      .map(Integer.parseInt(_, 16).toByte)
  }

  given decoderByteArray: JsonDecoder[Array[Byte]] = // use mapOrFail
    JsonDecoder.string.map(e => hex2bytes(e))
  given encoderByteArray: JsonEncoder[Array[Byte]] =
    JsonEncoder.string.contramap((e: Array[Byte]) => bytes2Hex(e))
}
