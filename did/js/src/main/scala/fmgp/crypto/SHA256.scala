package fmgp.crypto

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.typedarray.ArrayBuffer
import org.scalajs.dom.{crypto, HashAlgorithm}
import zio._
import fmgp.crypto.error.SomeThrowable
import fmgp.typings.std.global.TextEncoder
import fmgp.typings.jsSha256
import fmgp.util.bytes2Hex

object SHA256 {
  def digestToHex(str: String): String = bytes2Hex(digest(str))
  def digestToHex(data: Array[Byte]): String = bytes2Hex(digest(data))

  def digest(str: String): Array[Byte] = jsSha256.mod.sha256.array(str).map(_.toByte).toArray
  def digest(data: Array[Byte]): Array[Byte] = digest(String(data.map(_.toChar)))
}

object SHA256ZIO {
  def digestToHex(str: String): UIO[String] = digest(str).map(bytes2Hex(_))
  def digestToHex(data: Array[Byte]): UIO[String] = digest(data).map(bytes2Hex(_))

  def digest(str: String): UIO[Array[Byte]] = {
    val encoder = TextEncoder()
    val data = encoder.encode(str)
    for {
      hashBuffer <- ZIO
        .fromPromiseJS(
          // BufferSource
          crypto.subtle
            .digest(
              HashAlgorithm.`SHA-256`,
              data
            )
            .asInstanceOf[js.Promise[ArrayBuffer]]
        )
      // .catchAll(ex => ZIO.fail(SomeThrowable(ex))) // TODO ERROR Type
      hashArray = new Uint8Array(hashBuffer).toArray
      hashArrayByte = hashArray.map(_.toByte)
    } yield hashArrayByte
  }.orDie

  def digest(data: Array[Byte]): UIO[Array[Byte]] = {
    import scala.scalajs.js.JSConverters.*
    for {
      hashBuffer <- ZIO
        .fromPromiseJS(
          // BufferSource
          crypto.subtle
            .digest(
              HashAlgorithm.`SHA-256`,
              Uint8Array.from(data.map(_.toShort).toJSArray)
            )
            .asInstanceOf[js.Promise[ArrayBuffer]]
        )
      // .catchAll(ex => ZIO.fail(SomeThrowable(ex))) // TODO ERROR Type
      hashArray = new Uint8Array(hashBuffer).toArray
      hashArrayByte = hashArray.map(_.toByte)
    } yield hashArrayByte
  }.orDie
}
