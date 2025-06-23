package fmgp.did.method.prism

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
import scala.collection.StepperShape
import fmgp.blockfrost.*
import fmgp.did.method.prism.cardano._
import _root_.proto.prism.PrismBlock
import _root_.proto.prism.PrismOperation
import _root_.proto.prism.SignedPrismOperation
import fmgp.util._
import fmgp.did.method.prism.proto.MySignedPrismOperation
import _root_.proto.prism.PrismObject

object CardanoService {

  object internal {
    import fmgp.did.method.prism.cardano.PRISM_LABEL_CIP_10
    import fmgp.did.method.prism.cardano.MSG_LABEL_CIP_20

    // FIXME
    def network = Networks.preprod()
    def networkURL = Network.Preprod + "/" // Constants.BLOCKFROST_PREPROD_URL
    def networkBlockfostToken = "preprod9EGSSMf6oWb81qoi8eW65iWaQuHJ1HwB" // FIXME recreate my personal IOHK API key

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

  def makeTxBuilder(mnemonic: Seq[String], metadata: Metadata): TxBuilder = {
    val senderAccount: Account = Account(internal.network, CardanoWalletConfig().mnemonicPhrase)
    val output = internal.makeOutput(senderAccount)
    internal.txBuilder(senderAccount, output, metadata)
  }

  def makeTxBuilder(mnemonic: Seq[String], prismObject: PrismObject): TxBuilder =
    makeTxBuilder(mnemonic, makeMetadataPrismWithCIP20(prismObject))

  def makeTxBuilder(mnemonic: Seq[String], prismEvents: Seq[SignedPrismOperation]): TxBuilder =
    makeTxBuilder(mnemonic, PrismObject(blockContent = Some(PrismBlock(operations = prismEvents))))

  def makeTrasation(mnemonic: Seq[String], prismEvents: Seq[SignedPrismOperation]): Transaction = {
    val senderAccount: Account = Account(internal.network, CardanoWalletConfig().mnemonicPhrase)

    val backendService: BackendService = new BFBackendService(internal.networkURL, internal.networkBlockfostToken)
    val utxoSupplier: UtxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService())
    val protocolParamsSupplier: ProtocolParamsSupplier =
      new DefaultProtocolParamsSupplier(backendService.getEpochService())
    val txBuilder = makeTxBuilder(mnemonic, prismEvents)

    // Build and sign the transaction
    val signedTransaction: Transaction = TxBuilderContext
      .init(utxoSupplier, protocolParamsSupplier)
      .buildAndSign(txBuilder, signerFrom(senderAccount))
    signedTransaction
  }

  def submitTransaction(tx: Transaction) =
    for {
      _ <- ZIO.log("submitTransaction")
      backendService: BackendService = new BFBackendService(internal.networkURL, internal.networkBlockfostToken)
      txPayload = tx.serialize()
      _ <- ZIO.log(s"submitTransaction txPayload = ${bytes2Hex(tx.serialize())}")
      result <- ZIO.attempt(backendService.getTransactionService().submitTransaction(txPayload))
      _ <- ZIO.log(s"submitTransaction result = ${result.toString}")
    } yield (result.code(), result.getResponse())

}
