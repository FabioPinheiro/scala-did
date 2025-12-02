package fmgp.did.method.prism

import zio.*
import zio.json.*
import fmgp.did.DIDSubject
import fmgp.did.method.prism.*
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto.*
import fmgp.did.method.prism.mongo.*
import fmgp.did.method.prism.mongo.DataModels.given

import reactivemongo.api.{AsyncDriver, Cursor, CursorProducer}
import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import fmgp.did.method.prism.cardano.EventCursor
import reactivemongo.core.errors.DatabaseException

object PrismStateMongoDB {

  /** makeLayer create a PrismStateMongoDB from a mongoDBConnection
    *
    * For the AsyncDriverResource make sure to include the reactivemongo dependencies. It's assumed this dependency is
    * 'provided': {{{libraryDependencies += "org.reactivemongo" %% "reactivemongo" % V.reactivemongo}}}
    *
    * {{{
    * val mongoDBConnection = "mongodb+srv://readonly:readonly@cluster0.bgnyyy1.mongodb.net/indexer"
    * AsyncDriverResource.layer >>> PrismStateMongoDB.makeLayer(mongoDBConnection)
    * }}}
    */
  def makeLayer(url: String): ZLayer[AsyncDriver, Throwable, PrismState] = {
    ReactiveMongoApi.layer(url) >>> // "mongodb+srv://user:password@cluster0.bgnyyy1.mongodb.net/test"
      ZLayer.fromZIO(ZIO.service[ReactiveMongoApi].map(reactiveMongoApi => PrismStateMongoDB(reactiveMongoApi)))
  }

  def makeReadOnlyLayer(url: String): ZLayer[AsyncDriver, Throwable, PrismStateRead] = {
    ReactiveMongoApi.layer(url) >>> // "mongodb+srv://user:password@cluster0.bgnyyy1.mongodb.net/test"
      ZLayer.fromZIO(ZIO.service[ReactiveMongoApi].map(reactiveMongoApi => PrismStateReadMongoDB(reactiveMongoApi)))
  }
}

private[prism] trait PrismStateReadMongoDBTrait extends PrismStateRead {
  def reactiveMongoApi: ReactiveMongoApi

  /** Name of the main events collection. */
  def collectionName: String = "events"

  /** Returns the main events MongoDB collection. */
  def collection: IO[StorageCollection, BSONCollection] = reactiveMongoApi.database
    .map(_.collection(collectionName))
    .mapError(ex => StorageCollection(ex))

  /** Returns the cursor of the latest event stored in the database.
    *
    * Uses MongoDB sort to efficiently find the maximum EventCursor by (b, o) indices. If no events exist, returns
    * EventCursor.init (-1, -1).
    */
  override def cursor: ZIO[Any, Nothing, cardano.EventCursor]

  override def ssi2eventsRef: ZIO[Any, Nothing, Map[DIDSubject, Seq[EventRef]]] = {
    (for {
      coll <- collection.mapError(ex => StorageException(ex))
      cursor = coll
        .find(selector = BSONDocument.empty)
        .cursor[EventWithRootRef]()
      allEvents <- ZIO
        .fromFuture(implicit ec => cursor.collect(maxDocs = -1))
        .mapError(ex => StorageException(StorageThrowable(ex)))
      // Filter for DID events and group by rootRef
      didEvents = allEvents
        .filter(e => e.event.event.isDIDEvent)
        .groupBy(_.rootRef)
        .map { case (rootRef, events) =>
          val didSubject = DIDPrism(rootRef.hex).asDIDSubject
          val eventRefs = events
            .map(e => EventRef(e.event.b, e.event.o, e.event.eventHash))
            .sortBy(ref => (ref.b, ref.o))
            .toSeq
          (didSubject, eventRefs)
        }
    } yield didEvents).orDie // Convert all errors to defects since return type is Nothing
  }
  override def vdr2eventsRef: ZIO[Any, Nothing, Map[RefVDR, Seq[EventRef]]] = {
    (for {
      coll <- collection.mapError(ex => StorageException(ex))
      cursor = coll
        .find(selector = BSONDocument.empty)
        .cursor[EventWithRootRef]()
      allEvents <- ZIO
        .fromFuture(implicit ec => cursor.collect(maxDocs = -1))
        .mapError(ex => StorageException(StorageThrowable(ex)))
      // Filter for storage events and group by rootRef
      vdrEvents = allEvents
        .filter(e => e.event.event.isStorageEvent)
        .groupBy(_.rootRef)
        .map { case (rootRef, events) =>
          val vdrRef = RefVDR(rootRef.hex)
          val eventRefs = events
            .map(e => EventRef(e.event.b, e.event.o, e.event.eventHash))
            .sortBy(ref => (ref.b, ref.o))
            .toSeq
          (vdrRef, eventRefs)
        }
    } yield vdrEvents).orDie // Convert all errors to defects since return type is Nothing
  }

  override def getEventsIdBySSI(ssi: DIDSubject): ZIO[Any, Nothing, Seq[EventRef]] = {
    (for {
      prism <- ZIO
        .fromEither(DIDPrism.fromDID(ssi))
        .map(did => EventHash.fromPRISM(did))
        .mapError(errorStr => new RuntimeException(errorStr))
      events <- findALlRelatedEvents(prism)
      eventRefs = events
        .map(e => EventRef(e.event.b, e.event.o, e.event.eventHash))
        .sortBy(ref => (ref.b, ref.o))
    } yield eventRefs).orDie // FIXME Convert all errors to defects since return type is Nothing
  }
  override def getEventsIdByVDR(id: RefVDR): ZIO[Any, Nothing, Seq[EventRef]] = {
    (for {
      rootHash <- ZIO.succeed(EventHash.fromHex(id.hex))
      events <- findALlRelatedEvents(rootHash)
      eventRefs = events
        .map(e => EventRef(e.event.b, e.event.o, e.event.eventHash))
        .sortBy(ref => (ref.b, ref.o))
    } yield eventRefs).orDie // FIXME Convert all errors to defects since return type is Nothing
  }

  override def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[
    MySignedPrismEvent[CreateDidOP | UpdateDidOP | DeactivateDidOP]
  ]] = {
    for {
      prism <- ZIO
        .fromEither(DIDPrism.fromDID(ssi))
        .map(did => EventHash.fromPRISM(did))
        .mapError(errorStr => new RuntimeException(errorStr)) // FIXME
      seq <- findALlRelatedEvents(prism)
      tmp = seq
        .map(_.event)
        .filter(_.event.isDIDEvent)
        .map(_.asSignedPrismDIDEvent)
      seqSignedPrismEvent <- ZIO.foreach(tmp) {
        case Left(errorStr)          => ZIO.dieMessage(errorStr) // FIXME
        case Right(signedPrismEvent) => ZIO.succeed(signedPrismEvent)
      }
    } yield (seqSignedPrismEvent)
  }

  override def getEventsForVDR(refVDR: RefVDR): ZIO[Any, Throwable, Seq[
    MySignedPrismEvent[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP]
  ]] = {
    for {
      rootHash <- ZIO.succeed(EventHash.fromHex(refVDR.hex))
      seq <- findALlRelatedEvents(rootHash)
      tmp = seq
        .map(_.event)
        .filter(_.event.isStorageEvent)
        .map(_.asSignedPrismStorageEntryEvent)
      seqSignedPrismEvent <- ZIO.foreach(tmp) {
        case Left(errorStr)          => ZIO.dieMessage(errorStr) // FIXME
        case Right(signedPrismEvent) => ZIO.succeed(signedPrismEvent)
      }
    } yield (seqSignedPrismEvent)
  }

  override def getEventByHash(refHash: EventHash): ZIO[Any, Exception, Option[MySignedPrismEvent[OP]]] =
    for {
      _ <- ZIO.logInfo(s"getEventByHash $refHash")
      ret <- findEventByHash(refHash)
    } yield ret.map(_.event)

  // ## DB methods ##

  /** Finds event by hash in main collection. */
  private[prism] def findEventByHash(refHash: EventHash): ZIO[Any, Exception, Option[EventWithRootRef]] =
    for {
      coll <- collection.mapError(ex => StorageException(ex))
      cursor = coll.find(selector = BSONDocument("_id" -> refHash)).cursor[EventWithRootRef]()
      maybeEventWithRootRef <-
        ZIO
          .fromFuture(implicit ec => cursor.collect(maxDocs = 1))
          .mapError(ex => StorageException(StorageThrowable(ex)))
          .map(_.headOption)
    } yield maybeEventWithRootRef

  /** Finds all events in a chain by rootRef. */
  private[prism] def findALlRelatedEvents(rootHash: EventHash): ZIO[Any, Exception, Seq[EventWithRootRef]] =
    for {
      coll <- collection
        .mapError(ex => StorageException(ex))
      cursor = coll
        .find(selector = BSONDocument("ref" -> rootHash))
        .cursor[EventWithRootRef]()
      allRelatedEvents <-
        ZIO
          .fromFuture(implicit ec => cursor.collect(maxDocs = -1))
          .mapError(ex => StorageException(StorageThrowable(ex)))
    } yield allRelatedEvents.toSeq

  /** Inserts event with rootRef into main collection. */
  private[prism] def insertEvent(eventWithRootRef: EventWithRootRef): ZIO[Any, StorageException, Unit] =
    for {
      coll <- collection.mapError(ex => StorageException(ex))
      insertResult <- ZIO
        .fromFuture(implicit ec => coll.insert.one(eventWithRootRef))
        .catchSome {
          case obj: DatabaseException if obj.code.contains(11000) => // 'E11000 duplicate key error collection
            ZIO.logWarning(
              s"E11000 duplicate key when inserting ${eventWithRootRef.event.eventCursor}:'${eventWithRootRef.event.eventHash.hex}'"
            )
        }
        .tapError(err => ZIO.logError(s"Fail to insert event: ${err.getMessage}"))
        .mapError(ex => StorageException(StorageThrowable(ex)))
    } yield ()

}

/** PrismStateReadMongoDB is like [[PrismStateMongoDB]] but only the read part */
case class PrismStateReadMongoDB(reactiveMongoApi: ReactiveMongoApi) extends PrismStateReadMongoDBTrait {

  override def cursor: ZIO[Any, Nothing, cardano.EventCursor] = {
    (for {
      coll <- collection.mapError(ex => StorageException(ex))
      mEventCursor <- ZIO
        .fromFuture(implicit ec =>
          coll
            .find(selector = BSONDocument.empty)
            .sort(BSONDocument("b" -> -1, "o" -> -1)) // Sort by b desc, then o desc
            .one[EventCursor]
        )

      latestCursor = (Seq(cardano.EventCursor.init) ++ mEventCursor).sorted.last
    } yield latestCursor).orDie // Convert all errors to defects since return type is Nothing
  }

}

/** MongoDB-backed implementation of [[PrismState]] for production use.
  *
  * This implementation uses a sophisticated two-collection strategy to Debug out-of-order blockchain events:
  *
  * ==Collections==
  *
  * '''Main Collection (events):''' Stores events with known parent relationships. Each event is stored with a `rootRef`
  * field that tracks the root event (Create operation) of its chain. This enables efficient querying of all events for
  * a DID or VDR by querying on the `rootRef` field.
  *
  * '''Lost Collection (events_lost):''' Stores orphaned events whose parent (referenced via `previousEventHash`) has
  * not yet been seen. This handles the case where Update or Deactivate operations arrive before their corresponding
  * Create operation.
  *
  * ==Event Chain Tracking==
  *
  * Events form chains through `previousEventHash` references:
  *   - Create operations (CreateDidOP, CreateStorageEntryOP) start chains and have no previous hash
  *   - Update/Deactivate operations reference the previous event's hash
  *   - All events in a chain share the same `rootRef` (the hash of the Create event)
  *
  * When adding an event:
  *   1. If it has no `previousEventHash`, it's inserted with `rootRef = eventHash`
  *   1. If it has a `previousEventHash`, we search for the parent event
  *   1. If parent exists, event is inserted with parent's `rootRef`
  *   1. If parent is missing, event goes to lost collection for potential later recovery
  *
  * @constructor
  *   Creates a MongoDB-backed PRISM state
  * @param reactiveMongoApi
  *   MongoDB connection wrapper using ReactiveMongo driver
  *
  * @example
  *   {{{
  * val mongoApi: ReactiveMongoApi = ???
  * val state = PrismStateMongoDB(mongoApi)
  * for {
  *   _ <- state.addEvent(createEvent)
  *   _ <- state.addEvent(updateEvent)
  *   ssi <- state.getSSI(didSubject)
  * } yield ssi
  *   }}}
  */
case class PrismStateMongoDB(reactiveMongoApi: ReactiveMongoApi) extends PrismState with PrismStateReadMongoDBTrait {

  /** Name of the lost/orphaned events collection. */
  def lostCollectionName: String = collectionName + "_lost"

  /** Returns the lost events MongoDB collection. */
  def lostCollection: IO[StorageCollection, BSONCollection] = reactiveMongoApi.database
    .map(_.collection(lostCollectionName))
    .mapError(ex => StorageCollection(ex))

  override def cursor: ZIO[Any, Nothing, cardano.EventCursor] = {
    (for {
      coll <- collection.mapError(ex => StorageException(ex))
      mEventCursor <- ZIO
        .fromFuture(implicit ec =>
          coll
            .find(selector = BSONDocument.empty)
            .sort(BSONDocument("b" -> -1, "o" -> -1)) // Sort by b desc, then o desc
            .one[EventCursor]
        )
      lostColl <- lostCollection.mapError(ex => StorageException(ex))
      mLostEventCursor <- ZIO
        .fromFuture(implicit ec =>
          lostColl
            .find(selector = BSONDocument.empty)
            .sort(BSONDocument("b" -> -1, "o" -> -1)) // Sort by b desc, then o desc
            .one[EventCursor]
        )
      latestCursor = (Seq(cardano.EventCursor.init) ++ mEventCursor ++ mLostEventCursor).sorted.last
    } yield latestCursor).orDie // Convert all errors to defects since return type is Nothing
  }

  /** Adds event to MongoDB with automatic rootRef tracking.
    *
    * Implements two-collection strategy:
    *   - Events with no `previousEventHash` (Create ops) go to main collection with `rootRef = eventHash`
    *   - Events with `previousEventHash` look up the parent:
    *     - If parent exists, event goes to main collection with parent's `rootRef`
    *     - If parent missing, event goes to lost collection
    *
    * @note
    *   VoidOP operations are not supported and will fail with RuntimeException
    */
  override def addEvent(event: MySignedPrismEvent[OP]): ZIO[Any, Exception, Unit] = {
    for {
      _ <- ZIO.logInfo(s"inserting event '${event.eventHash.hex}'")
      coll <- collection
        .mapError(ex => StorageException(ex))
      maybePreviousEventHash <- event.event match
        case VoidOP(reason)              => ZIO.none // ZIO.fail(new RuntimeException(s"VoidOP not supported: $reason"))
        case e: CreateDidOP              => ZIO.none // Create has no previousEventHash
        case e: UpdateDidOP              => ZIO.some(EventHash.fromHex(e.previousEventHash))
        case e: IssueCredentialBatchOP   => ZIO.none // TODO
        case e: RevokeCredentialsOP      => ZIO.none // TODO
        case e: ProtocolVersionUpdateOP  => ZIO.none // TODO
        case e: DeactivateDidOP          => ZIO.some(EventHash.fromHex(e.previousEventHash))
        case e: CreateStorageEntryOP     => ZIO.none // Create has no previousEventHash
        case e: UpdateStorageEntryOP     => ZIO.some(EventHash.fromHex(e.previousEventHash))
        case e: DeactivateStorageEntryOP => ZIO.some(EventHash.fromHex(e.previousEventHash))
      _ <- maybePreviousEventHash match
        case None => // Have no previousEvent so we can just insert
          insertEvent(EventWithRootRef(rootRef = event.eventHash, event))
        case Some(previousEventHash) =>
          findEventByHash(previousEventHash).flatMap {
            case Some(previousEvent) =>
              insertEvent(EventWithRootRef(rootRef = previousEvent.rootRef, event))
            case None =>
              ZIO.logWarning(
                s"Event '${event.eventHash}' has previousEvent '${previousEventHash}' but is not in DB."
              ) *> insertLostEvent(event)
          }
    } yield ()
  }

  /** Inserts orphaned event into lost collection.
    *
    * Used when an event's parent is not found in the main collection.
    */
  private[prism] def insertLostEvent(event: MySignedPrismEvent[OP]): ZIO[Any, StorageException, Unit] =
    for {
      coll <- lostCollection.mapError(ex => StorageException(ex))
      insertResult <- ZIO
        .fromFuture(implicit ec => coll.insert.one(event))
        .catchSome {
          case obj: DatabaseException if obj.code.contains(11000) => // 'E11000 duplicate key error collection
            ZIO.logWarning(
              s"E11000 duplicate key when inserting ${event.eventCursor}:'${event.eventHash.hex}'"
            )
        }
        .tapError(err => ZIO.logError(s"fail to insert in lost colletion: ${err.getMessage}"))
        .mapError(ex => StorageException(StorageThrowable(ex)))
    } yield ()

  /** Finds event by hash in lost collection. */
  private[prism] def findLostEventByHash(refHash: EventHash): ZIO[Any, Exception, Option[MySignedPrismEvent[OP]]] =
    for {
      coll <- lostCollection.mapError(ex => StorageException(ex))
      cursor = coll.find(selector = BSONDocument("_id" -> refHash)).cursor[MySignedPrismEvent[OP]]()
      maybeEventWithRootRef <-
        ZIO
          .fromFuture(implicit ec => cursor.collect(maxDocs = 1))
          .mapError(ex => StorageException(StorageThrowable(ex)))
          .map(_.headOption)
    } yield maybeEventWithRootRef
}
