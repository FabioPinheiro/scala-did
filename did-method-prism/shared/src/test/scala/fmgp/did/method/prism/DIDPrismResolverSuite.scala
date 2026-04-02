package fmgp.did.method.prism

import munit.*
import fmgp.did.*
import fmgp.did.comm.*

import zio.*
import zio.json.*

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.DIDPrismResolverSuite */
class DIDPrismResolverSuite extends ZSuite {

  val intregrationTest = new munit.Tag("IntregrationTest")

  testZ(
    "SSI.didDocument for 'did:prism:a792dc6872a76bcbf96f6fbb73a06e07c32a042d55e7a8bc9131a542697e1138' with DIDCommMessaging service"
  ) {
    import fmgp.did.method.prism.proto.*
    import fmgp.did.method.prism.cardano.EventCursor

    val did = DIDSubject("did:prism:a792dc6872a76bcbf96f6fbb73a06e07c32a042d55e7a8bc9131a542697e1138")
    val event0 = DIDPrismExamples.ex_preprod_a792dc68_event0.fromJson[MySignedPrismEvent[OP]]
    val event1 = DIDPrismExamples.ex_preprod_a792dc68_event1.fromJson[MySignedPrismEvent[OP]]

    (event0, event1) match {
      case (Right(e0), Right(e1)) =>
        val ssi = SSI
          .init(did)
          .appendAny(e0)
          .appendAny(e1)

        assert(ssi.exists, "SSI should exist after applying events")
        assert(ssi.services.nonEmpty, "SSI should have services")

        val maybeDoc = ssi.didDocument
        assert(maybeDoc.isDefined, "didDocument should be defined")

        val doc = maybeDoc.get
        val json = doc.toJson
        // This is the critical assertion: the JSON should parse back into a DIDDocument
        json.fromJson[DIDDocument] match {
          case Left(error)      => fail(s"Failed to parse DIDDocument from SSI.didDocument JSON: $error")
          case Right(parsedDoc) =>
            assertEquals(parsedDoc.id, did)
            println("*" * 100)
            println(parsedDoc.service.head)
            assert(parsedDoc.service.exists(_.nonEmpty), "Parsed DIDDocument should have services")
        }
      case (Left(err), _) => fail(s"Failed to parse event0: $err")
      case (_, Left(err)) => fail(s"Failed to parse event1: $err")
    }
    ZIO.unit
  }

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
    }
      .provide(
        HttpUtilsSuiteAUX.layer >>> DIDPrismResolver.layer(
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
    }
      .provide(
        HttpUtilsSuiteAUX.layer >>> DIDPrismResolver.layer(
          "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/diddoc"
        )
      )
  }
}
