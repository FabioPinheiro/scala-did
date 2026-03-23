package fmgp.did.method.prism.cardano

import scala.jdk.CollectionConverters.*
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey
import scalus.cardano.address.*
import scalus.cardano.wallet.hd.*
import scalus.cardano.txbuilder.TransactionSigner
import scalus.crypto.ed25519.given

import fmgp.crypto.{Secp256k1PrivateKey, OKPPrivateKeyWithoutKid, KTY, Curve}
import fmgp.did.method.prism.proto.PrismKeyUsage
import fmgp.util.Base64

extension (wallet: CardanoWalletConfig) {

  def seed: Array[Byte] = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)

  def secp256k1PrivateKey(depth: Int, childIndex: Int): Secp256k1PrivateKey =
    Secp256k1PrivateKey(HDKey(wallet.seed, depth, childIndex).getKMMSecp256k1PrivateKey().getEncoded())

  def account(index: Int = 0): HdAccount = HdAccount.fromMnemonic(wallet.mnemonicPhrase, wallet.passphrase, index)
  def address(account: HdAccount, network: CardanoNetwork): ShelleyAddress =
    network match
      case PublicCardanoNetwork.Mainnet                        => account.baseAddress(Network.Mainnet)
      case PublicCardanoNetwork.Testnet                        => account.baseAddress(Network.Testnet)
      case PublicCardanoNetwork.Preprod                        => account.baseAddress(Network.Testnet)
      case PublicCardanoNetwork.Preview                        => account.baseAddress(Network.Testnet)
      case PrivateCardanoNetwork(blockfrostURL, protocolMagic) =>
        throw RuntimeException("CardanoWalletConfig address (with HdAccount)")
  def address(index: Int, network: CardanoNetwork): ShelleyAddress =
    network match
      case PublicCardanoNetwork.Mainnet                        => account(index).baseAddress(Network.Mainnet)
      case PublicCardanoNetwork.Testnet                        => account(index).baseAddress(Network.Testnet)
      case PublicCardanoNetwork.Preprod                        => account(index).baseAddress(Network.Testnet)
      case PublicCardanoNetwork.Preview                        => account(index).baseAddress(Network.Testnet)
      case PrivateCardanoNetwork(blockfrostURL, protocolMagic) =>
        throw RuntimeException("CardanoWalletConfig address (with didIndex)")

  def addressMainnet(account: HdAccount): ShelleyAddress = account.baseAddress(Network.Mainnet)
  def addressTestnet(account: HdAccount): ShelleyAddress = account.baseAddress(Network.Testnet)
  def addressMainnet(index: Int = 0): ShelleyAddress = account(index).baseAddress(Network.Mainnet)
  def addressTestnet(index: Int = 0): ShelleyAddress = account(index).baseAddress(Network.Testnet)

  // network match //network: PublicCardanoNetwork = PublicCardanoNetwork.Mainnet
  //   case PublicCardanoNetwork.Mainnet => account(index).baseAddress(Network.Mainnet) // networkId is 1
  //   case PublicCardanoNetwork.Testnet => account(index).baseAddress(Network.Testnet) // networkId is 0
  //   case PublicCardanoNetwork.Preprod => account(index).baseAddress(Network.Testnet) // networkId is 0 // TODO REVIEW
  //   case PublicCardanoNetwork.Preview => account(index).baseAddress(Network.Testnet) // networkId is 0 // TODO REVIEW

  def signer(account: HdAccount): TransactionSigner = new TransactionSigner(Set(account.paymentKeyPair))
  def signer(index: Int = 0): TransactionSigner = new TransactionSigner(Set(account(index).paymentKeyPair))

  def secp256k1DerivePrism(didIndex: Int = 0, keyUsage: PrismKeyUsage, keyIndex: Int = 0) =
    secp256k1DerivePath(Cip0000.didPath(didIndex, keyUsage, keyIndex))
  def secp256k1DerivePath(path: String) =
    Secp256k1PrivateKey(HDKey(wallet.seed, 0, 0).derive(path).getKMMSecp256k1PrivateKey().getEncoded())

  def prismDeriveMaster(didIndex: Int = 0, keyIndex: Int = 0) =
    secp256k1DerivePrism(didIndex = didIndex, keyUsage = PrismKeyUsage.MasterKeyUsage, keyIndex = keyIndex)
  def prismDeriveVDR(didIndex: Int = 0, keyIndex: Int = 0) =
    secp256k1DerivePrism(didIndex = didIndex, keyUsage = PrismKeyUsage.VdrKeyUsage, keyIndex = keyIndex)
  def prismDeriveAuthentication(didIndex: Int = 0, keyIndex: Int = 0): HdKeyPair =
    ed25519DerivePrism(didIndex = didIndex, keyUsage = PrismKeyUsage.AuthenticationKeyUsage, keyIndex = keyIndex)
  def ed25519DerivePrism(didIndex: Int = 0, keyUsage: PrismKeyUsage, keyIndex: Int = 0): HdKeyPair = ed25519DerivePath(
    Cip0000.didPath(didIndex, keyUsage, keyIndex)
  )
  def ed25519DerivePath(path: String) =
    HdKeyPair.fromMnemonic(mnemonic = wallet.mnemonicPhrase, passphrase = wallet.passphrase, path = path)

  /** Derives an X25519 key-agreement key from the HD wallet.
    *
    * Ed25519 (Twisted Edwards) and X25519 (Montgomery) are birationally equivalent forms of Curve25519. In
    * BIP32-Ed25519, `kL` (the left 32 bytes of the extended key) is already a clamped scalar - identical to what X25519
    * scalar multiplication requires per RFC 7748. Passing `kL` directly to `X25519.publicFromPrivate` (which re-clamps
    * internally) is idempotent and correct.
    *
    * This is '''not''' equivalent to libsodium's `crypto_sign_ed25519_sk_to_curve25519`, which expects a standard RFC
    * 8032 seed and SHA-512 hashes it first. BIP32-Ed25519 `kL` is already the scalar, not a seed, so no hashing is
    * applied here.
    *
    * Key separation: the X25519 key uses `KeyAgreementKeyUsage` (path `3'`), while Ed25519 signing keys use distinct
    * paths (`4'`, `6'`, ...). Different paths produce different `kL` values, so there is no scalar reuse between
    * signing and key-agreement keys.
    *
    * @see
    *   [[https://datatracker.ietf.org/doc/html/rfc7748 RFC 7748 - X25519]]
    * @see
    *   [[https://input-output-hk.github.io/adrestia/static/Ed25519_BIP.pdf BIP32-Ed25519 (Khovratovich & Law 2017)]]
    * @see
    *   [[https://eprint.iacr.org/2021/509.pdf Thormarker 2021 - Joint Ed25519+X25519 security]]
    */
  def x25519DerivePrism(
      didIndex: Int = 0,
      keyUsage: PrismKeyUsage = PrismKeyUsage.KeyAgreementKeyUsage,
      keyIndex: Int = 0
  ): OKPPrivateKeyWithoutKid = {
    val hdKeyPair: HdKeyPair = ed25519DerivePath(Cip0000.didPath(didIndex, keyUsage, keyIndex))
    val kL: Array[Byte] = hdKeyPair.extendedKey.kL
    val pubBytes: Array[Byte] = com.google.crypto.tink.subtle.X25519.publicFromPrivate(kL)
    OKPPrivateKeyWithoutKid(
      kty = KTY.OKP,
      crv = Curve.X25519,
      d = Base64.encode(kL).urlBase64WithoutPadding,
      x = Base64.encode(pubBytes).urlBase64WithoutPadding,
    )
  }
}
