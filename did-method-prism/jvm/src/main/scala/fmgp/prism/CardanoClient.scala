package fmgp.prism

import zio._

import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier
import com.bloxbean.cardano.client.api.UtxoSupplier
import com.bloxbean.cardano.client.api.model.Result
import com.bloxbean.cardano.client.backend.api.BackendService
import com.bloxbean.cardano.client.backend.api.DefaultProtocolParamsSupplier
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier
import com.bloxbean.cardano.client.backend.blockfrost.common.Constants
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata
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
import fmgp.util.bytes2Hex
import com.bloxbean.cardano.client.function.TxOutputBuilder
import com.bloxbean.cardano.client.metadata._
import com.bloxbean.cardano.client.metadata.cbor._
import java.math.BigInteger
import scala.collection.StepperShape

/** https://cardano-client.dev/docs/gettingstarted/simple-transfer */
object CardanoClient extends ZIOAppDefault {

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    for {
      _ <- Console.printLine(
        """██████╗ ██████╗ ██╗███████╗███╗   ███╗    ██╗   ██╗██████╗ ██████╗ 
          |██╔══██╗██╔══██╗██║██╔════╝████╗ ████║    ██║   ██║██╔══██╗██╔══██╗
          |██████╔╝██████╔╝██║███████╗██╔████╔██║    ██║   ██║██║  ██║██████╔╝
          |██╔═══╝ ██╔══██╗██║╚════██║██║╚██╔╝██║    ╚██╗ ██╔╝██║  ██║██╔══██╗
          |██║     ██║  ██║██║███████║██║ ╚═╝ ██║     ╚████╔╝ ██████╔╝██║  ██║
          |╚═╝     ╚═╝  ╚═╝╚═╝╚══════╝╚═╝     ╚═╝      ╚═══╝  ╚═════╝ ╚═╝  ╚═╝
          |PRISM - Verifiable Data Registry (VDR)
          |Vist: https://github.com/FabioPinheiro/scala-did
          |
          |""".stripMargin
      )
      nemonic =
        "mention side album physical uncle lab " +
          "horn nasty script few hazard announce " +
          "upon group ten moment fantasy helmet " +
          "supreme early gadget curve lecture edge"
      // aaa = com.bloxbean.cardano.client.crypto.bip39.MnemonicCode .toSeed(nemonic)

      senderAccount: Account = Account(Networks.preprod(), nemonic)
      _ <- Console.printLine("baseAddress: " + senderAccount.hdKeyPair())
      _ <- Console.printLine("baseAddress: " + senderAccount.baseAddress)
      // addr_test1qq998yc0cz9fdjqy72dzl4runargt08x7rwah4pl36fhnk25mghzay44ttnqt65ezmff35cqmfyp0ugjxxczw3d97vesgfgdmq
      // https://docs.cardano.org/cardano-testnets/tools/faucet -> 51dd7dd271396775d1d210935a6a01e664fcda92a780226be8e183ef70e325f7
      // https://preprod.cardanoscan.io/transaction/51dd7dd271396775d1d210935a6a01e664fcda92a780226be8e183ef70e325f7
      // https://preprod.cardanoscan.io/address/000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f333

      backendService: BackendService = new BFBackendService(
        // Constants.BLOCKFROST_PREPROD_URL,
        Constants.BLOCKFROST_PREPROD_URL,
        "***************************************"
      )

      // Define expected Outputs
      output1: Output = Output
        .builder()
        .address(senderAccount.baseAddress)
        .assetName(LOVELACE)
        .qty(BigInteger.valueOf(1)) // adaToLovelace(1)
        // .qty(adaToLovelace(1))
        .build();

      metadata = {
        val msgCIP20 = CBORMetadataMap().put("msg", CBORMetadataList().add("PRISM VDR (by fmgp)"))
        val prismBlock = Seq
          .range(1, 100)
          .map(_.toByte)
          .grouped(32) // https://developers.cardano.org/docs/get-started/cardano-serialization-lib/transaction-metadata/#metadata-limitations
          .foldLeft(CBORMetadataList()) { case (acu, seqData) => acu.add(seqData.toArray[Byte]) }

        val prismCBOR =
          CBORMetadataMap()
            .put("v", BigInteger.valueOf(1)) // PRISM version
            .put("c", prismBlock)

        MetadataBuilder
          .createMetadata()
          .put(21324, prismBlock)
          .put(674, msgCIP20) // MessageMetadata.create().add("PRISM VDR")
      }

      // Define TxBuilder
      txBuilder: TxBuilder = output1
        .outputBuilder()
        .buildInputs(createFromSender(senderAccount.baseAddress, senderAccount.baseAddress()))
        .andThen(metadataProvider(metadata))
        .andThen(balanceTx(senderAccount.baseAddress, 1))

      utxoSupplier: UtxoSupplier =
        new DefaultUtxoSupplier(backendService.getUtxoService())
      protocolParamsSupplier: ProtocolParamsSupplier =
        new DefaultProtocolParamsSupplier(backendService.getEpochService())
      // _ <- Console.printLine(protocolParamsSupplier.getProtocolParams())

      // Build and sign the transaction
      signedTransaction: Transaction = TxBuilderContext
        .init(utxoSupplier, protocolParamsSupplier)
        .buildAndSign(txBuilder, signerFrom(senderAccount))

      _ <- Console.printLine(bytes2Hex(signedTransaction.serialize().toArray))
//84a400d90102818258202cd2a326440a8b2461f976d2c283d34c5cbd89923e01988b4eb526b3aa9d94d7000181825839000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f3331b000000025363e82e021a00028de1075820760d2a22dd546c0d0094aa5ac5d3a437ce1cb47fd536241047e77273a67431efa100d90102818258208bdbeed2b2bd78c79db1492fb77fc79c8adeec0fc1a953f90f0198220b6588fc58404bf165c7fe661bfdc27a099acd7b179a9b798bebf33b96028a53e98a0531aa51bac8e9720db0db943e99ccf77f741376fbf494413a937c7bedaf943a27399305f5a119534c81642d2d2d2d

      result = backendService.getTransactionService().submitTransaction(signedTransaction.serialize())
      _ <- Console.printLine(result)
      // _ <- Console.printLine(result.code())
      // _ <- Console.printLine(result.getResponse())
      // _ <- Console.printLine(result.getValue())

    } yield ()

}
