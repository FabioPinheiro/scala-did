package fmgp.crypto.error

import munit._
import zio.json._

import fmgp.did.DIDSubject
import fmgp.did.comm.PIURI
import fmgp.crypto.Curve

/** didJVM / testOnly fmgp.crypto.CryptoFailedSuite */
class CryptoFailedSuite extends FunSuite {

  test("CryptoFailedSuite test") {
    assertEquals(
      (FailToParse("test"): DidFail).toJsonPretty,
      """{
        |  "typeOfDidFail" : "FailToParse",
        |  "error" : "test"
        |}""".stripMargin
    )
  }

}
