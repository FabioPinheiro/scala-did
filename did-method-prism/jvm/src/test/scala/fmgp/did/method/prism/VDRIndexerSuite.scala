package fmgp.did.method.prism

import munit._
import zio._
import zio.json._
import zio.stream._

import fmgp.did.method.prism.indexer.IndexerUtils
import fmgp.did.method.prism.proto.MaybeOperation
import _root_.proto.prism._
import fmgp.util.hex2bytes

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.VDRIndexerSuite
  *
  * Test data created with didResolverPrismJVM/Test/runMain fmgp.did.method.prism.MainVDR
  */
class VDRIndexerSuite extends ZSuite {

  val createSSI =
    "0a076d61737465723112473045022100fd1f2ea66ea9e7f37861dbe1599fb12b7ca3297e9efa872504bfc54f1daebec502205f0152d45b266b5524d2fe8eb38aaa1d3e78dc053b4f50d98fe4564f50c4c0da1a7b0a790a77123b0a076d61737465723110014a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f12380a047664723110084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f"
  val createVDR = // Create Bytes 00ff11
    "0a0476647231124730450221008b7d8eab69f8fe25c496d04545a0c87c1869de12fcd77e2be6746286c499858902200f5351773a4720f5ece5ff60f7912f67ac82d3f999a0772ff8477ec1fce1d4621a293a270a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4520300ff11"
  val updateVDR = // Update Bytes 3300ffcc
    "0a04766472311246304402206973afd6b82a1f94a952d279310c5ba3e1afc8462104506c0e5299df49268b9d02202c5c250a82288e5f392261014167bac8b61ca9d4173b0f7953386e8cb389ca041a2a42280a203ade633ab371f00687b9e23431d10b9dc1943484d364c48608d5c1a985357a3b52043300ffcc"

  testZ("Index SSI and two VDR Evetns") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      _ <- ZIO.log("test")
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismOperation.parseFrom(protoBytes))
          //   .map(protoBytes => PrismObject.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeOperation.fromProto(proto, "tx", 0, index))
      )

      job = stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
        .provideEnvironment(ZEnvironment(stateRef))
      _ <- job

      state <- stateRef.get
      _ = println(state.asInstanceOf[PrismStateInMemory].toJsonPretty)
    } yield ()

  }
}
