package fmgp.did.method.prism.vdr

import zio._
import zio.json._
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.{RefVDR, DIDPrism, PrismState, VDR, EventHash, PrismChainService}
import fmgp.did.method.prism.cardano.TxHash
import proto.prism.PrismBlock
import proto.prism.SignedPrismOperation

case class VDRServiceImpl(
    chain: PrismChainService,
    protected val refPrismState: Ref[PrismState]
) extends VDRService {
  override def submit(seqSignedPrismOperation: SignedPrismOperation*): ZIO[Any, Throwable, TxHash] =
    chain.commitAndPush(
      prismEvents = seqSignedPrismOperation,
      msg = None
    )
}

trait VDRService extends VDRPassoveService {
  def submit(seqSignedPrismOperation: SignedPrismOperation*): ZIO[Any, Throwable, TxHash]

  def createBytes(
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Any, Throwable, (RefVDR, SignedPrismOperation, TxHash)] =
    for {
      tmp <- dryCreateBytes(didPrism, vdrKey, data)
      (refVDR, signedPrismOperation) = tmp
      txHash <- submit(signedPrismOperation)
    } yield (refVDR, signedPrismOperation, txHash)
  def updateBytes(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Any, Throwable, (EventHash, SignedPrismOperation, TxHash)] =
    for {
      tmp <- dryUpdateBytes(eventRef, vdrKey, data)
      (refVDR, signedPrismOperation) = tmp
      txHash <- submit(signedPrismOperation)
    } yield (refVDR, signedPrismOperation, txHash)
  def deactivate(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey
  ): ZIO[Any, Throwable, (EventHash, SignedPrismOperation, TxHash)] =
    for {
      tmp <- dryDeactivate(eventRef, vdrKey)
      (refVDR, signedPrismOperation) = tmp
      txHash <- submit(signedPrismOperation)
    } yield (refVDR, signedPrismOperation, txHash)
}
