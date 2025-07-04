package fmgp.did.method.prism

import zio._

import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.common.model.Networks

import java.math.BigInteger
import scala.collection.StepperShape

import fmgp.blockfrost.*
import fmgp.util._
import fmgp.did.method.prism.cardano._
import fmgp.did.method.prism.vdr._
import _root_.proto.prism.PrismBlock
import _root_.proto.prism.PrismOperation
import _root_.proto.prism.SignedPrismOperation
import _root_.proto.prism.PrismObject

/** https://cardano-client.dev/docs/gettingstarted/simple-transfer
  *
  * didResolverPrismJVM/runMain fmgp.did.method.prism.CardanoClient
  */
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
      // aaa = com.bloxbean.cardano.client.crypto.bip39.MnemonicCode .toSeed(nemonic)

      blockfrastConfig = BlockfrastConfig(
        token = "preprod9EGSSMf6oWb81qoi8eW65iWaQuHJ1HwB"
      ) // FIXME recreate my personal IOHK API key
      wallet = CardanoWalletConfig()
      senderAccount: Account = Account(Networks.preprod(), wallet.mnemonicPhrase)
      _ <- ZIO.log("hdKeyPair: " + senderAccount.hdKeyPair())
      _ <- ZIO.log("baseAddress: " + senderAccount.baseAddress)
      // addr_test1qq998yc0cz9fdjqy72dzl4runargt08x7rwah4pl36fhnk25mghzay44ttnqt65ezmff35cqmfyp0ugjxxczw3d97vesgfgdmq
      // https://docs.cardano.org/cardano-testnets/tools/faucet -> 51dd7dd271396775d1d210935a6a01e664fcda92a780226be8e183ef70e325f7
      // https://preprod.cardanoscan.io/transaction/51dd7dd271396775d1d210935a6a01e664fcda92a780226be8e183ef70e325f7
      // https://preprod.cardanoscan.io/address/000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f333

      prismEvents: Seq[SignedPrismOperation] =
        Seq( // IndexerSuite
          // Create DID - DIDPrism("51d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4")
          "0a076d61737465723112473045022100fd1f2ea66ea9e7f37861dbe1599fb12b7ca3297e9efa872504bfc54f1daebec502205f0152d45b266b5524d2fe8eb38aaa1d3e78dc053b4f50d98fe4564f50c4c0da1a7b0a790a77123b0a076d61737465723110014a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f12380a047664723110084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f",
          // Create VDR - RefVDR("2a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7b") - hex2bytes("01")
          "0a0476647231124730450221008aa8a6f66a57d28798b24540dbf0a93772ff317f7bd8969a0ca3a98cec7ff9d4022016336c0c8b9fc82198b661f85468f53c1b4dc0cdd23b44dc0d17853aa50944161a283a260a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4a2060101",
          // Update VDR - hex2bytes("020304")
          "0a047664723112473045022100d07451415fecbe92f270ecd0371ee181cabd198e781759287e5122d688a9c97102202273b49e43a1f2022940f48012b5e9839f99eaacf7429c417ff67ec64e58b51b1a2a422812202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7ba20603020304",
        )
          .map(hex2bytes(_))
          .map(protoBytes => SignedPrismOperation.parseFrom(protoBytes))

      signedTransaction = CardanoService.makeTrasation(
        blockfrastConfig,
        wallet,
        prismEvents,
        maybeMsgCIP20 = Some("PRISM VDR (by fmgp)")
      )
      _ <- ZIO.log(bytes2Hex(signedTransaction.serialize().toArray))
      // Lixo 84a400d9010281825820571a57b47a6cabad5e91eca710da75e0893312529cee2d2bd1188b7617c38534000181825839000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f3331b0000000253349f34021a0002e5b507582053e55e0ca4b807188e02bde1ded5ed566477cd1e9caea007972abb6318c2d8b9a100d90102818258208bdbeed2b2bd78c79db1492fb77fc79c8adeec0fc1a953f90f0198220b6588fc58405c82f38c39827ddaf2a5f53e7a673b01adabad0b36e2b3934a650d4d9971ddff6bc742d7cd90b6e83813c0fa814aeed001fcbc2d10be374e45fd4c8ac7365c0cf5a21902a273505249534d205644522028627920666d67702919534ca261638f582012cf010a076d61737465723112473045022100fd1f2ea66ea9e7f37861dbe15958209fb12b7ca3297e9efa872504bfc54f1daebec502205f0152d45b266b5524d2fe58208eb38aaa1d3e78dc053b4f50d98fe4564f50c4c0da1a7b0a790a77123b0a076d582061737465723110014a2e0a09736563703235366b311221028210fd4c42b148df58202b908eb6a5c507822f63c440facc283b30c84859fde2e30f12380a0476647231582010084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c5582007822f63c440facc283b30c84859fde2e30f12790a04766472311247304502215820008aa8a6f66a57d28798b24540dbf0a93772ff317f7bd8969a0ca3a98cec7ff95820d4022016336c0c8b9fc82198b661f85468f53c1b4dc0cdd23b44dc0d17853aa558200944161a283a260a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c0758202ac82f55225b42d4f4a2060101127b0a047664723112473045022100d074514158205fecbe92f270ecd0371ee181cabd198e781759287e5122d688a9c971022022735820b49e43a1f2022940f48012b5e9839f99eaacf7429c417ff67ec64e58b51b1a2a5820422812202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca44a272a9a7ba20603020304617601
      // Lixo https://preprod.cardanoscan.io/transaction/74595c9a3b4701b2ad70d0b0537b9e714ffb4c56f6144cb4b9594e2754312219
      // 84a400d901028182582074595c9a3b4701b2ad70d0b0537b9e714ffb4c56f6144cb4b9594e2754312219000181825839000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f3331b000000025331b8fb021a0002e639075820dc518771a85f9396530edcc60a5a566a5fea2b03d1e08794b4c43c224d9a4083a100d90102818258208bdbeed2b2bd78c79db1492fb77fc79c8adeec0fc1a953f90f0198220b6588fc58404408ae6139d05e188cde97e73ee1d8de4b1c14f8da16b019b4218f2f2a7cc53e9025d38f5b21964c4dc18f4732a95a3c5cc3f6853cf279c2557bcee1e999340cf5a21902a273505249534d205644522028627920666d67702919534ca261638f582022ca0312cf010a076d61737465723112473045022100fd1f2ea66ea9e7f378615820dbe1599fb12b7ca3297e9efa872504bfc54f1daebec502205f0152d45b266b55582024d2fe8eb38aaa1d3e78dc053b4f50d98fe4564f50c4c0da1a7b0a790a77123b58200a076d61737465723110014a2e0a09736563703235366b311221028210fd4c425820b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f12380a0476582064723110084a2e0a09736563703235366b311221028210fd4c42b148df2b908e5820b6a5c507822f63c440facc283b30c84859fde2e30f12790a04766472311247305820450221008aa8a6f66a57d28798b24540dbf0a93772ff317f7bd8969a0ca3a98c5820ec7ff9d4022016336c0c8b9fc82198b661f85468f53c1b4dc0cdd23b44dc0d175820853aa50944161a283a260a2051d47b13393a7cc5c1afc47099dcbecccf0c8a705820828c072ac82f55225b42d4f4a2060101127b0a047664723112473045022100d058207451415fecbe92f270ecd0371ee181cabd198e781759287e5122d688a9c971025820202273b49e43a1f2022940f48012b5e9839f99eaacf7429c417ff67ec64e58b558201b1a2a422812202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c94dc59ca4272a9a7ba20603020304617601
      // Lixo https://preprod.cardanoscan.io/transaction/e10e33b55e6946d7d8d1d0748d304384435a5cccd821805ff77ad97d2e575af0

      result <- CardanoService.submitTransaction(signedTransaction).provide(ZLayer.succeed(blockfrastConfig))
      _ <- ZIO.log(result.toString())
    } yield ()

}
