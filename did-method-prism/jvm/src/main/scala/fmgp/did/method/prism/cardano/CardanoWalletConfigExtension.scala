package fmgp.did.method.prism.cardano

import scala.jdk.CollectionConverters.*
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey
import scalus.cardano.address.*
import scalus.cardano.wallet.hd.*
import scalus.cardano.txbuilder.TransactionSigner
import scalus.crypto.ed25519.given

import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.proto.PrismKeyUsage
import fmgp.did.method.prism.proto.PrismKeyUsage.{MasterKeyUsage, VdrKeyUsage}

extension (wallet: CardanoWalletConfig) {

  def seed: Array[Byte] = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)

  def secp256k1PrivateKey(depth: Int, childIndex: Int): Secp256k1PrivateKey =
    Secp256k1PrivateKey(HDKey(wallet.seed, depth, childIndex).getKMMSecp256k1PrivateKey().getEncoded())

  def account(index: Int = 0): HdAccount = HdAccount.fromMnemonic(wallet.mnemonicPhrase, wallet.passphrase, index)
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
    secp256k1DerivePrism(didIndex = didIndex, keyUsage = MasterKeyUsage, keyIndex = keyIndex)
  def prismDeriveVDR(didIndex: Int = 0, keyIndex: Int = 0) =
    secp256k1DerivePrism(didIndex = didIndex, keyUsage = VdrKeyUsage, keyIndex = keyIndex)
  def hdKeyPair(didIndex: Int = 0, keyUsage: PrismKeyUsage = MasterKeyUsage, keyIndex: Int = 0): HdKeyPair =
    HdKeyPair.fromMnemonic(
      mnemonic = wallet.mnemonicPhrase,
      passphrase = wallet.passphrase,
      path = Cip0000.didPath(didIndex, keyUsage, keyIndex)
    )
}
