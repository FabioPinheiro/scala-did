package fmgp.did.method.prism.cardano

import zio.json.*
import fmgp.did.method.prism.proto.PrismKeyUsage
import scalus.cardano.wallet.hd.Bip32Ed25519
import scalus.cardano.wallet.hd.HdKeyPair
import scalus.crypto.ed25519.given

/** @see [[Cip0000]] */
object Cip0000JVM {

  /** Derive a hardened child key pair.
    *
    * Hardened derivation uses the private key and produces keys that cannot be derived from the parent public key.
    *
    * @param index
    *   the child index (will be hardened: index + 0x80000000)
    * @return
    *   the derived child key pair
    */
  def deriveHardened(
      mnemonic: String,
      passphrase: String = "",
      didIndex: Int = 0,
      keyUsage: PrismKeyUsage = PrismKeyUsage.MasterKeyUsage,
      keyIndex: Int = 0
  ): HdKeyPair = {
    val extendedKey: Bip32Ed25519.ExtendedKey = Bip32Ed25519.deriveFromPath(
      mnemonic,
      passphrase,
      Cip0000.didPath(didIndex = didIndex, keyUsage = keyUsage, keyIndex = keyIndex)
    )
    // val childKey = extendedKey.deriveHardened(index)
    // HdKeyPair.fromExtendedKey(childKey)
    HdKeyPair.fromExtendedKey(extendedKey)
  }
}
