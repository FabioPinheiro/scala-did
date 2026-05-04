package fmgp.crypto

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.typedarray.*
import scala.util.{Failure, Success, Try}

import fmgp.typings.nobleCurves.secp256k1ModAUX
import fmgp.did.method.prism.proto.PrismPublicKey.*

object SharedCryto {

  def getXY(com: Array[Byte]): Either[String, (String, String)] = ???

  /** ECDSA signature verification on Scala.js, using `@noble/curves/secp256k1`.
    *
    * PRISM signs the raw protobuf bytes using ECDSA-secp256k1 (sha256 internally), DER-encoded. noble's
    * `secp256k1.verify(sig, msgHash, pubKey, { format: "der", prehash: false, lowS: false })` matches that contract:
    * the message we pass is the already-sha256-hashed payload.
    *
    * For UncompressedECKey we build the 65-byte uncompressed form `0x04 || x || y` (padding to 32 bytes). For
    * CompressedECKey we forward the 33-byte compressed bytes directly.
    *
    * `lowS = false` because PRISM signatures are not normalized to low-s form.
    */
  def checkECDSASignature(
      msg: Array[Byte],
      sig: Array[Byte],
      pubKey: UncompressedECKey | CompressedECKey
  ): Boolean =
    val pubKeyBytes: Uint8Array = pubKey match
      case UncompressedECKey(_, _, _, x, y) =>
        val xPadded = padTo32(x)
        val yPadded = padTo32(y)
        val out = new Uint8Array(65)
        out(0) = 0x04.toShort
        var i = 0
        while i < 32 do { out(1 + i) = (xPadded(i) & 0xff).toShort; i += 1 }
        i = 0
        while i < 32 do { out(33 + i) = (yPadded(i) & 0xff).toShort; i += 1 }
        out
      case CompressedECKey(_, _, _, data) =>
        toUint8Array(data)

    val msgHash: Uint8Array = toUint8Array(SHA256.digest(msg))
    val sigBytes: Uint8Array = toUint8Array(sig)

    val opts = js.Dynamic.literal(
      format = "der",
      prehash = false,
      lowS = false,
    )
    Try(
      secp256k1ModAUX.secp256k1.^.asInstanceOf[js.Dynamic]
        .verify(sigBytes, msgHash, pubKeyBytes, opts)
        .asInstanceOf[Boolean]
    ) match
      case Success(b)         => b
      case Failure(exception) =>
        // Verification failure (e.g. malformed DER, point not on curve) — treat as a `false` result rather than a fatal error.
        println(s"checkECDSASignature: $exception")
        false

  private def padTo32(b: Array[Byte]): Array[Byte] =
    if b.length == 32 then b
    else if b.length < 32 then Array.fill[Byte](32 - b.length)(0) ++ b
    else b.takeRight(32) // strip leading zeros if oversized (rare)

  private def toUint8Array(bytes: Array[Byte]): Uint8Array =
    Uint8Array.from(bytes.map(b => (b & 0xff).toShort).toJSArray)
}
