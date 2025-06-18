package fmgp.did.method.prism

import munit._
import zio._
import zio.json._
import zio.stream._

import fmgp.did.method.prism.indexer.IndexerUtils
import fmgp.did.method.prism.proto.MaybeOperation
import _root_.proto.prism._
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import fmgp.did.method.prism.VDR.DataEmpty
import fmgp.did.method.prism.VDR.DataDeactivated
import fmgp.did.method.prism.VDR.DataByteArray
import fmgp.did.method.prism.VDR.DataIPFS
import fmgp.did.method.prism.VDR.DataStatusList
import fmgp.did.method.prism.proto.MySignedPrismOperation
import fmgp.crypto.SHA256

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.IndexerSuite
  */
class IndexerSuite extends ZSuite {

  import KeyConstanceUtils._
  import PrismTestUtils._

  val didPrism = DIDPrism("51d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4")
  val refVDR = RefVDR("3ade633ab371f00687b9e23431d10b9dc1943484d364c48608d5c1a985357a3b")

  val createSSI =
    "0a076d61737465723112473045022100fd1f2ea66ea9e7f37861dbe1599fb12b7ca3297e9efa872504bfc54f1daebec502205f0152d45b266b5524d2fe8eb38aaa1d3e78dc053b4f50d98fe4564f50c4c0da1a7b0a790a77123b0a076d61737465723110014a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f12380a047664723110084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f"
  val updateSSI_addKey =
    "0a076d61737465723112463044022053718b0ed7499ea6c39d76f3b88474cc682b4cc0b984047c9f3a6abd690842f502201aaa4ef7f61bdebb6235c810df4129b52cf538077d9b67886768a39710fc093a1aaf0112ac010a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4124a6469643a707269736d3a353164343762313333393361376363356331616663343730393964636265636363663063386137303832386330373261633832663535323235623432643466341a3c0a3a0a380a047664723210084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f"

  val createVDR = // Create Bytes 00ff11
    "0a0476647231124730450221008b7d8eab69f8fe25c496d04545a0c87c1869de12fcd77e2be6746286c499858902200f5351773a4720f5ece5ff60f7912f67ac82d3f999a0772ff8477ec1fce1d4621a293a270a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4520300ff11"
  val updateVDR = // Update Bytes 3300ffcc
    "0a04766472311246304402206973afd6b82a1f94a952d279310c5ba3e1afc8462104506c0e5299df49268b9d02202c5c250a82288e5f392261014167bac8b61ca9d4173b0f7953386e8cb389ca041a2a42280a203ade633ab371f00687b9e23431d10b9dc1943484d364c48608d5c1a985357a3b52043300ffcc"
  val updateVDR_withTheNewKey = // Update Bytes aa0a
    "0a047664723212463044022062a9435247dc16516a75ab49f2c6ae6e1aa7e29e25f9bbe43159dd599b3dd1940220361cc4e51c950b05e0bf6a59bd6d427dcb89dc6275e63e14a34c2a9cda73219a1a2842260a201276c592cac3997ca07fc32424586d7a19721ae6a00cdfe2990212b840537df85202aa0a"

  private def getSignedPrismOperationFromHex(hex: String) =
    SignedPrismOperation.parseFrom(hex2bytes(hex))

  test("create SSI") {
    val (did, e1) = createDID(Seq(("master1", pkMaster)), Seq(("vdr1", pk1VDR)))
    assertEquals(did, didPrism)
    assertEquals(bytes2Hex(e1.toByteArray), createSSI)
  }
  test("update SSI") {
    val e4 = updateDIDAddKey(
      didPrism = didPrism,
      previousOperation = getSignedPrismOperationFromHex(createSSI),
      masterKeyName = "master1",
      masterKey = pkMaster,
      vdrKeyName = "vdr2",
      vdrKey = pk2VDR,
    )
    assertEquals(bytes2Hex(e4.toByteArray), updateSSI_addKey)
  }
  test("create VDR") {
    val (vdr, e2) = createVDREntry(didPrism, pk1VDR, "vdr1", hex2bytes("00ff11"))
    assertEquals(vdr, refVDR)
    assertEquals(bytes2Hex(e2.toByteArray), createVDR)
  }
  test("update VDR") {
    val e3 = updateVDREntry(refVDR, getSignedPrismOperationFromHex(createVDR), pk1VDR, "vdr1", hex2bytes("3300ffcc"))
    assertEquals(bytes2Hex(e3.toByteArray), updateVDR)
  }

  test("update VDR after add key in the SSI") {
    val e5 = updateVDREntry(refVDR, getSignedPrismOperationFromHex(updateVDR), pk2VDR, "vdr2", hex2bytes("aa0a"))
    assertEquals(bytes2Hex(e5.toByteArray), updateVDR_withTheNewKey)
  }

  testZ("Index SSI and the VDR create event") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismOperation.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeOperation.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
        .provideEnvironment(ZEnvironment(stateRef))
      state <- stateRef.get
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 1))
      vdr <- state.getVDR(refVDR)
      _ = vdr.data match
        case DataEmpty()              => fail("Wrong DATA type")
        case DataDeactivated(data)    => fail("Wrong DATA type")
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, hex2bytes("00ff11").toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  testZ("Index SSI and two VDR Events") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismOperation.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeOperation.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
        .provideEnvironment(ZEnvironment(stateRef))
      state <- stateRef.get
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 2))
      vdr <- state.getVDR(refVDR)
      _ = vdr.data match
        case DataEmpty()              => fail("Wrong DATA type")
        case DataDeactivated(data)    => fail("Wrong DATA type")
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, hex2bytes("3300ffcc").toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  testZ("Index SSI and two VDR Events out of order") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      stream = ZStream.fromIterable(
        Seq(createSSI, updateVDR, createVDR)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismOperation.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeOperation.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
        .provideEnvironment(ZEnvironment(stateRef))
      state <- stateRef.get
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 1))
      vdr <- state.getVDR(refVDR)
      _ = vdr.data match
        case DataEmpty()              => fail("Wrong DATA type")
        case DataDeactivated(data)    => fail("Wrong DATA type")
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, hex2bytes("00ff11").toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  testZ("Index SSI (with a create and update events)") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      stream = ZStream.fromIterable(
        Seq(createSSI, updateSSI_addKey)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismOperation.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeOperation.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
        .provideEnvironment(ZEnvironment(stateRef))
      state <- stateRef.get
      // _ = println(state.asInstanceOf[PrismStateInMemory].toJsonPretty)
      _ <- state.getSSIHistory(didPrism).map { didHistory =>
        // println(didHistory.toJsonPretty)
        assertEquals(didHistory.versions.size, 2)
      }

    } yield ()
  }

  testZ("Index SSI (create and add new key) and three VDR events (using the new key)") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR, updateSSI_addKey, updateVDR_withTheNewKey)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismOperation.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeOperation.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
        .provideEnvironment(ZEnvironment(stateRef))
      state <- stateRef.get
      _ <- state.getSSIHistory(didPrism).map { didHistory =>
        assertEquals(didHistory.versions.size, 2)
      }
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 3))
      vdr <- state.getVDR(refVDR)
      _ = vdr.data match
        case DataEmpty()              => fail("Wrong DATA type")
        case DataDeactivated(data)    => fail("Wrong DATA type")
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, hex2bytes("aa0a").toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  testZ("Index SSI (create and add new key) and three VDR events (using the new key before adding the key)") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR, updateVDR_withTheNewKey, updateSSI_addKey)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismOperation.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeOperation.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
        .provideEnvironment(ZEnvironment(stateRef))
      state <- stateRef.get
      _ <- state.getSSIHistory(didPrism).map { didHistory =>
        assertEquals(didHistory.versions.size, 2)
      }
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 3))
      vdr <- state.getVDR(refVDR)

      // _ = println(vdr.toJsonPretty)
      // _ = println(state.asInstanceOf[PrismStateInMemory].toJsonPretty)

      _ = vdr.data match
        case DataEmpty()              => fail("Wrong DATA type")
        case DataDeactivated(data)    => fail("Wrong DATA type")
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, hex2bytes("3300ffcc").toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }
}
