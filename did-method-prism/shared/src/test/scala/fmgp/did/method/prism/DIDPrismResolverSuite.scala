package fmgp.did.method.prism

import munit._
import fmgp.did._
import fmgp.did.comm._

import zio._
import zio.json._

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.DIDPrismResolverSuite */
class DIDPrismResolverSuite extends ZSuite {

  val intregrationTest = new munit.Tag("IntregrationTest")

  testZ("Resolver 'did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae'".tag(intregrationTest)) {
    {
      for {
        resolver <- ZIO.service[Resolver]
        subject = DIDSubject("did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae")
        doc <- resolver.didDocument(subject.asFROMTO)
        _ = assertEquals(
          doc.toJsonPretty,
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
      } yield assertEquals(doc.id, subject)
    } // .provideLayer(DIDPrismResolverSuiteUtils.resolverLayer)
      .provide(
        HttpUtilsSuiteAUX.layer >>> DIDPrismResolver.makeLayer(
          "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/diddoc"
        )
      )
  }

  testZ("Resolver 'did:prism:0108edf719cb2e42aed1fd2b70435da12a77291eb25c294d6095cdfe874607b8'".tag(intregrationTest)) {
    {
      for {
        resolver <- ZIO.service[Resolver]
        subject = DIDSubject("did:prism:0108edf719cb2e42aed1fd2b70435da12a77291eb25c294d6095cdfe874607b8")
        doc <- resolver.didDocument(subject.asFROMTO)
        _ = assertEquals(
          doc.toJsonPretty,
          """{
            |  "id" : "did:prism:0108edf719cb2e42aed1fd2b70435da12a77291eb25c294d6095cdfe874607b8"
            |}""".stripMargin
        )
      } yield assertEquals(doc.id, subject)
    } // .provideLayer(DIDPrismResolverSuiteUtils.resolverLayer)
      .provide(
        HttpUtilsSuiteAUX.layer >>> DIDPrismResolver.makeLayer(
          "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/diddoc"
        )
      )
  }
}
