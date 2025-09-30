package fmgp.did.method.prism

import munit._
import zio._
import zio.json._
import zio.stream._

import fmgp.did.method.prism.vdr.IndexerUtils
import fmgp.did.method.prism.proto.MaybeEvent
import _root_.proto.prism._
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import fmgp.did.method.prism.proto.MySignedPrismEvent
import fmgp.crypto.SHA256

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.IndexerSuite
  */
class IndexerSuite extends ZSuite {

  import fmgp.did.method.prism.vdr.KeyConstanceUtils._
  import fmgp.did.method.prism.vdr.VDRUtilsTestExtra._

  val prismStateFixture: FunFixture[ULayer[PrismState]] =
    ZTestLocalFixture { _ => PrismStateInMemory.empty.map(s => ZLayer.succeed(s)) }(_ => ZIO.unit)

  prismStateFixture.testZLayered("Index SSI and the VDR create event") {
    for {
      _ <- ZIO.unit
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
      state <- ZIO.service[PrismState]
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 1))
      vdr <- state.getVDR(refVDR)
      _ = vdr.data match
        case VDR.DataEmpty()              => fail("Wrong DATA type")
        case VDR.DataDeactivated(data)    => fail("Wrong DATA type")
        case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data1.toSeq)
        case VDR.DataIPFS(cid)            => fail("Wrong DATA type")
        case VDR.DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  prismStateFixture.testZLayered("Index SSI and a create and update VDR Events") {
    for {
      _ <- ZIO.unit
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
      state <- ZIO.service[PrismState]
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 2))
      vdr <- state.getVDR(refVDR)
      _ = assertEquals(vdr.unsupportedValidationField, false)
      _ = vdr.data match
        case VDR.DataEmpty()              => fail("Wrong DATA type")
        case VDR.DataDeactivated(data)    => fail("Wrong DATA type")
        case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data2.toSeq)
        case VDR.DataIPFS(cid)            => fail("Wrong DATA type")
        case VDR.DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  prismStateFixture.testZLayered("Index SSI and a create and update (with UnknownField 49) VDR Events") {
    for {
      _ <- ZIO.unit
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR_withUnknownField49)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
      state <- ZIO.service[PrismState]
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 2))
      vdr <- state.getVDR(refVDR)
      _ = assertEquals(vdr.unsupportedValidationField, true) // Because of field 49 (3 <= _ <= 49)
      _ = vdr.data match
        case VDR.DataEmpty()              => fail("Wrong DATA type")
        case VDR.DataDeactivated(data)    => fail("Wrong DATA type")
        case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data2.toSeq)
        case VDR.DataIPFS(cid)            => fail("Wrong DATA type")
        case VDR.DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  prismStateFixture.testZLayered("Index SSI and a create and update (with UnknownField 99) VDR Events") {
    for {
      _ <- ZIO.unit
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR_withUnknownField99)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
      state <- ZIO.service[PrismState]
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 2))
      vdr <- state.getVDR(refVDR)
      _ = assertEquals(vdr.unsupportedValidationField, false)
      _ = vdr.data match
        case VDR.DataEmpty()              => fail("Wrong DATA type")
        case VDR.DataDeactivated(data)    => fail("Wrong DATA type")
        case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data2.toSeq)
        case VDR.DataIPFS(cid)            => fail("Wrong DATA type")
        case VDR.DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  prismStateFixture.testZLayered("Index SSI and two VDR Events out of order") {
    for {
      _ <- ZIO.unit
      stream = ZStream.fromIterable(
        Seq(createSSI, updateVDR, createVDR)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
      state <- ZIO.service[PrismState]
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 1))
      vdr <- state.getVDR(refVDR)
      _ = vdr.data match
        case VDR.DataEmpty()              => fail("Wrong DATA type")
        case VDR.DataDeactivated(data)    => fail("Wrong DATA type")
        case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data1.toSeq)
        case VDR.DataIPFS(cid)            => fail("Wrong DATA type")
        case VDR.DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  prismStateFixture.testZLayered("Index SSI (with a create and update events)") {
    for {
      _ <- ZIO.unit
      stream = ZStream.fromIterable(
        Seq(createSSI, updateSSI_addKey)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
      state <- ZIO.service[PrismState]
      // _ = println(state.asInstanceOf[PrismStateInMemory].toJsonPretty)
      _ <- state.getSSIHistory(didPrism).map { didHistory =>
        // println(didHistory.toJsonPretty)
        assertEquals(didHistory.versions.size, 2)
      }

    } yield ()
  }

  prismStateFixture.testZLayered("Index SSI (create and add new key) and three VDR events (using the new key)") {
    for {
      _ <- ZIO.unit
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR, updateSSI_addKey, updateVDR_withTheNewKey)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
      state <- ZIO.service[PrismState]
      _ <- state.getSSIHistory(didPrism).map { didHistory =>
        assertEquals(didHistory.versions.size, 2)
      }
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 3))
      vdr <- state.getVDR(refVDR)
      _ = vdr.data match
        case VDR.DataEmpty()              => fail("Wrong DATA type")
        case VDR.DataDeactivated(data)    => fail("Wrong DATA type")
        case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data3.toSeq)
        case VDR.DataIPFS(cid)            => fail("Wrong DATA type")
        case VDR.DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  prismStateFixture.testZLayered(
    "Index SSI (create and add new key) and three VDR events (using the new key before adding the key)"
  ) {
    for {
      _ <- ZIO.unit
      stream = ZStream.fromIterable(
        Seq(createSSI, createVDR, updateVDR, updateVDR_withTheNewKey, updateSSI_addKey)
          .map(e => hex2bytes(e))
          .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
          .zipWithIndex
          .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
      )
      _ <- stream
        .via(IndexerUtils.pipelinePrismState)
        .run(ZSink.count)
      state <- ZIO.service[PrismState]
      _ <- state.getSSIHistory(didPrism).map { didHistory =>
        assertEquals(didHistory.versions.size, 2)
      }
      _ <- state.getEventsForVDR(refVDR).map(e => assertEquals(e.size, 3))
      vdr <- state.getVDR(refVDR)

      // _ = println(vdr.toJsonPretty)
      // _ = println(state.asInstanceOf[PrismStateInMemory].toJsonPretty)

      _ = vdr.data match
        case VDR.DataEmpty()              => fail("Wrong DATA type")
        case VDR.DataDeactivated(data)    => fail("Wrong DATA type")
        case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, data2.toSeq)
        case VDR.DataIPFS(cid)            => fail("Wrong DATA type")
        case VDR.DataStatusList(status)   => fail("Wrong DATA type")
    } yield ()
  }

  // TODO test update event with a different data type like IPFS
  // TODO test create event with a reserver field for future implementation
  // TODO test update event with a reserver field for future implementation
}
