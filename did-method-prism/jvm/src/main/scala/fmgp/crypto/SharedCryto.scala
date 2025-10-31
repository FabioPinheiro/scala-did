package fmgp.crypto

import fmgp.did.method.prism.proto._

object SharedCryto {
  def getXY(com: Array[Byte]): Either[String, (String, String)] =
    CrytoUtil
      .unsafeFromCompressed(com)
      .map(key => key.asInstanceOf[java.security.interfaces.ECPublicKey].getW())
      .map(p => (p.getAffineX().toString(16), p.getAffineY().toString(16))) // .... TODO

  def checkECDSASignature(
      msg: Array[Byte],
      sig: Array[Byte],
      pubKey: PrismPublicKey.UncompressedECKey | PrismPublicKey.CompressedECKey
  ): Boolean = {
    val key = pubKey match
      case PrismPublicKey.UncompressedECKey(id, usage, curve, x, y) =>
        CrytoUtil.unsafeFromByteCoordinates(x, y)
      case PrismPublicKey.CompressedECKey(id, usage, curve, data) =>
        CrytoUtil.unsafeFromCompressed(data)
    key match
      case Left(value) => false
      case Right(key)  =>
        CrytoUtil.checkECDSASignature(msg, sig, key) match
          case Left(value)  => false
          case Right(value) => value
  }
}
