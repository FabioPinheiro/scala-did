package fmgp.prism

import munit._
import zio._
import zio.json._
import fmgp.did.method.prism._

/** PrismStateHTTPSuite
  *
  * didResolverPrismJVM/testOnly fmgp.prism.PrismStateHTTPSuite
  *
  * didResolverPrismJS/testOnly fmgp.prism.PrismStateHTTPSuite
  */
class PrismStateHTTPSuite extends ZSuite {
  testZ("Get events for did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae") {
    {
      for {
        httpUtils <- ZIO.service[HttpUtils]
        state = PrismStateHTTP(httpUtils)
        events <- state.getEventsForSSI(DIDPrism("00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae"))
        _ = assertEquals(events.size, 2)
        _ = assertEquals(
          events.head.toJsonPretty,
          """{
            |  "tx" : "bc688c8d7b1588a0fb10b307b6e975998eb0aedf039440580ddcec1cc51bb66d",
            |  "b" : 89,
            |  "o" : 0,
            |  "signedWith" : "master0",
            |  "signature" : "304502210088333a3046528671e651afda2975546a9ecfa9e5c953a649b8d5b6d8f84507bd02203d9c4f7930c6cf99d37c592b316bf0c2b1563a8525309478a7b9b4e6318d03eb",
            |  "operation" : {
            |    "CreateDidOP" : {
            |      "publicKeys" : [
            |        {
            |          "CompressedECKey" : {
            |            "id" : "master0",
            |            "usage" : "MasterKeyUsage",
            |            "curve" : "secp256k1",
            |            "data" : "021456f5dd7bcddca7a3e48edad8cc68d0ce6a7a5991492cb48bac817c2e4d9adc"
            |          }
            |        }
            |      ],
            |      "services" : [],
            |      "context" : []
            |    }
            |  },
            |  "protobuf" : "0a3f0a3d123b0a076d61737465723010014a2e0a09736563703235366b311221021456f5dd7bcddca7a3e48edad8cc68d0ce6a7a5991492cb48bac817c2e4d9adc"
            |}""".stripMargin
        )
        _ = assertEquals(
          events.tail.head.toJsonPretty,
          """{
            |  "tx" : "bc688c8d7b1588a0fb10b307b6e975998eb0aedf039440580ddcec1cc51bb66d",
            |  "b" : 89,
            |  "o" : 1,
            |  "signedWith" : "master0",
            |  "signature" : "3045022100931b1a758cb7a34e7a159de8acb4e52751323e37aadeee2d33d121696ccb29b402204e54027e3ee1b6f38e511b9cf85865c45980e696686ad7548d810d2df05e8ed7",
            |  "operation" : {
            |    "UpdateDidOP" : {
            |      "previousOperationHash" : "00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae",
            |      "id" : "00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae",
            |      "actions" : [
            |        {
            |          "AddKey" : {
            |            "key" : {
            |              "CompressedECKey" : {
            |                "id" : "issuing0",
            |                "usage" : "IssuingKeyUsage",
            |                "curve" : "secp256k1",
            |                "data" : "02a680f17d9b683a9043b45a89989d37fed7a2de8a025eb19790933f59412b64f3"
            |              }
            |            }
            |          }
            |        },
            |        {
            |          "AddKey" : {
            |            "key" : {
            |              "CompressedECKey" : {
            |                "id" : "revocation0",
            |                "usage" : "RevocationKeyUsage",
            |                "curve" : "secp256k1",
            |                "data" : "0384cdd12ac3cf34f241a75281e755e97b35984845b0b7b922df14790b2a9a2266"
            |              }
            |            }
            |          }
            |        }
            |      ]
            |    }
            |  },
            |  "protobuf" : "12eb010a2000592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae1240303035393261313431613463326263623761366161363931373530353131653265396230343832333138323031323565313561623730623132613231306161651a400a3e0a3c0a0869737375696e673010024a2e0a09736563703235366b31122102a680f17d9b683a9043b45a89989d37fed7a2de8a025eb19790933f59412b64f31a430a410a3f0a0b7265766f636174696f6e3010054a2e0a09736563703235366b3112210384cdd12ac3cf34f241a75281e755e97b35984845b0b7b922df14790b2a9a2266"
            |}""".stripMargin
        )
      } yield ()
    }.provide(HttpUtilsSuiteAUX.layer)
  }
}
