package fmgp.did

import zio._

import fmgp.did.comm.{FROM, FROMTO, TO}

/** @see
  *   https://w3c.github.io/did-spec-registries/#did-methods
  * @see
  *   https://www.w3.org/TR/did-spec-registries/
  */
trait Resolver {
  def didDocument(did: FROMTO | TO | FROM): IO[ResolverError, DIDDocument] =
    didDocumentOf(did.asInstanceOf[FROMTO])
  protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument]
}

case class MultiFallbackResolver(
    firstResolver: Resolver,
    remainResolvers: Resolver*,
) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] =
    ZIO.firstSuccessOf(firstResolver.didDocument(did), remainResolvers.map(_.didDocument(did)))
}

case class MultiParResolver(
    firstResolver: Resolver,
    remainResolvers: Resolver*,
) extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] =
    ZIO.raceAll(firstResolver.didDocument(did), remainResolvers.map(_.didDocument(did)))
}
