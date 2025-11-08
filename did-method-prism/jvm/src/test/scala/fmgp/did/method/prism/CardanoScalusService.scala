package fmgp.prism

import scalus.*
import scalus.cardano.*
import scalus.cardano.txbuilder.*
import scalus.cardano.address.Address
import fmgp.did.method.prism.cardano.PublicCardanoNetwork
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import scalus.cardano.address.Network
import scalus.cardano.ledger.*
import munit.FunSuite
import fmgp.did.method.prism.CardanoService
import scalus.uplc.eval.ExBudget
import scalus.cardano.txbuilder.LowLevelTxBuilder.ChangeOutputDiffHandler
import fmgp.did.method.prism.cardano.PRISM_LABEL_CIP_10
import com.bloxbean.cardano.client.util.HexUtil

/** didResolverPrismJVM/testOnly fmgp.prism.CardanoScalusService */
class CardanoScalusService extends FunSuite {

  test("CardanoScalusService") {
    // val env: Environment = Environment()
    // val wallet: Wallet = Wallet.empty(Address)

    val network: PublicCardanoNetwork = PublicCardanoNetwork.Preprod
    val wallet: CardanoWalletConfig = CardanoWalletConfig()
    val account = CardanoService.makeAccount(network, wallet)
    val address = Address.fromString(account.baseAddress)
    println(address)

    val testProtocolParams = CardanoInfo.mainnet.protocolParams
    val testEvaluator = PlutusScriptEvaluator(
      slotConfig = SlotConfig.Preprod,
      initialBudget = ExBudget.enormous,
      protocolMajorVersion = MajorProtocolVersion.plominPV,
      costModels = testProtocolParams.costModels
    )

    // curl 'https://cardano-preprod.blockfrost.io/api/v0/addresses/addr_test1qq998yc0cz9fdjqy72dzl4runargt08x7rwah4pl36fhnk25mghzay44ttnqt65ezmff35cqmfyp0ugjxxczw3d97vesgfgdmq/utxos' --header 'project_id: preprod9EGSSMf6oWb81qoi8eW65iWaQuHJ1HwB'
// [
//   {
//     "address": "addr_test1qq998yc0cz9fdjqy72dzl4runargt08x7rwah4pl36fhnk25mghzay44ttnqt65ezmff35cqmfyp0ugjxxczw3d97vesgfgdmq",
//     "tx_hash": "4bac097a333d6a074dbd41ee77e5360338eed8f6dfb476252e4b2ecf7dea8040",
//     "tx_index": 0,
//     "output_index": 0,
//     "amount": [
//       {
//         "unit": "lovelace",
//         "quantity": "9984302759"
//       }
//     ],
//     "block": "3e5d837b5336c843a45270ac7e0acfa8642733f8b7fb681c4cd962183ed447a4",
//     "data_hash": null,
//     "inline_datum": null,
//     "reference_script_hash": null
//   }
// ]

    val input = TransactionInput.apply(
      TransactionHash.fromHex("4bac097a333d6a074dbd41ee77e5360338eed8f6dfb476252e4b2ecf7dea8040"),
      0
    )

    val output = TransactionOutput.apply(
      address = address,
      value = Value.ada(1) // lovelace(1) // inputs.foldLeft(Value.zero)((acc, o) => acc + o.output.value)
    )

    val steps = Seq(
      TransactionBuilderStep.Spend(
        utxo = TransactionUnspentOutput(input, output),
        witness = PubKeyWitness
      ),
      TransactionBuilderStep.ModifyAuxiliaryData(a => a),
      // TransactionBuilderStep.Fee
      TransactionBuilderStep.Send(output),
    )

    def metadata(md: Option[AuxiliaryData]): Option[AuxiliaryData] =
      md match
        case Some(value) => ???
        case None        => Some(AuxiliaryData.Metadata(Map(Word64(PRISM_LABEL_CIP_10) -> Metadatum.Text("test"))))

    val stage1 = TransactionBuilder.build(Network.Testnet, steps)

    stage1 match
      case Left(value)    => println("ERROR: " + value)
      case Right(context) => {
        println("Context: " + context)

        println(context.transaction.isValid)
        dumpTx(context.transaction)

        println("expectedSigners: " + context.expectedSigners)
        context.balance(
          ChangeOutputDiffHandler(testProtocolParams, 1).changeOutputDiffHandler,
          testProtocolParams,
          testEvaluator
        ) match
          case Left(value)  => println("ERROR balance: " + value)
          case Right(value) => println("Context balance: " + value)

        // val feeUtxo = TransactionUnspentOutput(
        //   TransactionInput(
        //     TransactionHash.fromHex("4bac097a333d6a074dbd41ee77e5360338eed8f6dfb476252e4b2ecf7dea8040"),
        //     0
        //   ),
        //   TransactionOutput.apply(
        //     address = address,
        //     value = Value.lovelace(1) // inputs.foldLeft(Value.zero)((acc, o) => acc + o.output.value)
        //   )
        // )

        // val stage2 = TransactionBuilder.modify(
        //   context,
        //   Seq(
        //     TransactionBuilderStep.Spend(feeUtxo, PubKeyWitness),
        //     TransactionBuilderStep.Send(feeUtxo.output)
        //   )
        // )

        // stage2 match
        //   case Left(value)     => println("ERROR: " + value)
        //   case Right(context2) => {
        //     println("Context2: " + context2)
        //   }
      }

    // BuilderContext(env, wallet)

  }

  import scala.sys.process.*
  private def dumpTx(transaction: Transaction): Unit = {
    val cborHex = HexUtil.encodeHexString(transaction.toCbor)
    println(s"CBOR Hex: $cborHex")
    dumpCborDiag(cborHex)
  }

  private def dumpCborDiag(cborHex: String): Unit = {
    try {
      // val result = (s"echo $cborHex" #| "/home/euonymos/.cargo/bin/cbor-diag").!!
      println
      println("Diagnostic notation:") // FIXME
      println(cborHex)
    } catch {
      case e: Exception =>
        println(s"Failed to run cbor-diag: ${e.getMessage}")
    }
  }

  private def dumpCtx(ctx: TransactionBuilder.Context): Unit = {
    val indentedPrinter = pprint.PPrinter(defaultIndent = 4)

    println("Context.expectedSigners:")
    indentedPrinter.pprintln(ctx.expectedSigners)
    // println("Context.resolvedUtxos:")
    // indentedPrinter.pprintln(ctx.resolvedUtxos)    }
  }

}
