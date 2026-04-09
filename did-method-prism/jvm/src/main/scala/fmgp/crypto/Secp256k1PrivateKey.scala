package fmgp.crypto

import fmgp.util.Base64
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey
import fmgp.did.method.prism.proto.PrismKeyUsage
import fmgp.did.method.prism.proto.PrismPublicKey

case class Secp256k1PrivateKey(rawBytes: Array[Byte]) {
  private val pk = KMMECSecp256k1PrivateKey.Companion.secp256k1FromByteArray(rawBytes)

  final def privateJWK = {
    val (x, y) = curvePoint
    ECPrivateKeyWithoutKid(
      kty = KTY.EC,
      crv = Curve.secp256k1,
      d = Base64.encode(rawBytes).urlBase64WithoutPadding,
      x = Base64.encode(x).urlBase64WithoutPadding,
      y = Base64.encode(y).urlBase64WithoutPadding,
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

  final def sign(payload: Array[Byte]): Array[Byte] =
    CryptoRawBytesOperationsJVM.ecKeySignBytesWithEC(privateJWK, payload)
  final def verify(signature: Array[Byte], payload: Array[Byte]): Boolean =
    CryptoRawBytesOperationsJVM.ecKeyVerifyBytesWithEC(privateJWK.toPublicKey, payload, signature)

  final def signWithApollo(payload: Array[Byte]): Array[Byte] = pk.sign(payload)

  /** user verify instade */
  final def verifyWithApollo(signature: Array[Byte], payload: Array[Byte]): Boolean = pk.verify(signature, payload)

}
