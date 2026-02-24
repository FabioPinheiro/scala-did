package fmgp.did.method.prism.cardano

import zio.json.*
import fmgp.did.method.prism.proto.PrismKeyUsage
import scalus.cardano.wallet.hd.Bip32Ed25519
import scalus.cardano.wallet.hd.HdKeyPair
import scalus.crypto.ed25519.given

case class Cip0000(didIndex: Int, keyUsage: PrismKeyUsage, keyIndex: Int) {
  def didPath = Cip0000.didPath(didIndex = didIndex, keyUsage = keyUsage, keyIndex = keyIndex)
}

/* https://github.com/input-output-hk/prism-did-method-spec/blob/main/extensions/deterministic-prism-did-generation-proposal.md */
object Cip0000 {

  given decoder: JsonDecoder[Cip0000] = DeriveJsonDecoder.gen[Cip0000]
  given encoder: JsonEncoder[Cip0000] = DeriveJsonEncoder.gen[Cip0000]

  /** Purpose constant 29 (anagram for ID in Hex 0x1D) */
  val PURPOSE: Int = 29

  /** Method type constant for PRISM DID method 29. */
  val METHODTYPE: Int = 29

  def fromPath(derivationPath: String): Either[String, Cip0000] = {
    val PathPattern = """m/29'/29'/(\d+)'/(\d+)'/(\d+)'""".r
    derivationPath match {
      case PathPattern(aStr, bStr, cStr) =>
        val a: Int = aStr.toInt // Safe due to regex
        val b: Int = bStr.toInt // Safe due to regex
        val c: Int = cStr.toInt // Safe due to regex

        if (a < 0) Left("didIndex MUST be a no negative number")
        else if (0 < b && b < 8) Left("keyUsage MUST be between 1 to 8")
        else if (c < 0) Left("didIndex MUST be a no negative number")
        PrismKeyUsage.fromProtoEnum(b) match
          case Left(error)     => Left("keyUsage MUST be between 1 to 8?")
          case Right(keyUsage) => Right(Cip0000(didIndex = a, keyUsage = keyUsage, keyIndex = c))
      case _ =>
        Left(s"Fail to parse regex in `$derivationPath`")
    }
  }

  /** Build the derivation path for an account root key.
    *
    * Path: m/purpose'/method'/did-index/key-usage/key-index
    */
  def didPath(didIndex: Int, keyUsage: PrismKeyUsage, keyIndex: Int): String = {
    require(didIndex >= 0, s"DID Index index must be non-negative, got $didIndex")
    require(keyIndex >= 0, s"Key Index index must be non-negative, got $keyIndex")

    // val keyIndex	Key index within the usage scope, allowing the creation of as many keys as needed.
    s"m/${PURPOSE}'/${METHODTYPE}'/${didIndex}'/${keyUsage.protoEnum}'/${keyIndex}'"
  }

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
      didPath(didIndex = didIndex, keyUsage = keyUsage, keyIndex = keyIndex)
    )
    // val childKey = extendedKey.deriveHardened(index)
    // HdKeyPair.fromExtendedKey(childKey)
    HdKeyPair.fromExtendedKey(extendedKey)
  }
}
