package fmgp.crypto

import fmgp.util.Base64
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey
import fmgp.did.method.prism.proto.PrismKeyUsage
import fmgp.did.method.prism.proto.PrismPublicKey

case class Secp256k1PrivateKey(rawBytes: Array[Byte]) {
  private val pk = KMMECSecp256k1PrivateKey.Companion.secp256k1FromByteArray(rawBytes)

  final def toECPrivateKey = {
    val (x, y) = curvePoint
    ECPrivateKeyWithoutKid(
      kty = KTY.EC,
      crv = Curve.secp256k1,
      d = Base64.encode(rawBytes).basicBase64,
      x = Base64.encode(x).basicBase64,
      y = Base64.encode(y).basicBase64,
    )
  }

  final def compressedPublicKeyBytes: Array[Byte] = pk.getPublicKey().getCompressed()
  final def compressedKey(id: String, keyUsage: PrismKeyUsage): PrismPublicKey.CompressedECKey =
    PrismPublicKey.CompressedECKey(id = id, usage = keyUsage, curve = "secp256k1", data = compressedPublicKeyBytes)

  final def compressedKeyData: proto.prism.PublicKey.KeyData.CompressedEcKeyData =
    proto.prism.PublicKey.KeyData.CompressedEcKeyData(
      value = proto.prism.CompressedECKeyData(
        curve = "secp256k1",
        data = com.google.protobuf.ByteString.copyFrom(this.compressedPublicKeyBytes)
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
