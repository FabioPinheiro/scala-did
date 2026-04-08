package fmgp.crypto

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import fmgp.util.*
import fmgp.crypto.error.*
import fmgp.crypto.UtilsJVM.toJWK

object CryptoRawBytesOperationsJVM {

  private def signatureAlgorithm(crv: Curve): String = crv match {
    case Curve.`P-256` | Curve.secp256k1 => "SHA256withECDSA"
    case Curve.`P-384`                   => "SHA384withECDSA"
    case Curve.`P-521`                   => "SHA512withECDSA"
    case other                           => throw new IllegalArgumentException(s"Unsupported EC curve: $other")
  }

  def okpKeySignBytesWithEd25519(
      okpKey: OKPPrivateKey,
      payload: Array[Byte]
  ): Array[Byte] = {
    val signer = Ed25519Sign(Base64.fromBase64url(okpKey.d).decode)
    signer.sign(payload)
  }

  def okpKeyVerifyBytesWithEd25519(key: OKPPublicKey, payload: Array[Byte], signature: Array[Byte]): Boolean = {
    val verifier = Ed25519Verify(Base64.fromBase64url(key.x).decode)
    try {
      verifier.verify(signature, payload)
      true
    } catch {
      case _: java.security.GeneralSecurityException => false
    }
  }

  def ecKeySignBytesWithEC(key: ECPrivateKey, payload: Array[Byte]): Array[Byte] = {
    val nimbusKey = key.toJWK
    val ecPrivateKey = nimbusKey.toECPrivateKey
    val sig = java.security.Signature.getInstance(signatureAlgorithm(key.crv), CryptoProvider.provider)
    sig.initSign(ecPrivateKey)
    sig.update(payload)
    sig.sign() // DER-encoded
  }

  def ecKeyVerifyBytesWithEC(key: ECPublicKey, payload: Array[Byte], signature: Array[Byte]): Boolean = {
    val nimbusKey = key.toJWK
    val ecPublicKey = nimbusKey.toECPublicKey
    val sig = java.security.Signature.getInstance(signatureAlgorithm(key.crv), CryptoProvider.provider)
    sig.initVerify(ecPublicKey)
    sig.update(payload)
    try sig.verify(signature)
    catch { case _: java.security.SignatureException => false }
  }

}
