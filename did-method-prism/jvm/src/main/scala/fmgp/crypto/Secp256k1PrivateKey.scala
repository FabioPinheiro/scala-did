package fmgp.crypto

import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey

case class Secp256k1PrivateKey(rawBytes: Array[Byte]) {
  private val pk = KMMECSecp256k1PrivateKey.Companion.secp256k1FromByteArray(rawBytes)
  final def compressedPublicKey: Array[Byte] = pk.getPublicKey().getCompressed()
  final def compressedEcKeyData: proto.prism.PublicKey.KeyData.CompressedEcKeyData =
    proto.prism.PublicKey.KeyData.CompressedEcKeyData(
      value = proto.prism.CompressedECKeyData(
        curve = "secp256k1",
        data = com.google.protobuf.ByteString.copyFrom(this.compressedPublicKey)
      )
    )
  // final def curvePoint: (x: Array[Byte], y: Array[Byte]) = { TODO report BUG in compiler: this does not work in scala JS?
  final def curvePoint: (Array[Byte], Array[Byte]) = {
    val point = pk.getPublicKey().getCurvePoint()
    (point.getX(), point.getY())
  }

  final def sign(data: Array[Byte]): Array[Byte] = pk.sign(data)
  final def verify(signature: Array[Byte], data: Array[Byte]): Boolean = pk.verify(signature, data)
}
