package fmgp.crypto

import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import fmgp.util.bytes2Hex

object SHA256 {
  def digestToHex(str: String): String = bytes2Hex(digest(str))
  def digestToHex(data: Array[Byte]): String = bytes2Hex(digest(data))
  def digest(str: String): Array[Byte] = digest(str.getBytes(StandardCharsets.UTF_8))
  def digest(data: Array[Byte]): Array[Byte] = MessageDigest
    .getInstance("SHA-256")
    .digest(data)
}

import zio.*

object SHA256ZIO {

  def digestToHex(str: String): UIO[String] = digest(str).map(bytes2Hex(_))
  def digestToHex(data: Array[Byte]): UIO[String] = digest(data).map(bytes2Hex(_))

  def digest(str: String): UIO[Array[Byte]] = ZIO.succeed(SHA256.digest(str))
  def digest(data: Array[Byte]): UIO[Array[Byte]] = ZIO.succeed(SHA256.digest(data))
}
