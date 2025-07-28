package fmgp.did.method.prism.vdr

import zio._
import zio.json._
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.{RefVDR, DIDPrism, PrismState, VDR, EventHash}
import fmgp.did.method.prism.cardano.TxHash
import proto.prism.PrismBlock
import proto.prism.SignedPrismOperation

case class VDRPassiveServiceImpl(protected val refPrismState: Ref[PrismState]) extends VDRPassoveService

trait VDRPassoveService {
  protected def refPrismState: Ref[PrismState]

  def fetch(vdrRef: RefVDR): ZIO[Any, Throwable, VDR] =
    VDRPassoveService.fetch(vdrRef).provideEnvironment(ZEnvironment(refPrismState))
  def prove(eventRef: RefVDR): ZIO[Any, Throwable, PrismBlock] =
    VDRPassoveService.prove(eventRef).provideEnvironment(ZEnvironment(refPrismState))
  def fullProve(eventRef: RefVDR): ZIO[Any, Throwable, PrismBlock] =
    VDRPassoveService.fullProve(eventRef).provideEnvironment(ZEnvironment(refPrismState))
  def dryCreateBytes(
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Any, Throwable, (RefVDR, SignedPrismOperation)] =
    VDRPassoveService.dryCreateBytes(didPrism, vdrKey, data).provideEnvironment(ZEnvironment(refPrismState))
  def dryUpdateBytes(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Any, Throwable, (EventHash, SignedPrismOperation)] =
    VDRPassoveService.dryUpdateBytes(eventRef, vdrKey, data).provideEnvironment(ZEnvironment(refPrismState))
  def dryDeactivate(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey
  ): ZIO[Any, Throwable, (EventHash, SignedPrismOperation)] =
    VDRPassoveService.dryDeactivate(eventRef, vdrKey).provideEnvironment(ZEnvironment(refPrismState))
}

object VDRPassoveService {
  def fetch(vdrRef: RefVDR): ZIO[Ref[PrismState], Throwable, VDR] =
    for {
      _ <- ZIO.log(s"fecth VDR entry '${vdrRef.hex}'")
      state <- ZIO.serviceWithZIO[Ref[PrismState]](_.get)
      vdr <- state.getVDR(vdrRef)
    } yield vdr

  /** return a PrismBlock with all event (in order) relative to the vdr */
  def prove(eventRef: RefVDR): ZIO[Ref[PrismState], Throwable, PrismBlock] = ??? // FIXME

  /** return a PrismBlock with all event (in order) relative to the vdr and the owner (did:prism) */
  def fullProve(eventRef: RefVDR): ZIO[Ref[PrismState], Throwable, PrismBlock] = ??? // FIXME

  def dryCreateBytes(
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Ref[PrismState], Throwable, (RefVDR, SignedPrismOperation)] =
    for {
      state <- ZIO.serviceWithZIO[Ref[PrismState]](_.get)
      ssi <- state.getSSI(didPrism)
      nonce <- Random.nextBytes(16)
      keyLable <- ssi.findVDRKey(vdrKey) match
        case None        => ZIO.fail(new RuntimeException("The VDR key is not active in SSI")) // TODO error type
        case Some(ecKey) => ZIO.succeed(ecKey.id)
      ret = VDRUtils.createVDREntryBytes(
        didPrism = didPrism,
        vdrKey = vdrKey,
        keyName = keyLable,
        data = data,
        nonce = nonce.toArray,
      )
    } yield ret

  def dryUpdateBytes(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Ref[PrismState], Throwable, (EventHash, SignedPrismOperation)] =
    for {
      state <- ZIO.serviceWithZIO[Ref[PrismState]](_.get)
      oldVDR <- state.getVDR(eventRef)
      didPrism <- oldVDR.did match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(new RuntimeException("VDR is missing the owner?")) // TODO error type
      latestVDRHash <- oldVDR.latestVDRHash match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(new RuntimeException("VDR is missing the latestVDRHash?")) // TODO error type
      ssi <- state.getSSI(didPrism)
      keyLable <- ssi.findVDRKey(vdrKey) match
        case None        => ZIO.fail(new RuntimeException("The VDR key is not active in SSI")) // TODO error type
        case Some(ecKey) => ZIO.succeed(ecKey.id)
      ret = VDRUtils.updateVDREntryBytes(
        eventRef = eventRef,
        previousEventHash = latestVDRHash,
        vdrKey = vdrKey,
        keyName = keyLable,
        data = data,
      )
    } yield ret

  def dryDeactivate(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey
  ): ZIO[Ref[PrismState], Throwable, (EventHash, SignedPrismOperation)] =
    for {
      state <- ZIO.serviceWithZIO[Ref[PrismState]](_.get)
      oldVDR <- state.getVDR(eventRef)
      didPrism <- oldVDR.did match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(new RuntimeException("VDR is missing the owner?")) // TODO error type
      latestVDRHash <- oldVDR.latestVDRHash match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(new RuntimeException("VDR is missing the latestVDRHash?")) // TODO error type
      ssi <- state.getSSI(didPrism)
      keyLable <- ssi.findVDRKey(vdrKey) match
        case None        => ZIO.fail(new RuntimeException("The VDR key is not active in SSI")) // TODO error type
        case Some(ecKey) => ZIO.succeed(ecKey.id)
      ret = VDRUtils.deactivateVDREntry(
        eventRef = eventRef,
        previousEventHash = latestVDRHash,
        vdrKey = vdrKey,
        keyName = keyLable,
      )
    } yield ret

}
