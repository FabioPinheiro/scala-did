package fmgp.did.method.prism

import zio.*
import scalus.cardano.node.BlockfrostProvider

import scalus.cardano.wallet.hd.*
import org.hyperledger.identus.apollo.derivation.HDKey
import fmgp.did.method.prism.proto.PrismKeyUsage
import fmgp.did.method.prism.cardano.*
import fmgp.crypto.*
import fmgp.util.Base64

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

extension (hdKey: HdKeyPair) def publicKeyJWK = OKPPublicKey.makeEd25519(x = Base64.encode(hdKey.verificationKey.bytes))
