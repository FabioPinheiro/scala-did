package fmgp.prism

object SharedCryto {
  def getXY(com: Array[Byte]): Either[String, (String, String)] = ???

  def checkECDSASignature(
      msg: Array[Byte],
      sig: Array[Byte],
      pubKey: PrismPublicKey
  ): Boolean = true // FIXME
}
