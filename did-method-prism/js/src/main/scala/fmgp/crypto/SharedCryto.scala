package fmgp.crypto

import scala.scalajs.js.typedarray._
import scala.scalajs.js.JSConverters._
import fmgp.typings.nobleCurves.secp256k1ModAUX
import fmgp.typings.nobleCurves.esmUtilsMod.Hex
import fmgp.did.method.prism.proto.PrismPublicKey._
import fmgp.crypto.Schnorr

object SharedCryto {
  def getXY(com: Array[Byte]): Either[String, (String, String)] = ???

  def checkECDSASignature(
      msg: Array[Byte],
      sig: Array[Byte],
      pubKey: UncompressedECKey | CompressedECKey
  ): Boolean = {
    pubKey match
      case UncompressedECKey(id, usage, curve, x, y) => ??? // FIXME
      case CompressedECKey(id, usage, curve, data) =>
        val (rValue, sValue) =
          Schnorr.rsValuesFromDEREncoded(sig) // https://b10c.me/blog/006-evolution-of-the-bitcoin-signature-length/
        val signature: Hex = Uint8Array.from((rValue ++ sValue).map(_.toShort).toJSArray)
        val message: Hex = Uint8Array.from(msg.map(_.toShort).toJSArray)
        val publicKey: Hex = Uint8Array.from(data.map(_.toShort).toJSArray)

        secp256k1ModAUX.schnorr.verify(
          signature = signature,
          message = message,
          publicKey = publicKey // Error: publicKey of length 32 expected, got 33 FIXME

        )
  }

}
