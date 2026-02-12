package fmgp.did.method.prism

import zio.*
import scalus.cardano.node.BlockfrostProvider

import fmgp.did.method.prism.cardano.*

import scalus.cardano.address.Network
import scalus.cardano.wallet.hd.*
import scalus.cardano.txbuilder.TransactionSigner
import scalus.crypto.ed25519.given

extension (wallet: CardanoWalletConfig)
  def account(index: Int = 0): HdAccount = HdAccount.fromMnemonic(wallet.mnemonicPhrase, wallet.passphrase, index)
  def address(account: HdAccount) = account.baseAddress(Network.Mainnet)
  def address(index: Int = 0) = account(index).baseAddress(Network.Mainnet)
  def signer(account: HdAccount): TransactionSigner = new TransactionSigner(Set(account.paymentKeyPair))
  def signer(index: Int = 0): TransactionSigner = new TransactionSigner(Set(account(index).paymentKeyPair))

extension (bf: BlockfrostConfig)
  def nodeProvider: Task[BlockfrostProvider] = bf.network match
    case PublicCardanoNetwork.Mainnet => ZIO.fromFuture(implicit ec => BlockfrostProvider.mainnet(bf.token))
    case PublicCardanoNetwork.Testnet => ???
    case PublicCardanoNetwork.Preprod => ??? // TODO
    case PublicCardanoNetwork.Preview => ??? // TODO
    case PrivateCardanoNetwork(blockfrostURL, protocolMagic) => ???
