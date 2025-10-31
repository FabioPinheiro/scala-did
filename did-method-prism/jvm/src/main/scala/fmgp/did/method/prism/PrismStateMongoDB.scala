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

  override def ssi2eventsRef: ZIO[Any, Nothing, Map[DIDSubject, Seq[EventRef]]] = ???
  override def vdr2eventsRef: ZIO[Any, Nothing, Map[RefVDR, Seq[EventRef]]] = ???

  override def getEventsIdBySSI(ssi: DIDSubject): ZIO[Any, Nothing, Seq[EventRef]] = ???
  override def getEventsIdByVDR(id: RefVDR): ZIO[Any, Nothing, Seq[EventRef]] = ???

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
  ]] = ???

  override def getEventsByHash(refHash: EventHash): ZIO[Any, Exception, Option[MySignedPrismEvent[OP]]] =
    for {
      _ <- ZIO.logInfo(s"getEventsByHash $refHash")
      ret <- findEventByHash(refHash)
    } yield ret.map(_.event)

  override def addEvent(event: MySignedPrismEvent[OP]): ZIO[Any, Exception, Unit] = {
    for {
      _ <- ZIO.logInfo(s"inserting event '${event.eventHash}'")
      coll <- collection
        .mapError(ex => StorageException(ex))
      maybePreviousEventHash = event.event match
        case VoidOP(reason)              => ???
        case e: CreateDidOP              => None
        case e: UpdateDidOP              => Some(EventHash.fromHex(e.previousEventHash))
        case e: IssueCredentialBatchOP   => None
        case e: RevokeCredentialsOP      => None
        case e: ProtocolVersionUpdateOP  => None
        case e: DeactivateDidOP          => Some(EventHash.fromHex(e.previousEventHash))
        case e: CreateStorageEntryOP     => None
        case e: UpdateStorageEntryOP     => Some(EventHash.fromHex(e.previousEventHash))
        case e: DeactivateStorageEntryOP => Some(EventHash.fromHex(e.previousEventHash))
      maybePreviousEvent <- maybePreviousEventHash match
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
  private def findEventByHash(refHash: EventHash): ZIO[Any, Exception, Option[EventWithRootRef]] =
    for {
      coll <- collection.mapError(ex => StorageException(ex))
      cursor = coll
        .find(selector = BSONDocument("_id" -> refHash))
        .cursor[EventWithRootRef]()
      maybeEventWithRootRef <-
        ZIO
          .fromFuture(implicit ec => cursor.collect(maxDocs = 1))
          .mapError(ex => StorageException(StorageThrowable(ex)))
          .map(_.headOption)
    } yield maybeEventWithRootRef

  private def findALlRelatedEvents(refHash: EventHash): ZIO[Any, Exception, Seq[EventWithRootRef]] =
    for {
      coll <- collection
        .mapError(ex => StorageException(ex))
      cursor = coll
        .find(selector = BSONDocument("refHash" -> refHash))
        .cursor[EventWithRootRef]()
      allRelatedEvents <-
        ZIO
          .fromFuture(implicit ec => cursor.collect(maxDocs = 1))
          .mapError(ex => StorageException(StorageThrowable(ex)))
    } yield allRelatedEvents.toSeq

  private def insertLostEvent(event: MySignedPrismEvent[OP]): ZIO[Any, StorageException, Unit] =
    for {
      coll <- lostCollection.mapError(ex => StorageException(ex))
      insertResult = ZIO
        .fromFuture(implicit ec => coll.insert.one(event))
        .tapError(err => ZIO.logError(s"fail to insert in lost colletion:  ${err.getMessage}"))
        .mapError(ex => StorageException(StorageThrowable(ex)))
    } yield ()

  private def insertEvent(event: EventWithRootRef): ZIO[Any, StorageException, Unit] =
    for {
      coll <- collection.mapError(ex => StorageException(ex))
      insertResult = ZIO
        .fromFuture(implicit ec => coll.insert.one(event))
        .tapError(err => ZIO.logError(s"Fail to insert event:  ${err.getMessage}"))
        .mapError(ex => StorageException(StorageThrowable(ex)))
    } yield ()
}
