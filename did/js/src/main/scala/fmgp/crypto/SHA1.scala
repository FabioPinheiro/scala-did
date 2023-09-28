// package fmgp.crypto

// import java.security.MessageDigest
// import scala.scalajs.js.typedarray.Uint8Array
// import fmgp.util.bytes2Hex
// import scala.scalajs.js
// import scala.scalajs.js.JSConverters._

// import typings.objectHash

// /** @see https://www.ietf.org/rfc/rfc3174.txt */
// object SHA1 {
//   def digestToHex(str: String): String = bytes2Hex(digest(str))
//   def digestToHex(data: Array[Byte]): String = bytes2Hex(digest(data))
//   def digest(data: Array[Byte]): Array[Byte] = digest(String(data.map(_.toChar)))
//   def digest(str: String): Array[Byte] =
//     val options = objectHash.mod.NormalOption()
//     options.encoding = objectHash.objectHashStrings.binary
// // ^.asInstanceOf[js.Dynamic].applyDynamic("sha1")(`object`.asInstanceOf[js.Any]).asInstanceOf[String]
// // ^.asInstanceOf[js.Dynamic].apply               (`object`.asInstanceOf[js.Any], options.asInstanceOf[js.Any])).asInstanceOf[String]
//     // val aaa = objectHash.mod.apply(str.getBytes().toJSArray, option) // sha1(str) // .map(_.toByte).toArray
//     val aaa = (objectHash.mod.^.asInstanceOf[js.Dynamic]
//       .applyDynamic("hash")(str.asInstanceOf[js.Any], options.asInstanceOf[js.Any]))
//       .asInstanceOf[String]
//     println(aaa)
//     Array.empty
// }
