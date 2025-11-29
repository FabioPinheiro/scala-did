package fmgp.did.method.web

import munit.*
import fmgp.did.*
import fmgp.did.comm.*

import zio.*

/** didResolverWebJVM/testOnly fmgp.did.method.web.DIDWebResolverSuite */
class DIDWebResolverSuite extends ZSuite {

  val intregrationTest = new munit.Tag("IntregrationTest")

  testZ("Resolver (.well-known) 'did:web:did.fmgp.app'".tag(intregrationTest)) {
    {
      for {
        resolver <- ZIO.service[Resolver]
        subject = DIDSubject("did:web:did.fmgp.app")
        doc <- resolver.didDocument(subject.asFROMTO)
      } yield assertEquals(doc.id, subject)
    }.provideLayer(DIDWebResolverSuiteUtils.resolverLayer)
  }

  testZ("Resolver (with path) 'did:web:did.fmgp.app:fabio'".tag(intregrationTest)) {
    {
      for {
        resolver <- ZIO.service[Resolver]
        subject = DIDSubject("did:web:did.fmgp.app:fabio")
        doc <- resolver.didDocument(subject.asFROMTO)
      } yield assertEquals(doc.id, subject)
    }.provideLayer(DIDWebResolverSuiteUtils.resolverLayer)
  }
}
