package fmgp.did.method.prism.vdr

import zio.*
import zio.json.*
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.{RefVDR, DIDPrism, PrismState, PrismStateRead, VDR, EventHash}
import fmgp.did.method.prism.cardano.TxHash
import proto.prism.PrismBlock
import proto.prism.SignedPrismEvent

case class VDRPassiveServiceImpl(protected val prismState: PrismStateRead) extends VDRPassiveService

trait VDRPassiveService {
  protected def prismState: PrismStateRead

  def fetch(vdrRef: RefVDR): ZIO[Any, Throwable, VDR] =
    VDRPassiveService.fetch(vdrRef).provideEnvironment(ZEnvironment(prismState))
  def prove(eventRef: RefVDR): ZIO[Any, Throwable, PrismBlock] =
    VDRPassiveService.prove(eventRef).provideEnvironment(ZEnvironment(prismState))
  def fullProve(eventRef: RefVDR): ZIO[Any, Throwable, PrismBlock] =
    VDRPassiveService.fullProve(eventRef).provideEnvironment(ZEnvironment(prismState))
  def dryCreateBytes(
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Any, Throwable, (RefVDR, SignedPrismEvent)] =
    VDRPassiveService.dryCreateBytes(didPrism, vdrKey, data).provideEnvironment(ZEnvironment(prismState))
  def dryUpdateBytes(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[Any, Throwable, (EventHash, SignedPrismEvent)] =
    VDRPassiveService.dryUpdateBytes(eventRef, vdrKey, data).provideEnvironment(ZEnvironment(prismState))
  def dryDeactivate(
      eventRef: RefVDR,
      vdrKey: Secp256k1PrivateKey
  ): ZIO[Any, Throwable, (EventHash, SignedPrismEvent)] =
    VDRPassiveService.dryDeactivate(eventRef, vdrKey).provideEnvironment(ZEnvironment(prismState))
}

object VDRPassiveService {
  def fetch(vdrRef: RefVDR): ZIO[PrismStateRead, Throwable, VDR] =
    for {
      _ <- ZIO.log(s"fecth VDR entry '${vdrRef.hex}'")
      state <- ZIO.service[PrismStateRead]
      vdr <- state.getVDR(vdrRef)
    } yield vdr

  /** return a PrismBlock with all event (in order) relative to the vdr */
  def prove(eventRef: RefVDR): ZIO[PrismStateRead, Throwable, PrismBlock] = ??? // FIXME

  /** return a PrismBlock with all event (in order) relative to the vdr and the owner (did:prism) */
  def fullProve(eventRef: RefVDR): ZIO[PrismStateRead, Throwable, PrismBlock] = ??? // FIXME

  def dryCreateBytes(
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      data: Array[Byte]
  ): ZIO[PrismStateRead, Throwable, (RefVDR, SignedPrismEvent)] =
    for {
      state <- ZIO.service[PrismStateRead]
      ssi <- state.getSSI(didPrism)
      nonce <- Random.nextBytes(16)
      _ <- ZIO.whenDiscard(!ssi.exists) { ZIO.fail(new RuntimeException("The SSI does not exist")) } // TODO error type
      _ <- ZIO.whenDiscard(ssi.disabled) { ZIO.fail(new RuntimeException("The SSI is disabled")) } // TODO error type
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
  ): ZIO[PrismStateRead, Throwable, (EventHash, SignedPrismEvent)] =
    for {
      state <- ZIO.service[PrismStateRead]
      oldVDR <- state.getVDR(eventRef)
      didPrism <- oldVDR.did match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(new RuntimeException("VDR is missing the owner?")) // TODO error type
      latestVDRHash <- oldVDR.latestVDRHash match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(new RuntimeException("VDR is missing the latestVDRHash?")) // TODO error type
      ssi <- state.getSSI(didPrism)
      _ <- ZIO.whenDiscard(!ssi.exists) { ZIO.fail(new RuntimeException("The SSI does not exist")) } // TODO error type
      _ <- ZIO.whenDiscard(ssi.disabled) { ZIO.fail(new RuntimeException("The SSI is disabled")) } // TODO error type
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
  ): ZIO[PrismStateRead, Throwable, (EventHash, SignedPrismEvent)] =
    for {
      state <- ZIO.service[PrismStateRead]
      oldVDR <- state.getVDR(eventRef)
      didPrism <- oldVDR.did match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(new RuntimeException("VDR is missing the owner?")) // TODO error type
      latestVDRHash <- oldVDR.latestVDRHash match
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(new RuntimeException("VDR is missing the latestVDRHash?")) // TODO error type
      ssi <- state.getSSI(didPrism)
      _ <- ZIO.whenDiscard(!ssi.exists) { ZIO.fail(new RuntimeException("The SSI does not exist")) } // TODO error type
      _ <- ZIO.whenDiscard(ssi.disabled) { ZIO.fail(new RuntimeException("The SSI is disabled")) } // TODO error type
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
