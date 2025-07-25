package fmgp.crypto

case class Secp256k1PrivateKey(rawBytes: Array[Byte]) { // TODO
  final def compressedPublicKey: Array[Byte] = ???
  final def compressedEcKeyData: proto.prism.PublicKey.KeyData.CompressedEcKeyData =
    proto.prism.PublicKey.KeyData.CompressedEcKeyData(
      value = proto.prism.CompressedECKeyData(
        curve = "secp256k1",
        data = com.google.protobuf.ByteString.copyFrom(this.compressedPublicKey)
      )
    )

  final def sign(data: Array[Byte]): Array[Byte] = ???
  final def verify(signature: Array[Byte], data: Array[Byte]): Boolean = ???
}
