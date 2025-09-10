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

  test("create SSI") {
    val (did, e1) = createDID(Seq(("master1", pkMaster)), Seq(("vdr1", pk1VDR)))
    assertEquals(did, didPrism)
    assertEquals(bytes2Hex(e1.toByteArray), createSSI)
  }
  test("update SSI") {
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
  test("create VDR") {
    val (vdr, e2) = createVDREntryBytes(didPrism, pk1VDR, "vdr1", data1, nonce = Array.empty)
    assertEquals(vdr, refVDR)
    assertEquals(bytes2Hex(e2.toByteArray), createVDR)
  }
  test("update VDR") {
    val (e3EventHash, e3) =
      updateVDREntryBytes(refVDR, getSignedPrismEventFromHex(createVDR), pk1VDR, "vdr1", data2)
    assertEquals(bytes2Hex(e3.toByteArray), updateVDR)
  }

  test("update VDR WithUnknownFields 49") {
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

  test("update VDR WithUnknownFields 99") {
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

  test("update VDR after add key in the SSI") {
    val (e5EventHash, e5) =
      updateVDREntryBytes(refVDR, getSignedPrismEventFromHex(updateVDR), pk2VDR, "vdr2", data3)
    assertEquals(bytes2Hex(e5.toByteArray), updateVDR_withTheNewKey)
  }

}
