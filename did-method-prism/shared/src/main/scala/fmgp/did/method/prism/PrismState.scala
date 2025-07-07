package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did.method.prism._
import fmgp.did.DIDSubject
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto._

trait PrismStateRead {
  def lastSyncedBlockEpochSecondNano: (Long, Int) = {
    val now = java.time.Instant.now // FIXME
    (now.getEpochSecond, now.getNano)
  }

  // TODO REMOVE
  def ssi2eventsId: Map[DIDSubject, Seq[EventRef]]
  // TODO REMOVE
  def ssiCount: Int = ssi2eventsId.size

  def getEventsIdBySSI(ssi: DIDSubject): Seq[EventRef]
  def getEventsIdByVDR(id: RefVDR): Seq[EventRef]

  def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[MySignedPrismOperation[
    CreateDidOP | UpdateDidOP | DeactivateDidOP
  ]]] =
    getEventsIdBySSI(ssi)
      .foldLeft(Right(Seq.empty): Either[String, Seq[MySignedPrismOperation[OP]]])((acc, eventRef) =>
        acc match
          case Left(errors) => Left(errors)
          case Right(seq) =>
            getEventsByHash(eventRef.eventHash) match
              case None => Left(s"impossible state: missing Event/Operation Hash '${eventRef.eventHash}'")
              case Some(signedPrismOperation) => Right(seq :+ signedPrismOperation)
      ) match {
      case Left(error) => ZIO.fail(new RuntimeException(error))
      case Right(seq)  => PrismState.forceType2DidEvent(seq)
    }

  def getEventsForVDR(
      refVDR: RefVDR
  ): ZIO[Any, Throwable, Seq[MySignedPrismOperation[
    CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP
  ]]] =
    getEventsIdByVDR(refVDR)
      .foldLeft(Right(Seq.empty): Either[String, Seq[MySignedPrismOperation[OP]]])((acc, eventRef) =>
        acc match
          case Left(errors) => Left(errors)
          case Right(seq) =>
            getEventsByHash(eventRef.eventHash) match
              case None => Left(s"impossible state: missing Event/Operation Hash '${eventRef.eventHash}'")
              case Some(signedPrismOperation) => Right(seq :+ signedPrismOperation)
      ) match {
      case Left(error) => ZIO.fail(new RuntimeException(error))
      case Right(seq)  => PrismState.forceType2StorageEvent(seq)
    }

  def getEventsByHash(refHash: EventHash): Option[MySignedPrismOperation[OP]]

  def getSSI(ssi: DIDSubject): ZIO[Any, Throwable, SSI] =
    getSSIHistory(ssi).map(_.latestVersion) // getEventsForSSI(ssi).map { events => SSI.make(ssi, events) }

  def getSSIHistory(ssi: DIDSubject): ZIO[Any, Throwable, SSIHistory] =
    getEventsForSSI(ssi).map { events => SSI.makeSSIHistory(ssi, events) }

  def getVDR(ref: RefVDR): zio.ZIO[Any, Throwable, VDR] = getEventsForVDR(ref)
    .flatMap { events =>
      events.headOption match
        case None => ZIO.succeed(VDR.init(ref)) // owner is missing
        case Some(headEvent) =>
          headEvent.operation match {
            case _: CreateStorageEntryOP =>
              val didPrismOwner =
                headEvent.asInstanceOf[MySignedPrismOperation[CreateStorageEntryOP]].operation.didPrism
              getSSIHistory(didPrismOwner).map { ssiHistory =>
                VDR.make(vdrRef = ref, ssiHistory = ssiHistory, ops = events)
              }
            case event => ???
          }
    }

}

object PrismState {
  def empty: PrismState = PrismStateInMemory.empty

  def forceType2DidEvent(seq: Seq[MySignedPrismOperation[OP]]): ZIO[Any, RuntimeException, Seq[
    MySignedPrismOperation[CreateDidOP | UpdateDidOP | DeactivateDidOP]
  ]] = {
    ZIO.foldLeft(seq)(Seq.empty[MySignedPrismOperation[OP.TypeDidEvent]]) { (s, event) =>
      event.operation match
        case _: CreateDidOP =>
          ZIO.succeed(s :+ event.asInstanceOf[MySignedPrismOperation[OP.TypeDidEvent]])
        case _: UpdateDidOP =>
          ZIO.succeed(s :+ event.asInstanceOf[MySignedPrismOperation[OP.TypeDidEvent]])
        case _: DeactivateDidOP =>
          ZIO.succeed(s :+ event.asInstanceOf[MySignedPrismOperation[OP.TypeDidEvent]])
        case event => ZIO.fail(new RuntimeException("This Event is not a DID Event")) // FIXME
    }
  }

  def forceType2StorageEvent(seq: Seq[MySignedPrismOperation[OP]]): ZIO[Any, RuntimeException, Seq[
    MySignedPrismOperation[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP]
  ]] = {
    ZIO.foldLeft(seq)(Seq.empty[MySignedPrismOperation[OP.TypeStorageEntryEvent]]) { (s, event) =>
      event.operation match
        case _: CreateStorageEntryOP =>
          ZIO.succeed(s :+ event.asInstanceOf[MySignedPrismOperation[OP.TypeStorageEntryEvent]])
        case _: UpdateStorageEntryOP =>
          ZIO.succeed(s :+ event.asInstanceOf[MySignedPrismOperation[OP.TypeStorageEntryEvent]])
        case _: DeactivateStorageEntryOP =>
          ZIO.succeed(s :+ event.asInstanceOf[MySignedPrismOperation[OP.TypeStorageEntryEvent]])
        case event => ZIO.fail(new RuntimeException("This Event is not a Storage Entry")) // FIXME
    }
  }

}

trait PrismState extends PrismStateRead {
  def addEvent(op: MySignedPrismOperation[OP]): PrismState
}
