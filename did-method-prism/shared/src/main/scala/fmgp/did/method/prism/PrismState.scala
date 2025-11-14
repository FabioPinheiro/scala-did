package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did.method.prism._
import fmgp.did.DIDSubject
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto._

/** Read-only interface for querying PRISM blockchain state.
  *
  * This trait provides methods to query the state of PRISM DIDs (Decentralized Identifiers) and VDRs (Verifiable Data
  * Registries) from an event-sourced storage backend. Events represent blockchain operations (create, update,
  * deactivate) that form immutable chains tracked through hash references.
  *
  * ==Key Concepts==
  *
  * '''SSI (Self-Sovereign Identity):''' A PRISM DID (e.g., `did:prism:xxx`) managed through [[CreateDidOP]],
  * [[UpdateDidOP]], and [[DeactivateDidOP]] events. SSIs represent identity with keys and services.
  *
  * '''VDR (Verifiable Data Registry):''' Storage entries owned by a DID, managed through [[CreateStorageEntryOP]],
  * [[UpdateStorageEntryOP]], and [[DeactivateStorageEntryOP]] events. Used for storing arbitrary data on-chain.
  *
  * '''Event Chains:''' Operations form chains via `previousEventHash` references. Create operations start chains;
  * update/deactivate operations reference previous events. All events in a chain share the same root hash.
  *
  * @example
  *   {{{
  * val state: PrismStateRead = ???
  * for {
  *   events <- state.getEventsForSSI(didSubject)
  *   ssi <- state.getSSI(didSubject)
  *   history <- state.getSSIHistory(didSubject)
  * } yield (events, ssi, history)
  *   }}}
  */
trait PrismStateRead {

  /** Returns the last synced block timestamp.
    *
    * @return
    *   Tuple of (epoch seconds, nanoseconds)
    * @note
    *   Current implementation returns current time (FIXME: should track actual sync state)
    */
  def lastSyncedBlockEpochSecondNano: (Long, Int) = {
    val now = java.time.Instant.now // FIXME
    (now.getEpochSecond, now.getNano)
  }

  /** Returns mapping of all SSI DIDs to their event references.
    *
    * @note
    *   TODO: make it a Stream of (DIDSubject, Seq[EventRef])
    */
  def ssi2eventsRef: ZIO[Any, Nothing, Map[DIDSubject, Seq[EventRef]]] // : Map[DIDSubject, Seq[EventRef]]

  /** Returns mapping of all VDRs to their event references.
    *
    * @note
    *   TODO: make it a Stream of (RefVDR, Seq[EventRef])
    */
  def vdr2eventsRef: ZIO[Any, Nothing, Map[RefVDR, Seq[EventRef]]] // : Map[DIDSubject, Seq[EventRef]]

  /** Returns count of SSI DIDs in the state.
    *
    * @note
    *   TODO: improve in specific implementations for better performance
    */
  def ssiCount: ZIO[Any, Nothing, Int] = ssi2eventsRef.map(_.size)

  /** Returns count of VDRs in the state.
    *
    * @note
    *   TODO: improve in specific implementations for better performance
    */
  def vdrCount: ZIO[Any, Nothing, Int] = vdr2eventsRef.map(_.size)

  /** Returns event references for a specific SSI DID. */
  def getEventsIdBySSI(ssi: DIDSubject): ZIO[Any, Nothing, Seq[EventRef]]

  /** Returns event references for a specific VDR. */
  def getEventsIdByVDR(id: RefVDR): ZIO[Any, Nothing, Seq[EventRef]]

  /** Fetches all events in the chain for the given DID.
    *
    * @return
    *   ZIO effect that may fail with `Throwable` (RuntimeException if event hash missing or type validation fails) and
    *   succeeds with sequence of DID-related [[MySignedPrismEvent]]
    */
  def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[MySignedPrismEvent[
    CreateDidOP | UpdateDidOP | DeactivateDidOP
  ]]] =
    getEventsIdBySSI(ssi).flatMap { seqEventRef =>
      for {
        events <- ZIO.foreach(seqEventRef) { eventRef =>
          getEventByHash(eventRef.eventHash).flatMap {
            case None =>
              ZIO.fail(new RuntimeException(s"impossible state: missing Event hash '${eventRef.eventHash}'"))
            case Some(value) => ZIO.succeed(value)
          }
        }
        didEvents <- PrismState.forceType2DidEvent(events)
      } yield didEvents
    }

  /** Fetches all events in the chain for the given VDR reference.
    *
    * @param refVDR
    *   The VDR reference to query
    * @return
    *   ZIO effect that may fail with `Throwable` (RuntimeException if event hash missing or type validation fails) and
    *   succeeds with sequence of storage-related [[MySignedPrismEvent]]
    */
  def getEventsForVDR(
      refVDR: RefVDR
  ): ZIO[Any, Throwable, Seq[MySignedPrismEvent[
    CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP
  ]]] =
    getEventsIdByVDR(refVDR).flatMap { seqEventRef =>
      for {
        events <- ZIO.foreach(seqEventRef) { eventRef =>
          getEventByHash(eventRef.eventHash).flatMap {
            case None =>
              ZIO.fail(new RuntimeException(s"impossible state: missing Event hash '${eventRef.eventHash}'"))
            case Some(value) => ZIO.succeed(value)
          }
        }
        storageEvents <- PrismState.forceType2StorageEvent(events)
      } yield storageEvents
    }

  /** Gets a specific event by its [[EventHash]]. */
  def getEventByHash(refHash: EventHash): ZIO[Any, Exception, Option[MySignedPrismEvent[OP]]]

  /** Gets the current SSI state. */
  def getSSI(ssi: DIDSubject): ZIO[Any, Throwable, SSI] =
    getSSIHistory(ssi).map(_.latestVersion) // getEventsForSSI(ssi).map { events => SSI.make(ssi, events) }

  /** Gets the full [[SSIHistory]] for a SSI. */
  def getSSIHistory(ssi: DIDSubject): ZIO[Any, Throwable, SSIHistory] =
    getEventsForSSI(ssi).map { events => SSI.makeSSIHistory(ssi, events) }

  /** Gets the [[VDR]] state with full ownership validation. */
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

/** Writable PRISM state interface extending [[PrismStateRead]] with event storage capabilities.
  *
  * Adds methods to persist blockchain events to the state backend. Events are added without validation, allowing
  * implementations to handle ordering and chain integrity according to their storage strategy.
  *
  * @example
  *   {{{
  * val state: PrismState = ???
  * val event: MySignedPrismEvent[CreateDidOP] = ???
  * for {
  *   _ <- state.addEvent(event)
  *   ssi <- state.getSSI(event.event.didPrism)
  * } yield ssi
  *   }}}
  */
trait PrismState extends PrismStateRead { self =>
  // type This //Type member

  /** Adds a signed PRISM event to the state.
    *
    * Events are indexed without validation. Implementations may handle out-of-order events differently.
    */
  def addEvent(event: MySignedPrismEvent[OP]): ZIO[Any, Exception, Unit]

  /** Adds an event after filtering out invalid ones.
    *
    * Only valid [[MySignedPrismEvent]] instances are added; invalid objects are silently ignored.
    */
  def addMaybeEvent(maybeEvent: MaybeEvent[OP]): ZIO[Any, Exception, Unit] = maybeEvent match
    case _: InvalidPrismObject       => ZIO.unit // self
    case _: InvalidSignedPrismEvent  => ZIO.unit // self
    case aux: MySignedPrismEvent[OP] => addEvent(aux)
}
