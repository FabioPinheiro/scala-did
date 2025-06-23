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
  val refVDR = RefVDR("2a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7b")

  val createSSI =
    "0a076d61737465723112473045022100fd1f2ea66ea9e7f37861dbe1599fb12b7ca3297e9efa872504bfc54f1daebec502205f0152d45b266b5524d2fe8eb38aaa1d3e78dc053b4f50d98fe4564f50c4c0da1a7b0a790a77123b0a076d61737465723110014a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f12380a047664723110084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f"
  val updateSSI_addKey =
    "0a076d61737465723112463044022053718b0ed7499ea6c39d76f3b88474cc682b4cc0b984047c9f3a6abd690842f502201aaa4ef7f61bdebb6235c810df4129b52cf538077d9b67886768a39710fc093a1aaf0112ac010a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4124a6469643a707269736d3a353164343762313333393361376363356331616663343730393964636265636363663063386137303832386330373261633832663535323235623432643466341a3c0a3a0a380a047664723210084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f"

  val createVDR = // Create Bytes data1
    "0a0476647231124730450221008aa8a6f66a57d28798b24540dbf0a93772ff317f7bd8969a0ca3a98cec7ff9d4022016336c0c8b9fc82198b661f85468f53c1b4dc0cdd23b44dc0d17853aa50944161a283a260a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4a2060101"
  val updateVDR = // Update Bytes data2
    "0a047664723112473045022100d07451415fecbe92f270ecd0371ee181cabd198e781759287e5122d688a9c97102202273b49e43a1f2022940f48012b5e9839f99eaacf7429c417ff67ec64e58b51b1a2a422812202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7ba20603020304"
  val updateVDR_withUnknownField49 = // Update Bytes data2
    "0a04766472311246304402206e6152ba20935eb946e79b4d39cd6cf327b162b25d37b5cc2e4df3ce9e44dd9f0220729a0891b714e6116838cf4dd2dca3e877b6e36a25814e7400fd35cb1da6084a1a2d422b12202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7ba206030203048a0300"
  val updateVDR_withUnknownField99 = // Update Bytes data2
    "0a047664723112463044022008ce25d2a5731b5946df3c57c3d4046a499830603680288b7c3c14d0b6e3d12302205c9f064f0a488e86377b0112448316827a6d356884e218a148db3d3198bf7c051a2d422b12202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7ba206030203049a0600"
  val updateVDR_withTheNewKey = // Update Bytes data2
    "0a047664723212473045022100b33c0b12449eb506c862c98cfbd221d48cbf31ba62e512b4af31170a34a62e2a0220329bbc57107491b62ecfb894cda5757cc221421ecccae61a7404d3d0ae1d01d41a2842261220d7c38f7d8aa4912d0a58ead87b154eed968b949b3bfb54f3f894ab9fc5365f40a2060105"

  val data1 = hex2bytes("01")
  val data2 = hex2bytes("020304")
  val data3 = hex2bytes("05")

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
    val (vdr, e2) = createVDREntry(didPrism, pk1VDR, "vdr1", data1)
    assertEquals(vdr, refVDR)
    assertEquals(bytes2Hex(e2.toByteArray), createVDR)
  }
  test("update VDR") {
    val e3 = updateVDREntry(refVDR, getSignedPrismOperationFromHex(createVDR), pk1VDR, "vdr1", data2)
    assertEquals(bytes2Hex(e3.toByteArray), updateVDR)
  }

  test("update VDR WithUnknownFields 49") {
    val e3x = updateVDREntryWithUnknownFields(
      refVDR,
      getSignedPrismOperationFromHex(createVDR),
      pk1VDR,
      "vdr1",
      data2,
      unknownFieldNumber = 49
    )
    assertEquals(bytes2Hex(e3x.toByteArray), updateVDR_withUnknownField49)
  }

  test("update VDR WithUnknownFields 99") {
    val e3x = updateVDREntryWithUnknownFields(
      refVDR,
      getSignedPrismOperationFromHex(createVDR),
      pk1VDR,
      "vdr1",
      data2,
      unknownFieldNumber = 99
    )
    assertEquals(bytes2Hex(e3x.toByteArray), updateVDR_withUnknownField99)
  }

  test("update VDR after add key in the SSI") {
    val e5 = updateVDREntry(refVDR, getSignedPrismOperationFromHex(updateVDR), pk2VDR, "vdr2", data3)
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
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data1.toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  testZ("Index SSI and a create and update VDR Events") {
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
      _ = assertEquals(vdr.unsupportedValidationField, false)
      _ = vdr.data match
        case DataEmpty()              => fail("Wrong DATA type")
        case DataDeactivated(data)    => fail("Wrong DATA type")
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data2.toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  testZ("Index SSI and a create and update (with UnknownField 49) VDR Events") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR_withUnknownField49)
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
      _ = assertEquals(vdr.unsupportedValidationField, true) // Because of field 49 (3 <= _ <= 49)
      _ = vdr.data match
        case DataEmpty()              => fail("Wrong DATA type")
        case DataDeactivated(data)    => fail("Wrong DATA type")
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data2.toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  testZ("Index SSI and a create and update (with UnknownField 99) VDR Events") {
    for {
      stateRef <- Ref.make(PrismState.empty)
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR_withUnknownField99)
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
      _ = assertEquals(vdr.unsupportedValidationField, false)
      _ = vdr.data match
        case DataEmpty()              => fail("Wrong DATA type")
        case DataDeactivated(data)    => fail("Wrong DATA type")
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data2.toSeq)
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
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data1.toSeq)
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
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data3.toSeq)
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
        case DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data2.toSeq)
        case DataIPFS(cid)            => fail("Wrong DATA type")
        case DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  // TODO test update event with a different data type like IPFS
  // TODO test create event with a reserver field for future implementation
  // TODO test update event with a reserver field for future implementation
}
