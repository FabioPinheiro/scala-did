package fmgp.did.method.prism

import scala.jdk.CollectionConverters.*

import zio.*
import java.math.BigInteger

import fmgp.blockfrost.*
import fmgp.util.*
import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.vdr.*
import _root_.proto.prism.PrismBlock
import _root_.proto.prism.SignedPrismEvent
import _root_.proto.prism.PrismObject

import scalus.cardano.ledger.*
import scalus.cardano.txbuilder.*
import scalus.cardano.node.BlockchainReader
import scalus.cardano.node.BlockfrostProvider
import scalus.cardano.node.BlockchainProvider
import scalus.uplc.builtin.ByteString

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalus.cardano.wallet.Account
import scalus.cardano.address.ShelleyAddress
import scalus.cardano.address.Address
import scalus.cardano.node.UtxoQuery
import scalus.cardano.node.UtxoSource

object ScalusService {
  val env: CardanoInfo = CardanoInfo.mainnet

  object internal {
    import fmgp.did.method.prism.cardano.PRISM_LABEL_CIP_10
    import fmgp.did.method.prism.cardano.MSG_LABEL_CIP_20

    def prismObject2CBORMetadataList(block: PrismObject): Metadatum.List = bytes2CBORMetadataList(block.toByteArray)
    def bytes2CBORMetadataList(bytes: Array[Byte]): Metadatum.List = {
      val seq: Seq[Metadatum] =
        bytes
          .grouped(cborByteStringMaxMatadataSize)
          .foldLeft(Seq.empty[Array[Byte]]) { case (acu, seqData) => acu :+ seqData.toArray[Byte] }
          .map(bytes => Metadatum.Bytes(ByteString.fromArray(bytes)))

      Metadatum.List(seq.toIndexedSeq)
    }

    def prismObject2metadata(prismObject: PrismObject) =
      Metadatum.Map(
        Map(
          Metadatum.Text("v") -> Metadatum.Int(1), // PRISM version
          Metadatum.Text("c") -> prismObject2CBORMetadataList(prismObject)
        )
      )

    //   def text2metadata(text: String) = CBORMetadataMap().put("msg", text)

    //   /** Define expected Outputs from Tx */
    //   def makeOutput(senderAccount: Account): Output = Output
    //     .builder()
    //     .address(senderAccount.baseAddress)
    //     .assetName(LOVELACE)
    //     .qty(BigInteger.valueOf(1)) // .qty(adaToLovelace(1))
    //     .build();

    //   def txBuilder(senderAccount: Account, output: Output, metadata: Metadata): TxBuilder = output
    //     .outputBuilder()
    //     .buildInputs(createFromSender(senderAccount.baseAddress, senderAccount.baseAddress()))
    //     .andThen(metadataProvider(metadata))
    //     .andThen(balanceTx(senderAccount.baseAddress, 1))
  }

  def makeMetadataPrism(prismObject: PrismObject) =
    AuxiliaryData.Metadata(
      Map(Word64.fromUnsignedInt(PRISM_LABEL_CIP_10) -> internal.prismObject2metadata(prismObject))
    )

  // def makeMetadataCIP20(msgCIP20: String) =
  //   MetadataBuilder
  //     .createMetadata()
  //     .put(MSG_LABEL_CIP_20, msgCIP20)

  // def makeMetadataPrismWithCIP20(prismObject: PrismObject, msgCIP20: String = "PRISM VDR (by fmgp)") =
  //   MetadataBuilder
  //     .createMetadata()
  //     .put(PRISM_LABEL_CIP_10, internal.prismObject2metadata(prismObject))
  //     .put(MSG_LABEL_CIP_20, msgCIP20)

  // def makeTxBuilder(bfConfig: BlockfrostConfig, wallet: CardanoWalletConfig, metadata: Metadata): TxBuilder = {
  //   val senderAccount: Account = makeAccount(bfConfig, wallet)
  //   val output = internal.makeOutput(senderAccount)
  //   internal.txBuilder(senderAccount, output, metadata)
  // }

  // def makeTxBuilder(
  //     bfConfig: BlockfrostConfig,
  //     wallet: CardanoWalletConfig,
  //     prismObject: PrismObject,
  //     maybeMsgCIP20: Option[String]
  // ): TxBuilder =
  //   maybeMsgCIP20 match
  //     case None           => makeTxBuilder(bfConfig, wallet, makeMetadataPrism(prismObject))
  //     case Some(msgCIP20) => makeTxBuilder(bfConfig, wallet, makeMetadataPrismWithCIP20(prismObject, msgCIP20))

  // def makeTxBuilder(
  //     bfConfig: BlockfrostConfig,
  //     wallet: CardanoWalletConfig,
  //     prismEvents: Seq[SignedPrismEvent],
  //     maybeMsgCIP20: Option[String],
  // ): TxBuilder =
  //   makeTxBuilder(
  //     bfConfig,
  //     wallet,
  //     PrismObject(blockContent = Some(PrismBlock(events = prismEvents))),
  //     maybeMsgCIP20
  //   )

  def makeTrasation(
      prismEvents: Seq[SignedPrismEvent],
      maybeMsgCIP20: Option[String],
  ): ZIO[BlockfrostConfig & CardanoWalletConfig, Throwable, Transaction] = {

    // val senderAccount: Account = makeAccount(bfConfig, wallet)
    // val backendService: BackendService = makeBFBackendService(bfConfig)
    // val utxoSupplier: UtxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService())
    // val protocolParamsSupplier: ProtocolParamsSupplier =
    //   new DefaultProtocolParamsSupplier(backendService.getEpochService())
    // val txBuilder = makeTxBuilder(bfConfig, wallet, prismEvents, maybeMsgCIP20)

    // // Build and sign the transaction
    // val signedTransaction: Transaction = TxBuilderContext
    //   .init(utxoSupplier, protocolParamsSupplier)
    //   .buildAndSign(txBuilder, signerFrom(senderAccount))
    // signedTransaction

    val meta = AuxiliaryData.Metadata(Map(Word64.fromUnsignedInt(999) -> Metadatum.Int(123)))

    for {
      bfConfig <- ZIO.service[BlockfrostConfig]
      wallet <- ZIO.service[CardanoWalletConfig]
      nodeProvider: BlockfrostProvider <- bfConfig.nodeProvider
      tx <- ZIO.fromFuture(implicit ec =>
        TxBuilder(env)
          .payTo(wallet.addressMainnet(0), Value.lovelace(1))
          .metadata(meta)
          // Magic: sets inputs, collateral input/output, execution budgets, fee, handle change, etc.
          .complete(reader = nodeProvider, sponsor = wallet.addressMainnet(0))
      )
      ret = tx.sign(wallet.signer(0)).transaction
    } yield ret
  }

  // private def makeBFNetworks(n: CardanoNetwork) = n match
  //   case PublicCardanoNetwork.Mainnet    => Networks.mainnet()
  //   case PublicCardanoNetwork.Testnet    => Networks.testnet()
  //   case PublicCardanoNetwork.Preprod    => Networks.preprod()
  //   case PublicCardanoNetwork.Preview    => Networks.preview()
  //   case PrivateCardanoNetwork(_, magic) => Network(0x00, magic)
  // private def makeBFBackendService(bfConfig: BlockfrostConfig) =
  //   new BFBackendService(bfConfig.network.blockfrostURL + "/", bfConfig.token)
  // def makeAccountMainnet(wallet: CardanoWalletConfig): ShelleyAddress = wallet.addressMainnet(0)
  // def makeAccount(wallet: CardanoWalletConfig): ShelleyAddress = wallet.addressTestnet(0)

  // Return the hash/id of the transaction
  def submitTransaction(
      tx: Transaction
  ): ZIO[BlockfrostConfig, Throwable, TxHash] =
    for {
      _ <- ZIO.log("submitTransaction")
      bfConfig <- ZIO.service[BlockfrostConfig]
      nodeProvider: BlockfrostProvider <- bfConfig.nodeProvider
      hash <- ZIO
        .fromFuture(implicit ec => nodeProvider.submit(tx))
        .flatMap {
          case Left(value)  => ZIO.fail(new RuntimeException(value.message)) // TODO new Exception type
          case Right(value) => ZIO.succeed(value)
        }
      // backendService: BackendService = makeBFBackendService(bfConfig)
      // txPayload = tx.serialize()
      // _ <- ZIO.log(s"submitTransaction txPayload = ${bytes2Hex(tx.serialize())}")
      // result <- ZIO.attempt(backendService.getTransactionService().submitTransaction(txPayload))
      _ <- ZIO.log(s"submitTransaction result = ${hash.toHex}")
      _ <- ZIO.log(s"See https://${bfConfig.network.name}.cardanoscan.io/transaction/${hash.toHex}?tab=metadata")
    } yield TxHash.fromHex(hash.toHex)

  def addressesTotalAda(address: String): ZIO[BlockfrostConfig, Throwable, BigDecimal] =
    addressesTotalAda(Address.fromString(address))

  def addressesTotalAda(address: Address): ZIO[BlockfrostConfig, Throwable, BigDecimal] =
    for {
      _ <- ZIO.log(s"addressesTotal for $address")
      bfConfig <- ZIO.service[BlockfrostConfig]
      nodeProvider: BlockfrostProvider <- bfConfig.nodeProvider
      // query = UtxoQuery(UtxoSource.FromAddress(Address.fromString(address)))
      query = UtxoQuery(UtxoSource.FromAddress(address))
      utxos <- ZIO
        .fromFuture(implicit ec => nodeProvider.findUtxos(query))
        .flatMap {
          case Left(value)  => ZIO.fail(new RuntimeException(value.toString)) // TODO new Exception type
          case Right(value) => ZIO.succeed(value)
        }
      coin = utxos.values
        .map { case v: TransactionOutput => v.value.coin }
        .reduce((l, r) => l + r)

      _ <- ZIO.log(s"addressesTotal result = ${coin.value.toString} ADA")
      total = BigDecimal(coin.value) / BigDecimal(1000000)
      _ <- ZIO.log("total ADA found in address")
    } yield total
}
