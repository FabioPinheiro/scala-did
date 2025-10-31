package fmgp.util

import java.{util => ju}
import zio.json._
// import scala.collection.compat.immutable.ArraySeq
import scala.util.Try

// Base64 URL
// opaque type Base64 = ArraySeq[Byte] // Byte is primitive, but elements will be boxed/unboxed on their way in or out of the backing Array
opaque type Base64 = Vector[Byte] //TODO benchmark this
// opaque type Base64 = Array[Byte] // Not hash safe! Has different hash each instance.
//opaque type Base64 = Seq[Byte]

object Base64:
  /** Base64 url (without padding) encoder RFC4648 */
  val urlEncoder = ju.Base64.getUrlEncoder.withoutPadding()

  /** Base64 url decoder RFC4648 */
  val urlDecoder = ju.Base64.getUrlDecoder

  /** Base64 basic encoder RFC4648 */
  val basicEncoder = ju.Base64.getEncoder

  /** Base64 basic decoder RFC4648 */
  val basicDecoder = ju.Base64.getDecoder

  given decoder: JsonDecoder[Base64] = JsonDecoder.string.map(Base64.apply)
  given encoder: JsonEncoder[Base64] = JsonEncoder.string.contramap[Base64](e => e.urlBase64)

  /** @param str
    *   Base64 URL string
    *
    * TODO: method copyArrayToImmutableIndexedSeq in class LowPriorityImplicits2 is deprecated since 2.13.0: Implicit
    * conversions from Array to immutable.IndexedSeq are implemented by copying; Use the more efficient non-copying
    * ArraySeq.unsafeWrapArray or an explicit toIndexedSeq call
    */
  def apply(str: String): Base64 = str.getBytes.toVector

  /** @param str
    *   Base64 URL Byte
    */
  def apply(bytes: Array[Byte]): Base64 = bytes.toVector
  def apply(bytes: Vector[Byte]): Base64 = bytes
  // TODO rename fromBase64url to unsafeBase64url
  def fromBase64url(str: String): Base64 = unsafeBase64url(str)
  def fromBase64(str: String): Base64 = encode(basicDecoder.decode(str.getBytes))

  inline def unsafeBase64url(str: String): Base64 = str.getBytes.toVector
  def safeBase64url(str: String): Either[String, Base64] =
    Try(Base64.urlDecoder.decode(str)).toEither match // try to par
      case Right(value)                       => Right(unsafeBase64url(str))
      case Left(ex: IllegalArgumentException) => Left("Fail to decode base64 (url): " + ex.getMessage)
      case Left(ex) /*should never happen*/   => Left("Fail unexpected to decode base64 (url): " + ex.getMessage)

  inline def encode(str: String): Base64 = urlEncoder.encode(str.getBytes).toVector
  inline def encode(data: Array[Byte]): Base64 = urlEncoder.encode(data).toVector
  inline def encode(data: Vector[Byte]): Base64 = urlEncoder.encode(data.toArray).toVector

  extension (bytes: Base64)
    def bytesArray: Array[Byte] = bytes.toArray
    def bytesVec: Vector[Byte] = bytes

    // @deprecated("this method will be removed in favor of urlBase64WithoutPadding", "v0.1.0-M23")
    /** original String Base64 URL (can or not have padding) */
    def urlBase64: String = String(bytesArray)
    def urlBase64WithoutPadding: String = String(bytesArray).filterNot(_ == '=')

    def basicBase64: String = String(Base64.basicEncoder.encode(Base64.urlDecoder.decode(bytesArray)))
    def decode: Array[Byte] = Base64.urlDecoder.decode(bytesArray)
    def decodeToVector: Vector[Byte] = Base64.urlDecoder.decode(bytesArray).toVector
    def decodeToString: String = String(Base64.urlDecoder.decode(bytesArray))

    /** Decodes this Base64 object to an unsigned big integer. */
    def decodeToBigInt = BigInt(1, bytes.decode)
    def decodeToHex = bytes.decode.map("%02X".format(_)).mkString

    def asObj[T](using jsonDecoder: JsonDecoder[T]): Either[String, Base64Obj[T]] =
      decodeToString.fromJson[T].map(t => Base64Obj(t, Some(bytes)))
    def unsafeAsObj[T](using jsonDecoder: JsonDecoder[T]): Base64Obj[T] = asObj[T] match
      case Right(value) => value
      case Left(error)  =>
        throw new java.lang.AssertionError("assumption failed: " + s"Base64($urlBase64).unsafeAsObj: $error")

/** Base64Obj keep the original base64 encoder (useful to preserve data for doing MAC checks) */
case class Base64Obj[T](obj: T, original: Option[Base64] = None) {
  def base64url(using jsonEncoder: JsonEncoder[T]): String = original.getOrElse(Base64.encode(obj.toJson)).urlBase64
}
object Base64Obj {

  given decoder[T](using jsonDecoder: JsonDecoder[T]): JsonDecoder[Base64Obj[T]] =
    Base64.decoder.mapOrFail { original =>
      original.decodeToString
        .fromJson[T]
        .map(
          Base64Obj(_, Some(original)) // Store the original
        )
    }

  given encoder[T](using jsonEncoder: JsonEncoder[T]): JsonEncoder[Base64Obj[T]] =
    Base64.encoder.contramap[Base64Obj[T]] {
      case Base64Obj(_, Some(original)) => original
      case Base64Obj(obj, None)         => Base64.encode(obj.toJson)
    }
}
