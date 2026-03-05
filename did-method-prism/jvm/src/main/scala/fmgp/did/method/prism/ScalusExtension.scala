package fmgp.did.method.prism

import zio.*
import scalus.cardano.node.BlockfrostProvider

import scalus.cardano.wallet.hd.*
import org.hyperledger.identus.apollo.derivation.HDKey
import com.bloxbean.cardano.client.crypto.config.CryptoConfiguration
import com.nimbusds.jose.util.StandardCharset
import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.proto.PrismKeyUsage
import fmgp.did.method.prism.proto.PrismPublicKey
import fmgp.crypto.*
import fmgp.util.Base64
import scalus.cardano.wallet.hd.Bip32Ed25519.ExtendedKey

// object HdAccountPRISM {

// /** Create an HdAccount from a BIP-39 mnemonic.
//   *
//   * Uses BIP32-Ed25519 (Icarus-style) key derivation which is compatible with standard Cardano wallets like Daedalus,
//   * Yoroi, and others.
//   *
//   * @param mnemonic
//   *   the BIP-39 mnemonic sentence
//   * @param passphrase
//   *   optional passphrase (empty string if none)
//   * @param didIndex
//   *   the account index (default 0)
//   * @return
//   *   the HD account
//   */
// def fromMnemonic(
//     mnemonic: String,
//     passphrase: String = "",
//     didIndex: Int = 0,
//     keyUsage: KeyUsageType = KeyUsageType.Master,
//     keyIndex: Int = 0
// )(using ed25519.Ed25519Signer): Bip32Ed25519.ExtendedKey = {
//   require(didIndex >= 0, s"Account index must be non-negative, got $didIndex")
//   val path = Cip0000.didPath(didIndex, keyUsage, keyIndex)
//   val extendedKey = Bip32Ed25519.deriveFromPath(mnemonic, passphrase, path)

//   val pubKeyBytes = ed25519.Ed25519Math.scalarMultiplyBase(extendedKey.kL)
//   val verificationKey = ed25519.VerificationKey.unsafeFromArray(pubKeyBytes)

//   verificationKey.bytes
//   // val sig = signEd25519Extended(extendedKey.bytes /*cardanoExtendedPrivKey*/, publicKey.bytes, message.bytes)
//   // Signature.unsafeFromArray(sig)
//   extendedKey
// }
// }

/** FIXME move to shared folder */
extension (hdKey: HdKeyPair)
  def publicJWK: OKPPublicKeyWithoutKid = OKPPublicKey.makeEd25519(x = Base64.encode(hdKey.verificationKey.bytes))

  // REMOVE
  // /** Convert HdKeyPair (Ed25519) to OKPPrivateKeyWithoutKid */
  // def privateJWK: OKPPrivateKeyWithoutKid = OKPPublicKey.makeEd25519Private(
  //   // BIP32-Ed25519 uses an extended private key for signing, not just kL.
  //   // The standard Ed25519Signer in Nimbus (which uses Google Tink) expects a regular 32-byte Ed25519 seed/scalar.
  //   // But these are Cardano BIP32-Ed25519 keys where kL is the clamped scalar (not a seed).
  //   // Using kL directly as the signing key would work for signing but may not produce signatures verifiable with the standard algorithm using the public key.
  //   // kL IS the private scalar (already clamped), and the public key is kL * B. Standard Ed25519Signer in Nimbus takes the 32-byte private scalar directly. So using kL should work correctly.
  //   d = Base64.encode(hdKey.extendedKey.kL),
  //   x = Base64.encode(hdKey.verificationKey.bytes)
  // )

  final def compressedKey(id: String, keyUsage: PrismKeyUsage): PrismPublicKey.CompressedECKey =
    PrismPublicKey.CompressedECKey(id = id, usage = keyUsage, curve = "Ed25519", data = hdKey.verificationKey.bytes)
  final def compressedKeyData: _root_.proto.prism.PublicKey.KeyData.CompressedEcKeyData =
    _root_.proto.prism.PublicKey.KeyData.CompressedEcKeyData(
      value = _root_.proto.prism.CompressedECKeyData(
        curve = "Ed25519",
        data = com.google.protobuf.ByteString.copyFrom(hdKey.verificationKey.bytes)
      )
    )

extension (jwtUnsigned: JWTUnsigned) {

  def signWithExtendedKey(hdKey: HdKeyPair): Either[String, JWT] = signWithExtendedKey(hdKey.extendedKey)
  def signWithExtendedKey(extendedKey: ExtendedKey): Either[String, JWT] = {
    val signingInput: Array[Byte] =
      jwtUnsigned.base64JWTFormatWithNoSignature.getBytes(StandardCharset.UTF_8)
    val signature: Array[Byte] =
      CryptoConfiguration.INSTANCE.getSigningProvider.signExtended(
        signingInput,
        extendedKey.extendedSecretKey // kL ++ kR (64 bytes)
      )
    SignatureJWT
      .fromBase64url(Base64.encode(signature).urlBase64WithoutPadding)
      .map(s => jwtUnsigned.toJWT(signature = s))

  }
}
