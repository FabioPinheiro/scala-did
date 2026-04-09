package fmgp.crypto

import fmgp.util.Base64
import fmgp.did.method.prism.proto.PrismPublicKey
import fmgp.did.method.prism.proto.PrismKeyUsage

case class Secp256k1PrivateKey(rawBytes: Array[Byte]) { // TODO

  final def privateJWK = {
    val (x, y) = curvePoint
    ECPrivateKeyWithoutKid(
      kty = KTY.EC,
      crv = Curve.secp256k1,
      d = Base64.encode(rawBytes).basicBase64,
      x = Base64.encode(x).basicBase64,
      y = Base64.encode(y).basicBase64,
    )
  }

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

  final def sign(payload: Array[Byte]): Array[Byte] = ???
  final def verify(signature: Array[Byte], payload: Array[Byte]): Boolean = ???
  final def signWithApollo(payload: Array[Byte]): Array[Byte] = ???
  final def verifyWithApollo(signature: Array[Byte], payload: Array[Byte]): Boolean = ???
}
