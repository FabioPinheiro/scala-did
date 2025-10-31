package fmgp.did.method.peer

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto._

// TODO RENAME DIDPeerResolver
class DidPeerResolver extends Resolver {
  override protected def didDocumentOf(did: FROMTO): IO[UnsupportedMethod, DIDDocument] = did.toDID match {
    case peer: DIDPeer                                => DidPeerResolver.didDocument(peer)
    case did if DIDPeer.regexPeer.matches(did.string) =>
      DidPeerResolver.didDocument(DIDPeer(did))
    case did => ZIO.fail(UnsupportedMethod(did.namespace))
  }
}
object DidPeerResolver {
  val default = new DidPeerResolver()
  val layer: ULayer[Resolver] = ZLayer.succeed(default)
  val layerDidPeerResolver: ULayer[DidPeerResolver] = ZLayer.succeed(default)

  /** see https://identity.foundation/peer-did-method-spec/#generation-method */
  def didDocument(didPeer: DIDPeer): UIO[DIDDocument] = didPeer match {
    case peer: DIDPeer0 => genesisDocument(peer)
    case peer: DIDPeer1 => genesisDocument(peer)
    case peer: DIDPeer2 => genesisDocument(peer)
    case peer: DIDPeer3 => genesisDocument(peer)
    case peer: DIDPeer4 => genesisDocument(peer)
  }

  def genesisDocument(did: DIDPeer0): UIO[DIDDocument] = ZIO.succeed(did.document)
  def genesisDocument(did: DIDPeer1): UIO[DIDDocument] = ZIO.succeed(did.document)
  def genesisDocument(did: DIDPeer2): UIO[DIDDocument] = ZIO.succeed(did.document)
  def genesisDocument(did: DIDPeer3): UIO[DIDDocument] = ZIO.succeed(did.document)
  def genesisDocument(did: DIDPeer4): UIO[DIDDocument] = ZIO.succeed(did.document)
}
