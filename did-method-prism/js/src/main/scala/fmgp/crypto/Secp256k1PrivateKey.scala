package fmgp.crypto

import fmgp.did.method.prism.proto.PrismPublicKey
import fmgp.did.method.prism.proto.PrismKeyUsage

case class Secp256k1PrivateKey(rawBytes: Array[Byte]) { // TODO
  final def compressedPublicKeyBytes: Array[Byte] = ???
  final def compressedKeyData: proto.prism.PublicKey.KeyData.CompressedEcKeyData =
    proto.prism.PublicKey.KeyData.CompressedEcKeyData(
      value = proto.prism.CompressedECKeyData(
        curve = "secp256k1",
        data = com.google.protobuf.ByteString.copyFrom(this.compressedPublicKeyBytes)
      )
    )
  final def compressedKey(id: String, keyUsage: PrismKeyUsage): PrismPublicKey.CompressedECKey =
    PrismPublicKey.CompressedECKey(id = id, usage = keyUsage, curve = "secp256k1", data = compressedPublicKeyBytes)

  final def curvePoint: (Array[Byte], Array[Byte]) = (???, ???)

  final def sign(data: Array[Byte]): Array[Byte] = ???
  final def verify(signature: Array[Byte], data: Array[Byte]): Boolean = ???
}
