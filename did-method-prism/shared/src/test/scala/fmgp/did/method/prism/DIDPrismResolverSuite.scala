package fmgp.did.method.prism

import munit.*
import fmgp.did.*
import fmgp.did.comm.*

import zio.*
import zio.json.*

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.DIDPrismResolverSuite */
class DIDPrismResolverSuite extends ZSuite {

  val intregrationTest = new munit.Tag("IntregrationTest")

  import fmgp.did.method.prism.proto.*

  val a792dc68_did = DIDSubject("did:prism:a792dc6872a76bcbf96f6fbb73a06e07c32a042d55e7a8bc9131a542697e1138")
  lazy val a792dc68_doc: DIDDocument = {
    val event0 = DIDPrismExamples.ex_preprod_a792dc68_event0.fromJson[MySignedPrismEvent[OP]].toOption.get
    val event1 = DIDPrismExamples.ex_preprod_a792dc68_event1.fromJson[MySignedPrismEvent[OP]].toOption.get
    val ssi = SSI.init(a792dc68_did).appendAny(event0).appendAny(event1)
    assert(ssi.exists, "SSI should exist after applying events")
    assert(ssi.services.nonEmpty, "SSI should have services")
    ssi.didDocument.getOrElse(fail("didDocument should be defined"))
  }

  testZ(
    "SSI.didDocument for 'did:prism:a792dc68...' should round-trip through JSON"
  ) {
    val json = a792dc68_doc.toJson
    json.fromJson[DIDDocument] match {
      case Left(error)      => fail(s"Failed to parse DIDDocument from SSI.didDocument JSON: $error")
      case Right(parsedDoc) =>
        assertEquals(parsedDoc.id, a792dc68_did)
        assert(parsedDoc.service.exists(_.nonEmpty), "Parsed DIDDocument should have services")
    }
    ZIO.unit
  }

  // https://github.com/FabioPinheiro/scala-did/issues/474
  test(
    "Service IDs in SSI.didDocument should be prefixed with DID (issue #474)"
  ) {
    a792dc68_doc.service.foreach(_.foreach { s =>
      assert(
        s.id.startsWith(a792dc68_did.string) && s.id.contains("#"),
        s"Service id '${s.id}' should start with '${a792dc68_did.string}#'"
      )
    })
    assertEquals(
      a792dc68_doc.service.flatMap(_.headOption).map(_.id),
      Some(s"${a792dc68_did.string}#didcomm"),
    )
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
