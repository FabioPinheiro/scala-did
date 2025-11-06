package fmgp.did.method.prism

import scala.jdk.CollectionConverters._

import zio._
import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier
import com.bloxbean.cardano.client.api.UtxoSupplier
import com.bloxbean.cardano.client.api.model.Result
import com.bloxbean.cardano.client.backend.api.BackendService
import com.bloxbean.cardano.client.backend.api.DefaultProtocolParamsSupplier
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.common.model.Networks
import com.bloxbean.cardano.client.function.Output
import com.bloxbean.cardano.client.function.TxBuilder
import com.bloxbean.cardano.client.function.TxBuilderContext
import com.bloxbean.cardano.client.function.helper.InputBuilders
import com.bloxbean.cardano.client.transaction.spec.Transaction
import com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace
import com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE
import com.bloxbean.cardano.client.function.helper.AuxDataProviders.metadataProvider
import com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx
import com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender
import com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom
import com.bloxbean.cardano.client.backend.blockfrost.service.http.MetadataApi
import com.bloxbean.cardano.client.function.TxOutputBuilder
import com.bloxbean.cardano.client.metadata._
import com.bloxbean.cardano.client.metadata.cbor._
import java.math.BigInteger
import fmgp.blockfrost.*
import fmgp.util._
import fmgp.did.method.prism.cardano._
import fmgp.did.method.prism.vdr._
import _root_.proto.prism.PrismBlock
// import _root_.proto.prism.PrismEvent
import _root_.proto.prism.SignedPrismEvent
import _root_.proto.prism.PrismObject
import com.bloxbean.cardano.client.common.model.Network

object CardanoService {

  object internal {
    import fmgp.did.method.prism.cardano.PRISM_LABEL_CIP_10
    import fmgp.did.method.prism.cardano.MSG_LABEL_CIP_20

    def prismObject2CBORMetadataList(block: PrismObject): CBORMetadataList = bytes2CBORMetadataList(block.toByteArray)
    def bytes2CBORMetadataList(bytes: Array[Byte]): CBORMetadataList =
      bytes
        .grouped(cborByteStringMaxMatadataSize)
        .foldLeft(CBORMetadataList()) { case (acu, seqData) => acu.add(seqData.toArray[Byte]) }

    def prismObject2metadata(prismObject: PrismObject) =
      CBORMetadataMap()
        .put("v", BigInteger.valueOf(1)) // PRISM version
        .put("c", prismObject2CBORMetadataList(prismObject))

    def text2metadata(text: String) = CBORMetadataMap().put("msg", text)

    /** Define expected Outputs from Tx */
    def makeOutput(senderAccount: Account): Output = Output
      .builder()
      .address(senderAccount.baseAddress)
      .assetName(LOVELACE)
      .qty(BigInteger.valueOf(1)) // .qty(adaToLovelace(1))
      .build();

    def txBuilder(senderAccount: Account, output: Output, metadata: Metadata): TxBuilder = output
      .outputBuilder()
      .buildInputs(createFromSender(senderAccount.baseAddress, senderAccount.baseAddress()))
      .andThen(metadataProvider(metadata))
      .andThen(balanceTx(senderAccount.baseAddress, 1))
  }

  def makeMetadataPrism(prismObject: PrismObject) =
    MetadataBuilder
      .createMetadata()
      .put(PRISM_LABEL_CIP_10, internal.prismObject2metadata(prismObject))

  def makeMetadataCIP20(msgCIP20: String) =
    MetadataBuilder
      .createMetadata()
      .put(MSG_LABEL_CIP_20, msgCIP20)

  def makeMetadataPrismWithCIP20(prismObject: PrismObject, msgCIP20: String = "PRISM VDR (by fmgp)") =
    MetadataBuilder
      .createMetadata()
      .put(PRISM_LABEL_CIP_10, internal.prismObject2metadata(prismObject))
      .put(MSG_LABEL_CIP_20, msgCIP20)

  def makeTxBuilder(bfConfig: BlockfrostConfig, wallet: CardanoWalletConfig, metadata: Metadata): TxBuilder = {
    val senderAccount: Account = makeAccount(bfConfig, wallet)
    val output = internal.makeOutput(senderAccount)
    internal.txBuilder(senderAccount, output, metadata)
  }

  def makeTxBuilder(
      bfConfig: BlockfrostConfig,
      wallet: CardanoWalletConfig,
      prismObject: PrismObject,
      maybeMsgCIP20: Option[String]
  ): TxBuilder =
    maybeMsgCIP20 match
      case None           => makeTxBuilder(bfConfig, wallet, makeMetadataPrism(prismObject))
      case Some(msgCIP20) => makeTxBuilder(bfConfig, wallet, makeMetadataPrismWithCIP20(prismObject, msgCIP20))

  def makeTxBuilder(
      bfConfig: BlockfrostConfig,
      wallet: CardanoWalletConfig,
      prismEvents: Seq[SignedPrismEvent],
      maybeMsgCIP20: Option[String],
  ): TxBuilder =
    makeTxBuilder(
      bfConfig,
      wallet,
      PrismObject(blockContent = Some(PrismBlock(events = prismEvents))),
      maybeMsgCIP20
    )

  def makeTrasation(
      bfConfig: BlockfrostConfig,
      wallet: CardanoWalletConfig,
      prismEvents: Seq[SignedPrismEvent],
      maybeMsgCIP20: Option[String],
  ): Transaction = {

    val senderAccount: Account = makeAccount(bfConfig, wallet)
    val backendService: BackendService = makeBFBackendService(bfConfig)
    val utxoSupplier: UtxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService())
    val protocolParamsSupplier: ProtocolParamsSupplier =
      new DefaultProtocolParamsSupplier(backendService.getEpochService())
    val txBuilder = makeTxBuilder(bfConfig, wallet, prismEvents, maybeMsgCIP20)

    // Build and sign the transaction
    val signedTransaction: Transaction = TxBuilderContext
      .init(utxoSupplier, protocolParamsSupplier)
      .buildAndSign(txBuilder, signerFrom(senderAccount))
    signedTransaction
  }

  private def makeBFNetworks(n: CardanoNetwork) = n match
    case PublicCardanoNetwork.Mainnet    => Networks.mainnet()
    case PublicCardanoNetwork.Testnet    => Networks.testnet()
    case PublicCardanoNetwork.Preprod    => Networks.preprod()
    case PublicCardanoNetwork.Preview    => Networks.preview()
    case PrivateCardanoNetwork(_, magic) => Network(0x00, magic)
  private def makeBFBackendService(bfConfig: BlockfrostConfig) =
    new BFBackendService(bfConfig.network.blockfrostURL + "/", bfConfig.token)
  def makeAccount(bfConfig: BlockfrostConfig, wallet: CardanoWalletConfig): Account =
    new Account(makeBFNetworks(bfConfig.network), wallet.mnemonicPhrase)
  def makeAccount(network: PublicCardanoNetwork, wallet: CardanoWalletConfig): Account =
    new Account(makeBFNetworks(network), wallet.mnemonicPhrase)

  // Return the hash/id of the transaction
  def submitTransaction(tx: Transaction): ZIO[BlockfrostConfig, Throwable, TxHash] =
    for {
      _ <- ZIO.log("submitTransaction")
      bfConfig <- ZIO.service[BlockfrostConfig]
      backendService: BackendService = makeBFBackendService(bfConfig)
      txPayload = tx.serialize()
      _ <- ZIO.log(s"submitTransaction txPayload = ${bytes2Hex(tx.serialize())}")
      result <- ZIO.attempt(backendService.getTransactionService().submitTransaction(txPayload))
      _ <- ZIO.log(s"submitTransaction result = ${result.toString}")
      _ <- ZIO.log(s"See https://${bfConfig.network.name}.cardanoscan.io/transaction/${result.getValue}?tab=metadata")
      // TODO If result.code(),  <= 200 < 300 return error
    } yield TxHash.fromHex(result.getValue)

  def addressesTotalAda(address: String): ZIO[BlockfrostConfig, Throwable, BigDecimal] =
    for {
      _ <- ZIO.log(s"addressesTotal for $address")
      backendService: BackendService <- ZIO.service[BlockfrostConfig].map(makeBFBackendService(_))
      result <- ZIO.attempt(backendService.getAddressService().getAddressDetails(address))
      _ <- ZIO.log(s"addressesTotal result = ${result.toString}")
      receivedSum = result.getValue
        .getReceivedSum()
        .asScala
        .map(e => BigInt(e.getQuantity))
        .fold(BigInt(0))((a, b) => a + b)
      sentSum = result.getValue
        .getSentSum()
        .asScala
        .map(e => BigInt(e.getQuantity))
        .fold(BigInt(0))((a, b) => a + b)
      total = BigDecimal(receivedSum - sentSum) / BigDecimal(1000000)
      _ <- ZIO.log("total ADA found in address")
    } yield total
}
