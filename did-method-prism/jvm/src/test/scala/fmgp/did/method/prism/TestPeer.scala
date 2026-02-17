package fmgp.did.method.prism

import scalus.uplc.builtin.ByteString
import scala.collection.mutable
import scalus.cardano.address.*
import scalus.cardano.ledger.Hash
import scalus.cardano.ledger.Hash28
import scalus.cardano.wallet.hd.*
import scalus.cardano.wallet.Account
import scalus.cardano.node.Emulator
import scalus.cardano.txbuilder.TransactionSigner
import scalus.cardano.wallet.hd.HdAccount
import scalus.crypto.ed25519.given

import fmgp.did.method.prism.cardano.*

enum TestPeer(@annotation.unused ix: Int) derives CanEqual {
  case Alice extends TestPeer(0)
  case Bob extends TestPeer(1)
  case Carol extends TestPeer(2)
  case Daniella extends TestPeer(3)
  case Erin extends TestPeer(4)
  case Frank extends TestPeer(5)
  case Gustavo extends TestPeer(6)
  case Hector extends TestPeer(7)
  case Isabel extends TestPeer(8)
  case Julia extends TestPeer(9)

//   def compareTo(another: TestPeer): Int = this.toString.compareTo(another.toString)

  def account: HdAccount = TestPeer.account(ix)
  def address: ShelleyAddress = TestPeer.address(account)

//   def wallet: Wallet = TestPeer.mkWallet(this)

//   // def walletId: WalletId = TestPeer.mkWalletId(this)

  def signer: TransactionSigner = new TransactionSigner(Set(account.paymentKeyPair))

}

object TestPeer:
  val mnemonic: String =
    "test test test test " +
      "test test test test " +
      "test test test test " +
      "test test test test " +
      "test test test test " +
      "test test test sauce"

  val cw = CardanoWalletConfig.fromMnemonicPhrase(phrase = mnemonic, passphrase = "").getOrElse(???)

  def account(index: Int) = cw.account(0) // HdAccount.fromMnemonic(mnemonic, "", index)
  def address(account: HdAccount) = cw.addressMainnet(0) // account.baseAddress(Network.Mainnet)

  def emulator(addressAUX: ShelleyAddress) = Emulator.withAddresses(Seq(addressAUX))

  val ctx = {
    val accountAUX = account(0)
    val addressAUX = address(accountAUX)
    val emulatorAUX = emulator(addressAUX)
    new AppCtx(
      emulatorAUX.cardanoInfo,
      emulatorAUX,
      accountAUX,
      accountAUX.signerForUtxos,
    )
  }

  // private val accountCache: mutable.Map[TestPeer, Account] = mutable.Map.empty
  //   .withDefault(peer =>
  //     Account.createFromMnemonic(
  //       BBNetwork(0, 42),
  //       mnemonic,
  //       createExternalAddressDerivationPathForAccount(peer.ordinal)
  //     )
  //   )

  // private val walletCache: mutable.Map[TestPeer, Wallet] = mutable.Map.empty
  //   .withDefault(peer =>
  //     Wallet(
  //       peer.toString,
  //       WalletModuleBloxbean,
  //       account(peer).hdKeyPair().getPublicKey,
  //       account(peer).hdKeyPair().getPrivateKey
  //     )
  //   )

  // private val addressCache: mutable.Map[TestPeer, (ShelleyPaymentPart, ShelleyDelegationPart)] =
  //   mutable.Map.empty.withDefault(peer =>
  //     (
  //       Key(Hash(Hash28(ByteString.unsafeFromArray(account(peer).publicKeyBytes())))),
  //       Null
  //     )
  //   )

//   // def mkWalletId(peer: TestPeer): WalletId = WalletId(peer.toString)

//   def address(peer: TestPeer, network: Network = Mainnet): ShelleyAddress = {
//     val (payment, delegation) = addressCache.cache(peer)
//     ShelleyAddress(network, payment, delegation)
//   }

// extension [K, V](map: mutable.Map[K, V])
//   def cache(key: K): V = map.get(key) match {
//     case None =>
//       val missing = map.default(key)
//       @annotation.unused
//       val _ = map.put(key, missing)
//       missing
//     case Some(value) => value
//   }

import scalus.uplc.builtin.{ByteString, Data}
import scalus.cardano.address.{Address, Network as ScalusNetwork}
import scalus.cardano.ledger.{AddrKeyHash, CardanoInfo, Script}

import scala.concurrent.duration.*
import scalus.cardano.node.{BlockchainProvider, BlockfrostProvider}
import scalus.cardano.txbuilder.TransactionSigner
import scalus.cardano.wallet.hd.HdAccount
import scalus.cardano.onchain.plutus.v1.PubKeyHash
import scalus.crypto.ed25519.Ed25519Signer
import scalus.uplc.PlutusV3
import sttp.client4.DefaultFutureBackend
// import sttp.tapir.*
// import sttp.tapir.server.netty.sync.NettySyncServer
// import sttp.tapir.swagger.bundle.SwaggerInterpreter

case class AppCtx(
    cardanoInfo: CardanoInfo,
    provider: BlockchainProvider,
    account: HdAccount,
    signer: TransactionSigner,
    // tokenName: String
) {

  /** Public key hash for use in Plutus scripts (on-chain format) */
  lazy val pubKeyHash: PubKeyHash = PubKeyHash(account.paymentKeyHash)

  /** Address key hash for transaction building (off-chain format) */
  lazy val addrKeyHash: AddrKeyHash = account.paymentKeyHash

  // lazy val tokenNameByteString: ByteString = ByteString.fromString(tokenName)
  lazy val address: Address = account.baseAddress(cardanoInfo.network)

  /** Full asset identifier: policy ID + token name (used for lookups) */
  // lazy val unitName: String = (mintingScript.script.scriptHash ++ tokenNameByteString).toHex

  /** The configured minting policy script, parameterized with our admin key and token name */
  // lazy val mintingScript: PlutusV3[Data => Unit] =
  //   MintingPolicyContract.makeMintingPolicyScript(pubKeyHash, tokenNameByteString)
}
