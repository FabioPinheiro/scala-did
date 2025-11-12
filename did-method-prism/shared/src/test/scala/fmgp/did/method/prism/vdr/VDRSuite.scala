package fmgp.did.method.prism.vdr

import munit._
import zio._
import zio.json._
import zio.stream._

import _root_.proto.prism._
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import fmgp.did.method.prism.*
import fmgp.did.method.prism.vdr.*
import fmgp.did.method.prism.proto.MaybeEvent
import fmgp.did.method.prism.proto.MySignedPrismEvent
import fmgp.crypto.SHA256

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.vdr.VDRSuite
  */
class VDRSuite extends ZSuite {

  import fmgp.did.method.prism.vdr.KeyConstanceUtils._
  import fmgp.did.method.prism.vdr.VDRUtilsTestExtra._
  import fmgp.did.method.prism.vdr.VDRUtils._

  private def getSignedPrismEventFromHex(hex: String) =
    SignedPrismEvent.parseFrom(hex2bytes(hex))

  test("create SSI".tag(fmgp.JsUnsupported)) {
    val (did, e1) = createDID(Seq(("master1", pkMaster)), Seq(("vdr1", pk1VDR)))
    assertEquals(did, didPrism)
    assertEquals(bytes2Hex(e1.toByteArray), createSSI)
  }

  test("update SSI".tag(fmgp.JsUnsupported)) {
    val e4 = updateDIDAddKey(
      didPrism = didPrism,
      previousEvent = getSignedPrismEventFromHex(createSSI),
      masterKeyName = "master1",
      masterKey = pkMaster,
      vdrKeyName = "vdr2",
      vdrKey = pk2VDR,
    )
    assertEquals(bytes2Hex(e4.toByteArray), updateSSI_addKey)
  }

  test("create VDR".tag(fmgp.JsUnsupported)) {
    val (vdr, e2) = createVDREntryBytes(didPrism, pk1VDR, "vdr1", data1, nonce = Array.empty)
    assertEquals(vdr, refVDR)
    assertEquals(bytes2Hex(e2.toByteArray), createVDR)
  }

  test("update VDR".tag(fmgp.JsUnsupported)) {
    val (e3EventHash, e3) =
      updateVDREntryBytes(refVDR, getSignedPrismEventFromHex(createVDR), pk1VDR, "vdr1", data2)
    assertEquals(bytes2Hex(e3.toByteArray), updateVDR)
  }

  test("update VDR WithUnknownFields 49".tag(fmgp.JsUnsupported)) {
    val e3x = updateVDREntryWithUnknownFields(
      refVDR,
      getSignedPrismEventFromHex(createVDR),
      pk1VDR,
      "vdr1",
      data2,
      unknownFieldNumber = 49
    )
    assertEquals(bytes2Hex(e3x.toByteArray), updateVDR_withUnknownField49)
  }

  test("update VDR WithUnknownFields 99".tag(fmgp.JsUnsupported)) {
    val e3x = updateVDREntryWithUnknownFields(
      refVDR,
      getSignedPrismEventFromHex(createVDR),
      pk1VDR,
      "vdr1",
      data2,
      unknownFieldNumber = 99
    )
    assertEquals(bytes2Hex(e3x.toByteArray), updateVDR_withUnknownField99)
  }

  test("update VDR after add key in the SSI".tag(fmgp.JsUnsupported)) {
    val (e5EventHash, e5) =
      updateVDREntryBytes(refVDR, getSignedPrismEventFromHex(updateVDR), pk2VDR, "vdr2", data3)
    assertEquals(bytes2Hex(e5.toByteArray), updateVDR_withTheNewKey)
  }

  test("deactivate VDR after create updateVDR".tag(fmgp.JsUnsupported)) {
    val (e6EventHash, e6) =
      deactivateVDREntry(refVDR, getSignedPrismEventFromHex(updateVDR), pk1VDR, "vdr1")
    assertEquals(bytes2Hex(e6.toByteArray), deactivateVDR_afterUpdate)
  }

  test("VDR make: createSSI + createVDR".tag(fmgp.JsUnsupported)) {
    val e1 = makeSignedPrismEvent(createVDR, 1)
    val ssiHistory = SSI.makeSSIHistory(
      VDRUtilsTestExtra.didPrism,
      Seq(makeSignedPrismEvent(VDRUtilsTestExtra.createSSI, 0))
    )
    val vdr = VDR.make(
      vdrRef = RefVDR.fromEvent(e1),
      ssiHistory = ssiHistory,
      events = Seq(e1),
    )

    assertEquals(vdr.id.hex, VDRUtilsTestExtra.refVDR.hex)
    vdr.data match
      case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, VDRUtilsTestExtra.data1.toSeq)
      case _                            => fail("Wrong Data type")
  }

  test("VDR make: createSSI + createVDR + updateVDR".tag(fmgp.JsUnsupported)) {
    val events = Seq(
      makeSignedPrismEvent(createVDR, 1),
      makeSignedPrismEvent(updateVDR, 2),
    )
    val ssiHistory = SSI.makeSSIHistory(
      VDRUtilsTestExtra.didPrism,
      Seq(makeSignedPrismEvent(VDRUtilsTestExtra.createSSI, 0))
    )
    val vdr = VDR.make(
      vdrRef = RefVDR.fromEvent(events.head),
      ssiHistory = ssiHistory,
      events = events,
    )

    assertEquals(vdr.id.hex, VDRUtilsTestExtra.refVDR.hex)
    vdr.data match
      case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, VDRUtilsTestExtra.data2.toSeq)
      case _                            => fail("Wrong Data type")
  }

  test(
    "VDR make: createSSI + createVDR + updateVDR + updateSSI_addKey + updateVDR (with new key)".tag(fmgp.JsUnsupported)
  ) {
    val events = Seq(
      makeSignedPrismEvent(createVDR, 1),
      makeSignedPrismEvent(updateVDR, 2),
      makeSignedPrismEvent(updateVDR_withTheNewKey, 11),
    )
    val ssiHistory = SSI.makeSSIHistory(
      VDRUtilsTestExtra.didPrism,
      Seq(
        makeSignedPrismEvent(VDRUtilsTestExtra.createSSI, 0),
        makeSignedPrismEvent(VDRUtilsTestExtra.updateSSI_addKey, 10),
      )
    )
    val vdr = VDR.make(
      vdrRef = RefVDR.fromEvent(events.head),
      ssiHistory = ssiHistory,
      events = events,
    )

    assertEquals(vdr.id.hex, VDRUtilsTestExtra.refVDR.hex)
    vdr.data match
      case VDR.DataByteArray(byteArray) => assertEquals(byteArray.toSeq, VDRUtilsTestExtra.data3.toSeq)
      case _                            => fail("Wrong Data type")
  }

  test("VDR make: createSSI + createVDR + updateVDR + updateVDR (invalid key)".tag(fmgp.JsUnsupported)) {
    val events = Seq(
      makeSignedPrismEvent(createVDR, 1),
      makeSignedPrismEvent(updateVDR, 2),
      makeSignedPrismEvent(updateVDR_withTheNewKey, 11),
    )
    val ssiHistory = SSI.makeSSIHistory(
      VDRUtilsTestExtra.didPrism,
      Seq(makeSignedPrismEvent(VDRUtilsTestExtra.createSSI, 0))
    )
    val vdr = VDR.make(
      vdrRef = RefVDR.fromEvent(events.head),
      ssiHistory = ssiHistory,
      events = events,
    )

    assertEquals(vdr.id.hex, VDRUtilsTestExtra.refVDR.hex)
    vdr.data match
      case VDR.DataByteArray(byteArray) =>
        assertEquals(byteArray.toSeq, VDRUtilsTestExtra.data2.toSeq, "Data must have no effect from the latest event")
      case _ => fail("Wrong Data type")
  }

  test(
    "VDR make: createSSI + createVDR + updateVDR + updateVDR (with future key) + updateSSI_addKey".tag(
      fmgp.JsUnsupported
    )
  ) {
    val events = Seq(
      makeSignedPrismEvent(createVDR, 1),
      makeSignedPrismEvent(updateVDR, 2),
      makeSignedPrismEvent(updateVDR_withTheNewKey, 3),
    )
    val ssiHistory = SSI.makeSSIHistory(
      VDRUtilsTestExtra.didPrism,
      Seq(
        makeSignedPrismEvent(VDRUtilsTestExtra.createSSI, 0),
        makeSignedPrismEvent(VDRUtilsTestExtra.updateSSI_addKey, 10),
      )
    )
    val vdr = VDR.make(
      vdrRef = RefVDR.fromEvent(events.head),
      ssiHistory = ssiHistory,
      events = events,
    )

    assertEquals(vdr.id.hex, VDRUtilsTestExtra.refVDR.hex)
    vdr.data match
      case VDR.DataByteArray(byteArray) =>
        assertEquals(
          byteArray.toSeq,
          VDRUtilsTestExtra.data2.toSeq,
          "The order of events matters (evetns with future key must remain invalid)"
        )
      case _ => fail("Wrong Data type")
  }

  test("VDR make: createSSI + createVDR + updateVDR + deactivateVDR".tag(fmgp.JsUnsupported)) {
    val events = Seq(
      makeSignedPrismEvent(createVDR, 1),
      makeSignedPrismEvent(updateVDR, 2),
      makeSignedPrismEvent(deactivateVDR_afterUpdate, 3),
    )
    val ssiHistory = SSI.makeSSIHistory(
      VDRUtilsTestExtra.didPrism,
      Seq(makeSignedPrismEvent(VDRUtilsTestExtra.createSSI, 0))
    )
    val vdr = VDR.make(
      vdrRef = RefVDR.fromEvent(events.head),
      ssiHistory = ssiHistory,
      events = events,
    )

    assertEquals(vdr.id.hex, VDRUtilsTestExtra.refVDR.hex)
    vdr.data match
      case VDR.DataDeactivated(VDR.DataByteArray(byteArray)) => // ok
        assertEquals(byteArray.toSeq, VDRUtilsTestExtra.data2.toSeq)
      case VDR.DataByteArray(_) => fail("It should return empty after the deactivate")
      case _                    => fail("Wrong Data type")
  }

}
