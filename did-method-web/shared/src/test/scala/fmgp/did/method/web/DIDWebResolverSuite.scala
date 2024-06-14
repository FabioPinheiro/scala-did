package fmgp.did.method.web

import munit._
import fmgp.did._
import fmgp.did.comm._

import zio._

class DIDWebResolverSuite extends ZSuite {

  testZ("Resolver (.well-known) 'did:web:did.fmgp.app'") {
    {
      for {
        resolver <- ZIO.service[Resolver]
        subject = DIDSubject("did:web:did.fmgp.app")
        doc <- resolver.didDocument(subject.asFROMTO)
      } yield assertEquals(doc.id, subject)
    }.provideLayer(DIDWebResolverSuiteUtils.resolverLayer)
  }

  testZ("Resolver (with path) 'did:web:did.fmgp.app:fabio'") {
    {
      for {
        resolver <- ZIO.service[Resolver]
        subject = DIDSubject("did:web:did.fmgp.app:fabio")
        doc <- resolver.didDocument(subject.asFROMTO)
      } yield assertEquals(doc.id, subject)
    }.provideLayer(DIDWebResolverSuiteUtils.resolverLayer)
  }
}
