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

  // TODO make it a Stream of (DIDSubject, Seq[EventRef]])
  def ssi2eventsRef: ZIO[Any, Nothing, Map[DIDSubject, Seq[EventRef]]] // : Map[DIDSubject, Seq[EventRef]]
  // TODO make it a Stream of (RefVDR, Seq[EventRef]])
  def vdr2eventsRef: ZIO[Any, Nothing, Map[RefVDR, Seq[EventRef]]] // : Map[DIDSubject, Seq[EventRef]]

  def ssiCount: ZIO[Any, Nothing, Int] = ssi2eventsRef.map(_.size) // TODO improve in the specific implementation
  def vdrCount: ZIO[Any, Nothing, Int] = vdr2eventsRef.map(_.size) // TODO improve in the specific implementation

  def getEventsIdBySSI(ssi: DIDSubject): ZIO[Any, Nothing, Seq[EventRef]]
  def getEventsIdByVDR(id: RefVDR): ZIO[Any, Nothing, Seq[EventRef]]

  def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[MySignedPrismEvent[
    CreateDidOP | UpdateDidOP | DeactivateDidOP
  ]]] =
    getEventsIdBySSI(ssi).flatMap { seqEventRef =>
      for {
        events <- ZIO.foreach(seqEventRef) { eventRef =>
          getEventsByHash(eventRef.eventHash).flatMap {
            case None =>
              ZIO.fail(new RuntimeException(s"impossible state: missing Event hash '${eventRef.eventHash}'"))
            case Some(value) => ZIO.succeed(value)
          }
        }
        didEvents <- PrismState.forceType2DidEvent(events)
      } yield didEvents
    }

  def getEventsForVDR(
      refVDR: RefVDR
  ): ZIO[Any, Throwable, Seq[MySignedPrismEvent[
    CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP
  ]]] =
    getEventsIdByVDR(refVDR).flatMap { seqEventRef =>
      for {
        events <- ZIO.foreach(seqEventRef) { eventRef =>
          getEventsByHash(eventRef.eventHash).flatMap {
            case None =>
              ZIO.fail(new RuntimeException(s"impossible state: missing Event hash '${eventRef.eventHash}'"))
            case Some(value) => ZIO.succeed(value)
          }
        }
        storageEvents <- PrismState.forceType2StorageEvent(events)
      } yield storageEvents
    }

  def getEventsByHash(refHash: EventHash): ZIO[Any, Exception, Option[MySignedPrismEvent[OP]]]

  def getSSI(ssi: DIDSubject): ZIO[Any, Throwable, SSI] =
    getSSIHistory(ssi).map(_.latestVersion) // getEventsForSSI(ssi).map { events => SSI.make(ssi, events) }

  def getSSIHistory(ssi: DIDSubject): ZIO[Any, Throwable, SSIHistory] =
    getEventsForSSI(ssi).map { events => SSI.makeSSIHistory(ssi, events) }

  def getVDR(ref: RefVDR): zio.ZIO[Any, Throwable, VDR] = getEventsForVDR(ref)
    .flatMap { events =>
      events.headOption match
        case None            => ZIO.succeed(VDR.init(ref)) // owner is missing
        case Some(headEvent) =>
          headEvent.event match {
            case _: CreateStorageEntryOP =>
              val didPrismOwner =
                headEvent.asInstanceOf[MySignedPrismEvent[CreateStorageEntryOP]].event.didPrism
              getSSIHistory(didPrismOwner).map { ssiHistory =>
                VDR.make(vdrRef = ref, ssiHistory = ssiHistory, events = events)
              }
            case event => // this should never happen
              ZIO.fail(new RuntimeException("The first event of the VDR MUST be a CreateStorageEntry"))
          }
    }

}

object PrismState {

  def forceType2DidEvent(seq: Seq[MySignedPrismEvent[OP]]): ZIO[Any, RuntimeException, Seq[
    MySignedPrismEvent[CreateDidOP | UpdateDidOP | DeactivateDidOP]
  ]] = {
    ZIO.foldLeft(seq)(Seq.empty[MySignedPrismEvent[OP.TypeDIDEvent]]) { (s, sEvent) =>
      sEvent.event match
        case _: CreateDidOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeDIDEvent]])
        case _: UpdateDidOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeDIDEvent]])
        case _: DeactivateDidOP =>
          ZIO.succeed(s :+ sEvent.asInstanceOf[MySignedPrismEvent[OP.TypeDIDEvent]])
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

  def addEvent(event: MySignedPrismEvent[OP]): ZIO[Any, Exception, Unit]

  def addMaybeEvent(maybeEvent: MaybeEvent[OP]): ZIO[Any, Exception, Unit] = maybeEvent match
    case _: InvalidPrismObject       => ZIO.unit // self
    case _: InvalidSignedPrismEvent  => ZIO.unit // self
    case aux: MySignedPrismEvent[OP] => addEvent(aux)
}
