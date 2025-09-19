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

  def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[MySignedPrismEvent[
    CreateDidOP | UpdateDidOP | DeactivateDidOP
  ]]] =
    getEventsIdBySSI(ssi)
      .foldLeft(Right(Seq.empty): Either[String, Seq[MySignedPrismEvent[OP]]])((acc, eventRef) =>
        acc match
          case Left(errors) => Left(errors)
          case Right(seq) =>
            getEventsByHash(eventRef.eventHash) match
              case None                   => Left(s"impossible state: missing Event hash '${eventRef.eventHash}'")
              case Some(signedPrismEvent) => Right(seq :+ signedPrismEvent)
      ) match {
      case Left(error) => ZIO.fail(new RuntimeException(error))
      case Right(seq)  => PrismState.forceType2DidEvent(seq)
    }

  def getEventsForVDR(
      refVDR: RefVDR
  ): ZIO[Any, Throwable, Seq[MySignedPrismEvent[
    CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP
  ]]] =
    getEventsIdByVDR(refVDR)
      .foldLeft(Right(Seq.empty): Either[String, Seq[MySignedPrismEvent[OP]]])((acc, eventRef) =>
        acc match
          case Left(errors) => Left(errors)
          case Right(seq) =>
            getEventsByHash(eventRef.eventHash) match
              case None                   => Left(s"impossible state: missing Event hash '${eventRef.eventHash}'")
              case Some(signedPrismEvent) => Right(seq :+ signedPrismEvent)
      ) match {
      case Left(error) => ZIO.fail(new RuntimeException(error))
      case Right(seq)  => PrismState.forceType2StorageEvent(seq)
    }

  def getEventsByHash(refHash: EventHash): Option[MySignedPrismEvent[OP]]

  def getSSI(ssi: DIDSubject): ZIO[Any, Throwable, SSI] =
    getSSIHistory(ssi).map(_.latestVersion) // getEventsForSSI(ssi).map { events => SSI.make(ssi, events) }

  def getSSIHistory(ssi: DIDSubject): ZIO[Any, Throwable, SSIHistory] =
    getEventsForSSI(ssi).map { events => SSI.makeSSIHistory(ssi, events) }

  def getVDR(ref: RefVDR): zio.ZIO[Any, Throwable, VDR] = getEventsForVDR(ref)
    .flatMap { events =>
      events.headOption match
        case None => ZIO.succeed(VDR.init(ref)) // owner is missing
        case Some(headEvent) =>
          headEvent.event match {
            case _: CreateStorageEntryOP =>
              val didPrismOwner =
                headEvent.asInstanceOf[MySignedPrismEvent[CreateStorageEntryOP]].event.didPrism
              getSSIHistory(didPrismOwner).map { ssiHistory =>
                VDR.make(vdrRef = ref, ssiHistory = ssiHistory, ops = events)
              }
            case event => ???
          }
    }

}

object PrismState {
  def empty: PrismState = PrismStateInMemory.empty

  def forceType2DidEvent(seq: Seq[MySignedPrismEvent[OP]]): ZIO[Any, RuntimeException, Seq[
    MySignedPrismEvent[CreateDidOP | UpdateDidOP | DeactivateDidOP]
  ]] = {
    ZIO.foldLeft(seq)(Seq.empty[MySignedPrismEvent[OP.TypeDidEvent]]) { (s, sEvent) =>
      sEvent.event match
        case _: CreateDidOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeDidEvent]])
        case _: UpdateDidOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeDidEvent]])
        case _: DeactivateDidOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeDidEvent]])
        case sEvent => ZIO.fail(new RuntimeException("This Event is not a DID Event")) // FIXME
    }
  }

  def forceType2StorageEvent(seq: Seq[MySignedPrismEvent[OP]]): ZIO[Any, RuntimeException, Seq[
    MySignedPrismEvent[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP]
  ]] = {
    ZIO.foldLeft(seq)(Seq.empty[MySignedPrismEvent[OP.TypeStorageEntryEvent]]) { (s, sEvent) =>
      sEvent.event match
        case _: CreateStorageEntryOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeStorageEntryEvent]])
        case _: UpdateStorageEntryOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeStorageEntryEvent]])
        case _: DeactivateStorageEntryOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeStorageEntryEvent]])
        case sEvent => ZIO.fail(new RuntimeException("This Event is not a Storage Entry")) // FIXME
    }
  }

}

trait PrismState extends PrismStateRead { self =>
  // type This //Type member

  def addEvent(event: MySignedPrismEvent[OP]): PrismState

  def addMaybeEvent(maybeEvent: MaybeEvent[OP]): PrismState = maybeEvent match
    case _: InvalidPrismObject       => self
    case _: InvalidSignedPrismEvent  => self
    case aux: MySignedPrismEvent[OP] => addEvent(aux)
}
