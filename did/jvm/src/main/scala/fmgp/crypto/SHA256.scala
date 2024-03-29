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
