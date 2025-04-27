package fmgp.did.method.prism

import munit._
import zio._
import zio.json._
import fmgp.crypto._
import fmgp.did._
import fmgp.multibase._

import DIDPrismExamples._
import zio.json.ast.Json
import fmgp.util.Base64
import scala.util.matching.Regex

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.DIDResolverProxySuite */
class DIDResolverProxySuite extends ZSuite {

  override val munitTimeout = scala.concurrent.duration.Duration(5, "s")

  val intregrationTest = new munit.Tag("IntregrationTest")

  testZ("Get DIDDocument for a DID".tag(intregrationTest)) {
    {
      for {
        proxy <- ZIO.service[DIDResolverProxy]
        tmp <- proxy
          .httpProxy(
            DIDSubject("did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae").toDID
          )
        _ = assertEquals(
          tmp.toJsonPretty,
          """{
          |  "id" : "did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae",
          |  "assertionMethod" : [
          |    {
          |      "id" : "did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae#issuing0",
          |      "controller" : "did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae",
          |      "type" : "JsonWebKey2020",
          |      "publicKeyJwk" : {
          |        "kty" : "EC",
          |        "crv" : "secp256k1",
          |        "x" : "poDxfZtoOpBDtFqJmJ03_tei3ooCXrGXkJM_WUErZPM",
          |        "y" : "M6WTO1raVf2TNHO7t0IpiurajRo6k12HbJvNa2L-8sA",
          |        "kid" : "did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae#issuing0"
          |      }
          |    }
          |  ]
          |}""".stripMargin
        )
      } yield ()
    }.provide(DIDResolverProxySuiteUtils.layer)
    // https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/di:c/d:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae
  }
}
