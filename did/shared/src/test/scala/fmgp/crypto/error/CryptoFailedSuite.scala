package fmgp.crypto.error

import munit.*
import zio.json.*

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
