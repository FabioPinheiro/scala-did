package fmgp.did.method.prism.vdr

import zio._
import zio.json._
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.{RefVDR, DIDPrism, PrismStateRead, VDR, EventHash, PrismChainService}
import fmgp.did.method.prism.cardano.TxHash
import proto.prism.PrismBlock
import proto.prism.SignedPrismEvent

case class VDRServiceImpl(
    chain: PrismChainService,
    protected val prismState: PrismStateRead
) extends VDRService {
  override def submit(seqSignedPrismEvent: SignedPrismEvent*): ZIO[Any, Throwable, TxHash] =
    chain.commitAndPush(
      prismEvents = seqSignedPrismEvent,
      msg = None
    )
}

trait VDRService extends VDRPassiveService {
  def submit(seqSignedPrismEvent: SignedPrismEvent*): ZIO[Any, Throwable, TxHash]

  def createBytes(
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Any, Throwable, (RefVDR, SignedPrismEvent, TxHash)] =
    for {
      tmp <- dryCreateBytes(didPrism, vdrKey, data)
      (refVDR, signedPrismEvent) = tmp
      txHash <- submit(signedPrismEvent)
    } yield (refVDR, signedPrismEvent, txHash)
  def updateBytes(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Any, Throwable, (EventHash, SignedPrismEvent, TxHash)] =
    for {
      tmp <- dryUpdateBytes(eventRef, vdrKey, data)
      (refVDR, signedPrismEvent) = tmp
      txHash <- submit(signedPrismEvent)
    } yield (refVDR, signedPrismEvent, txHash)
  def deactivate(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey
  ): ZIO[Any, Throwable, (EventHash, SignedPrismEvent, TxHash)] =
    for {
      tmp <- dryDeactivate(eventRef, vdrKey)
      (refVDR, signedPrismEvent) = tmp
      txHash <- submit(signedPrismEvent)
    } yield (refVDR, signedPrismEvent, txHash)
}
