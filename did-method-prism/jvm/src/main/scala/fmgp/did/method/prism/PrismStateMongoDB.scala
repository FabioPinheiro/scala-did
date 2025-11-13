package fmgp.did.method.prism

import zio.*
import zio.json.*
import fmgp.did.DIDSubject
import fmgp.did.method.prism.*
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto.*
import fmgp.did.method.prism.mongo.*
import fmgp.did.method.prism.mongo.DataModels.given

import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, CursorProducer}

case class PrismStateMongoDB(reactiveMongoApi: ReactiveMongoApi) extends PrismState {

  def collectionName: String = "events"
  def lostCollectionName: String = collectionName + "_lost"

  def collection: IO[StorageCollection, BSONCollection] = reactiveMongoApi.database
    .map(_.collection(collectionName))
    .mapError(ex => StorageCollection(ex))

  def lostCollection: IO[StorageCollection, BSONCollection] = reactiveMongoApi.database
    .map(_.collection(lostCollectionName))
    .mapError(ex => StorageCollection(ex))

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

  override def addEvent(event: MySignedPrismEvent[OP]): ZIO[Any, Exception, Unit] = {
    for {
      _ <- ZIO.logInfo(s"inserting event '${event.eventHash}'")
      coll <- collection
        .mapError(ex => StorageException(ex))
      maybePreviousEventHash <- event.event match
        case VoidOP(reason)              => ZIO.fail(new RuntimeException(s"VoidOP not supported: $reason"))
        case e: CreateDidOP              => ZIO.none
        case e: UpdateDidOP              => ZIO.some(EventHash.fromHex(e.previousEventHash))
        case e: IssueCredentialBatchOP   => ZIO.none
        case e: RevokeCredentialsOP      => ZIO.none
        case e: ProtocolVersionUpdateOP  => ZIO.none
        case e: DeactivateDidOP          => ZIO.some(EventHash.fromHex(e.previousEventHash))
        case e: CreateStorageEntryOP     => ZIO.none
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

  // ## DB methods ##

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

  private[prism] def insertEvent(event: EventWithRootRef): ZIO[Any, StorageException, Unit] =
    for {
      coll <- collection.mapError(ex => StorageException(ex))
      insertResult <- ZIO
        .fromFuture(implicit ec => coll.insert.one(event))
        .tapError(err => ZIO.logError(s"Fail to insert event:  ${err.getMessage}"))
        .mapError(ex => StorageException(StorageThrowable(ex)))
    } yield ()

  private[prism] def insertLostEvent(event: MySignedPrismEvent[OP]): ZIO[Any, StorageException, Unit] =
    for {
      coll <- lostCollection.mapError(ex => StorageException(ex))
      insertResult <- ZIO
        .fromFuture(implicit ec => coll.insert.one(event))
        .tapError(err => ZIO.logError(s"fail to insert in lost colletion:  ${err.getMessage}"))
        .mapError(ex => StorageException(StorageThrowable(ex)))
    } yield ()
}
