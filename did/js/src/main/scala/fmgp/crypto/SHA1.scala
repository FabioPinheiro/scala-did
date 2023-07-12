package fmgp.crypto

import java.security.MessageDigest
import scala.scalajs.js.typedarray.Uint8Array
import fmgp.util.bytes2Hex

import typings.jsSha1.mod

/** @see https://www.ietf.org/rfc/rfc3174.txt */
object SHA1 {
  def digestToHex(str: String): String = bytes2Hex(digest(str))
  def digestToHex(data: Array[Byte]): String = bytes2Hex(digest(data))
  def digest(data: Array[Byte]): Array[Byte] = digest(String(data.map(_.toChar)))
  def digest(str: String): Array[Byte] = mod.^.array(str).map(_.toByte).toArray
}
