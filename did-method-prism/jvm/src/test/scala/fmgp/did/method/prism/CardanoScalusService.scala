package fmgp.prism

import munit.FunSuite
import scalus.cardano.address.Address
import scalus.cardano.address.Address.{addr, stake}
import scalus.cardano.ledger.*
import scalus.cardano.ledger.AuxiliaryData.Metadata
import scalus.cardano.node.Emulator
import scalus.compiler.*
import scalus.uplc.PlutusV3
import scalus.uplc.eval.PlutusVM
import scalus.uplc.transform.V3Optimizer
import scalus.uplc.builtin.ByteString
import scalus.uplc.builtin.Data

import scalus.utils.await

import fmgp.did.method.prism.TestPeer.{Alice, Bob}
import scalus.cardano.txbuilder.TxBuilder

/** didResolverPrismJVM/testOnly fmgp.prism.CardanoScalusService */
class CardanoScalusService extends FunSuite {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val env: CardanoInfo = CardanoInfo.mainnet
  // Common test values
  val genesisHash: TransactionHash =
    TransactionHash.fromByteString(ByteString.fromHex("0" * 64))

  // Helper methods for creating UTXOs
  def input(index: Int): TransactionInput = Input(genesisHash, index)
  def adaOutput(address: Address, ada: Int): TransactionOutput =
    TransactionOutput(address, Value.ada(ada))

  val alwaysOkScript: Script.PlutusV3 = {
    val alwaysOk = PlutusV3.compile((sc: Data) => ())(using Options.default)
    alwaysOk.script
  }

  val emulator = Emulator(
    Map(
      input(0) -> adaOutput(Alice.address, 100),
      input(1) -> adaOutput(Alice.address, 50),
      input(2) -> adaOutput(Alice.address, 50)
    )
  )

  test("Building a simple transaction") {
    // Simple tx, using magic `complete()`
    val tx = TxBuilder(env)
      .payTo(Bob.address, Value.ada(10))
      // Magic: sets inputs, collateral input/output, execution budgets, fee, handle change, etc.
      .complete(reader = emulator, sponsor = Alice.address)
      .map(_.sign(Alice.signer))
      .map(_.transaction)
      .await()

    pprint.pprintln(tx.body.value)
    pprint.pprintln(tx.witnessSet)
  }

  test("Building a simple transaction with Metadata") {

    val meta = AuxiliaryData.Metadata(Map(Word64.fromUnsignedInt(999) -> Metadatum.Int(123)))
    val tx = TxBuilder(env)
      .payTo(Bob.address, Value.ada(10))
      .metadata(meta)
      // Magic: sets inputs, collateral input/output, execution budgets,
      // fee, handle change, etc.
      .complete(reader = emulator, sponsor = Alice.address)
      .map(_.sign(Alice.signer))
      .map(_.transaction)
      .await()

    pprint.pprintln(tx.auxiliaryData)
    pprint.pprintln(tx.body.value)
    pprint.pprintln(tx.witnessSet)
  }

//   test("CardanoScalusService") {
//     // val env: Environment = Environment()
//     // val wallet: Wallet = Wallet.empty(Address)

//     val network: PublicCardanoNetwork = PublicCardanoNetwork.Preprod
//     val wallet: CardanoWalletConfig = CardanoWalletConfig()
//     val account = CardanoService.makeAccount(network, wallet)
//     val address = Address.fromString(account.baseAddress)
//     println(address)

//     val testProtocolParams = CardanoInfo.mainnet.protocolParams
//     val testEvaluator = PlutusScriptEvaluator(
//       slotConfig = SlotConfig.preprod,
//       initialBudget = ExBudget.enormous,
//       protocolMajorVersion = MajorProtocolVersion.plominPV,
//       costModels = testProtocolParams.costModels
//     )

//     // curl 'https://cardano-preprod.blockfrost.io/api/v0/addresses/addr_test1qq998yc0cz9fdjqy72dzl4runargt08x7rwah4pl36fhnk25mghzay44ttnqt65ezmff35cqmfyp0ugjxxczw3d97vesgfgdmq/utxos' --header 'project_id: preprod9EGSSMf6oWb81qoi8eW65iWaQuHJ1HwB'
// // [
// //   {
// //     "address": "addr_test1qq998yc0cz9fdjqy72dzl4runargt08x7rwah4pl36fhnk25mghzay44ttnqt65ezmff35cqmfyp0ugjxxczw3d97vesgfgdmq",
// //     "tx_hash": "4bac097a333d6a074dbd41ee77e5360338eed8f6dfb476252e4b2ecf7dea8040",
// //     "tx_index": 0,
// //     "output_index": 0,
// //     "amount": [
// //       {
// //         "unit": "lovelace",
// //         "quantity": "9984302759"
// //       }
// //     ],
// //     "block": "3e5d837b5336c843a45270ac7e0acfa8642733f8b7fb681c4cd962183ed447a4",
// //     "data_hash": null,
// //     "inline_datum": null,
// //     "reference_script_hash": null
// //   }
// // ]

//     val input = TransactionInput.apply(
//       TransactionHash.fromHex("4bac097a333d6a074dbd41ee77e5360338eed8f6dfb476252e4b2ecf7dea8040"),
//       0
//     )

//     val output = TransactionOutput.apply(
//       address = address,
//       value = Value.ada(1) // lovelace(1) // inputs.foldLeft(Value.zero)((acc, o) => acc + o.output.value)
//     )

//     val steps = Seq(
//       TransactionBuilderStep.Spend(
//         utxo = Utxo(input, output),
//         witness = PubKeyWitness
//       ),
//       TransactionBuilderStep.ModifyAuxiliaryData(a => a),
//       // TransactionBuilderStep.Fee
//       TransactionBuilderStep.Send(output),
//     )

//     def metadata(md: Option[AuxiliaryData]): Option[AuxiliaryData] =
//       md match
//         case Some(value) => ???
//         case None        => Some(AuxiliaryData.Metadata(Map(Word64(PRISM_LABEL_CIP_10) -> Metadatum.Text("test"))))

//     val stage1 = TransactionBuilder.build(Network.Testnet, steps)

//     stage1 match
//       case Left(value)    => println("ERROR: " + value)
//       case Right(context) => {
//         println("Context: " + context)

//         println(context.transaction.isValid)
//         dumpTx(context.transaction)

//         println("expectedSigners: " + context.expectedSigners)
//         context.balance(
//           Change.changeOutputDiffHandler(???, ???, testProtocolParams, 1)
//           // ChangeOutputDiffHandler(, 1).changeOutputDiffHandler, // TransactionBuilder
//           testProtocolParams,
//           testEvaluator
//         ) match
//           case Left(value)  => println("ERROR balance: " + value)
//           case Right(value) => println("Context balance: " + value)

//         // val feeUtxo = TransactionUnspentOutput(
//         //   TransactionInput(
//         //     TransactionHash.fromHex("4bac097a333d6a074dbd41ee77e5360338eed8f6dfb476252e4b2ecf7dea8040"),
//         //     0
//         //   ),
//         //   TransactionOutput.apply(
//         //     address = address,
//         //     value = Value.lovelace(1) // inputs.foldLeft(Value.zero)((acc, o) => acc + o.output.value)
//         //   )
//         // )

//         // val stage2 = TransactionBuilder.modify(
//         //   context,
//         //   Seq(
//         //     TransactionBuilderStep.Spend(feeUtxo, PubKeyWitness),
//         //     TransactionBuilderStep.Send(feeUtxo.output)
//         //   )
//         // )

//         // stage2 match
//         //   case Left(value)     => println("ERROR: " + value)
//         //   case Right(context2) => {
//         //     println("Context2: " + context2)
//         //   }
//       }

//     // BuilderContext(env, wallet)

//   }

//   import scala.sys.process.*
//   private def dumpTx(transaction: Transaction): Unit = {
//     val cborHex = HexUtil.encodeHexString(transaction.toCbor)
//     println(s"CBOR Hex: $cborHex")
//     dumpCborDiag(cborHex)
//   }

//   private def dumpCborDiag(cborHex: String): Unit = {
//     try {
//       // val result = (s"echo $cborHex" #| "/home/euonymos/.cargo/bin/cbor-diag").!!
//       println
//       println("Diagnostic notation:") // FIXME
//       println(cborHex)
//     } catch {
//       case e: Exception =>
//         println(s"Failed to run cbor-diag: ${e.getMessage}")
//     }
//   }

//   private def dumpCtx(ctx: TransactionBuilder.Context): Unit = {
//     val indentedPrinter = pprint.PPrinter(defaultIndent = 4)

//     println("Context.expectedSigners:")
//     indentedPrinter.pprintln(ctx.expectedSigners)
//     // println("Context.resolvedUtxos:")
//     // indentedPrinter.pprintln(ctx.resolvedUtxos)    }
//   }

}
