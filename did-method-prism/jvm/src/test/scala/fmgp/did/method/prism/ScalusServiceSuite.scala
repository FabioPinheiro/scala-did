package fmgp.did.method.prism

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
import fmgp.util.*
import fmgp.did.method.prism.cardano.*
import _root_.proto.prism.*

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.ScalusServiceSuite */
class ScalusServiceSuite extends FunSuite {
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

    // pprint.pprintln(tx.body.value)
    // pprint.pprintln(tx.witnessSet)
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

    // pprint.pprintln(tx.auxiliaryData)
    // pprint.pprintln(tx.body.value)
    // pprint.pprintln(tx.witnessSet)
  }

  test("ScalusService Metadata") {

    val prismEvents: Seq[SignedPrismEvent] =
      Seq( // IndexerSuite
        // Create DID - DIDPrism("51d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4")
        "0a076d61737465723112473045022100fd1f2ea66ea9e7f37861dbe1599fb12b7ca3297e9efa872504bfc54f1daebec502205f0152d45b266b5524d2fe8eb38aaa1d3e78dc053b4f50d98fe4564f50c4c0da1a7b0a790a77123b0a076d61737465723110014a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f12380a047664723110084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f",
        // Create VDR - RefVDR("2a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7b") - hex2bytes("01")
        "0a0476647231124730450221008aa8a6f66a57d28798b24540dbf0a93772ff317f7bd8969a0ca3a98cec7ff9d4022016336c0c8b9fc82198b661f85468f53c1b4dc0cdd23b44dc0d17853aa50944161a283a260a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4a2060101",
        // Update VDR - hex2bytes("020304")
        "0a047664723112473045022100d07451415fecbe92f270ecd0371ee181cabd198e781759287e5122d688a9c97102202273b49e43a1f2022940f48012b5e9839f99eaacf7429c417ff67ec64e58b51b1a2a422812202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7ba20603020304",
      )
        .map(hex2bytes(_))
        .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))

    val prismObject = PrismObject(blockContent = Some(PrismBlock(events = prismEvents)))

    val meta = ScalusService.makeMetadataPrism(prismObject)
    pprint.pprintln(meta)

  }

}
