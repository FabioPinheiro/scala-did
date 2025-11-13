package fmgp.did.method.prism

import munit.*
import zio.*
import zio.stream.*
import fmgp.did.method.prism.mongo.*
import fmgp.util.hex2bytes
import _root_.proto.prism.SignedPrismEvent
import fmgp.did.method.prism.proto.MaybeEvent
import fmgp.did.method.prism.proto.InvalidPrismObject
import fmgp.did.method.prism.proto.InvalidSignedPrismEvent
import fmgp.did.method.prism.proto.MySignedPrismEvent
import fmgp.did.method.prism.proto.OP
import fmgp.did.method.prism.proto.CreateStorageEntryOP
import fmgp.did.method.prism.proto.UpdateStorageEntryOP
import fmgp.did.method.prism.vdr.VDRUtilsTestExtra
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.BSONDocument
import fmgp.util.bytes2Hex

import fmgp.did.method.prism.vdr.VDRUtilsTestExtra.{
  createSSI,
  createVDR,
  updateVDR,
  updateVDR_withTheNewKey,
  updateSSI_addKey
}

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.PrismStateMongoDBSuite
  */
class PrismStateMongoDBSuite extends ZSuite {

  val makePrismState = ZLayer.fromZIO(
    for {
      reactiveMongoApi <- ZIO.service[ReactiveMongoApi]
      state = PrismStateMongoDB(reactiveMongoApi)
    } yield state: PrismState
  )
  val mongo = AsyncDriverResource.layer >>> ReactiveMongoApi
    .layer("mongodb+srv://fabio:ZiT61pB5@cluster0.bgnyyy1.mongodb.net/test")

  val prismStateFixture: FunFixture[ULayer[PrismState]] =
    ZTestLocalFixture { _ =>
      for {
        prismStateLayer <- ZIO.succeed(mongo.orDie >>> makePrismState)
        cleanJob <- ZIO // Cleanup: clear collections before each test
          .serviceWithZIO[PrismState] { state =>
            state match {
              case mongoState: PrismStateMongoDB =>
                (clearCollection(mongoState.collection) *> clearCollection(mongoState.lostCollection)).orDie
              case _ => ZIO.unit
            }
          }
          .provideLayer(prismStateLayer)
      } yield prismStateLayer
    } { _ => ZIO.unit }

  /** Clear all events from the collection (for testing) */
  def clearCollection(c: IO[StorageCollection, BSONCollection]): ZIO[Any, StorageException, Unit] =
    for {
      coll <- c.mapError(ex => StorageException(ex))
      _ <- ZIO
        .fromFuture(implicit ec => coll.delete().one(BSONDocument.empty))
        .mapError(ex => StorageException(StorageThrowable(ex)))
      _ <- ZIO.log(s"Collection '${coll.name}' deleted")
    } yield ()

  val aux = Seq(createSSI, createVDR, updateVDR, updateVDR_withTheNewKey, updateSSI_addKey).zipWithIndex
    .map { (hex, index) => VDRUtilsTestExtra.makeSignedPrismEvent(hex, opIndex = index) }

  // Test data summary:
  // aux(0): CreateDidOP -> DID creation
  // aux(1): CreateStorageEntryOP -> VDR creation
  // aux(2): UpdateStorageEntryOP -> VDR update
  // aux(3): UpdateStorageEntryOP -> VDR update with new key
  // aux(4): UpdateDidOP -> DID update (add key)

  prismStateFixture.testZLayered(
    "addEvent: insert CreateDidOP event"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      createDidEvent = aux(0) // CreateDidOP
      _ <- state.addEvent(createDidEvent) // no exceptions thrown
    } yield ()
  }

  prismStateFixture.testZLayered(
    "addEvent createSSI & findEventByHash & getEventsIdBySSI & getEventsForSSI"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      createSSIEvent = VDRUtilsTestExtra.makeSignedPrismEvent(createSSI, opIndex = 0)
      // updateSSIEvent = VDRUtilsTestExtra.makeSignedPrismEvent(updateSSI_addKey, opIndex = 1)
      eventHash = createSSIEvent.eventHash

      _ <- state.addEvent(createSSIEvent)
      // _ <- state.addEvent(updateSSIEvent)
      retrieved <- state.getEventByHash(eventHash)
      _ = retrieved match
        case None =>
          fail(s"Event ${eventHash.hex} not found after insertion")
        case Some(mySignedPrismEvent) =>
          assertEquals(
            obtained = retrieved.get.eventHash.hex,
            expected = eventHash.hex,
            clue = s"SSI event hash mismatch: ${retrieved.get.eventHash.hex} != ${eventHash.hex}"
          )
      didSubject = VDRUtilsTestExtra.didPrism.asDIDSubject
      eventRefs1 <- state.getEventsIdBySSI(didSubject) // Test getEventsIdBySSI
      eventRefs2 <- state.getEventsForSSI(didSubject) // Test getEventsForSSI
      ssi <- state.getSSI(didSubject) // Test getSSI
      _ = assertEquals(eventRefs1.size, 1)
      _ = assertEquals(eventRefs2.size, 1)
      _ = assertEquals(ssi.did, didSubject)
      _ = assertEquals(ssi.keys.size, 2)
    } yield ()
  }

  prismStateFixture.testZLayered(
    "addEvent (createSSI & updateSSI) & findEventByHash & getEventsIdBySSI & getEventsForSSI"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      createSSIEvent = VDRUtilsTestExtra.makeSignedPrismEvent(createSSI, opIndex = 0)
      updateSSIEvent = VDRUtilsTestExtra.makeSignedPrismEvent(updateSSI_addKey, opIndex = 1)
      eventHash = createSSIEvent.eventHash

      _ <- state.addEvent(createSSIEvent)
      _ <- state.addEvent(updateSSIEvent)
      retrieved <- state.getEventByHash(eventHash)
      _ = retrieved match
        case None =>
          fail(s"Event ${eventHash.hex} not found after insertion")
        case Some(mySignedPrismEvent) =>
          assertEquals(
            obtained = retrieved.get.eventHash.hex,
            expected = eventHash.hex,
            clue = s"SSI event hash mismatch: ${retrieved.get.eventHash.hex} != ${eventHash.hex}"
          )
      didSubject = VDRUtilsTestExtra.didPrism.asDIDSubject
      eventRefs1 <- state.getEventsIdBySSI(didSubject) // Test getEventsIdBySSI
      eventRefs2 <- state.getEventsForSSI(didSubject) // Test getEventsForSSI
      ssi <- state.getSSI(didSubject) // Test getSSI
      _ = assertEquals(eventRefs1.size, 2)
      _ = assertEquals(eventRefs2.size, 2)
      _ = assertEquals(ssi.did, didSubject)
      _ = assertEquals(ssi.keys.size, 3)
    } yield ()
  }

  prismStateFixture.testZLayered(
    "getEventsIdBySSI & getEventsForSSI: empty for non-existent SSI"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      // Query for a non-existent SSI
      fakeDID = DIDPrism("0000000000000000000000000000000000000000000000000000000000000000").asDIDSubject
      eventRefs1 <- state.getEventsIdBySSI(fakeDID) // Test getEventsIdBySSI
      eventRefs2 <- state.getEventsForSSI(fakeDID) // Test getEventsForSSI
      _ = assertEquals(eventRefs1.size, 0) // Should return empty sequence
      _ = assertEquals(eventRefs2.size, 0) // Should return empty sequence
    } yield ()
  }

  prismStateFixture.testZLayered(
    "getEventsIdByVDR & getEventsForVDR: empty for non-existent VDR"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      // Query for a non-existent VDR
      fakeVDR = RefVDR("0000000000000000000000000000000000000000000000000000000000000000")
      eventRefs1 <- state.getEventsIdByVDR(fakeVDR) // Test getEventsIdByVDR
      eventRefs2 <- state.getEventsForVDR(fakeVDR) // Test getEventsForVDR
      _ = assertEquals(eventRefs1.size, 0) // Should return empty sequence
      _ = assertEquals(eventRefs2.size, 0) // Should return empty sequence
    } yield ()
  }

  prismStateFixture.testZLayered(
    "addEvent CreateStorageEntryOP & findEventByHash"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      createVDREvent = aux(1)
      eventHash = createVDREvent.eventHash

      _ <- state.addEvent(createVDREvent)
      retrieved <- state.getEventByHash(eventHash)

      _ = retrieved match
        case None =>
          fail(s"Event ${eventHash.hex} not found after insertion")
        case Some(signedPrismEvent) =>
          assertEquals(
            obtained = signedPrismEvent.eventHash.hex,
            expected = eventHash.hex,
            clue = s"VDR event hash mismatch: ${retrieved.get.eventHash.hex} != ${eventHash.hex}"
          )
    } yield ()
  }

  prismStateFixture.testZLayered(
    "chain of events: Create + Update"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      createDidEvent = aux(0)
      updateDidEvent = aux(4)

      _ <- state.addEvent(createDidEvent) // Insert create first
      _ <- state.addEvent(updateDidEvent) // Then insert update (should link to create via previousEventHash)

      // Verify both events are retrievable
      createRetrieved <- state.getEventByHash(createDidEvent.eventHash)
      updateRetrieved <- state.getEventByHash(updateDidEvent.eventHash)

      _ = createRetrieved match
        case None => fail(s"Event createDidEvent ${createDidEvent.eventHash.hex} not found after insertion")
        case Some(signedPrismEvent) => // ok
      _ = updateRetrieved match
        case None => fail(s"Event updateDidEvent ${updateDidEvent.eventHash.hex} not found after insertion")
        case Some(signedPrismEvent) => // ok

      // PrismStateMongoDB specific
      updateRetrievedWithRootRef <- state.asInstanceOf[PrismStateMongoDB].findEventByHash(updateDidEvent.eventHash)
      _ = updateRetrievedWithRootRef match
        case None => fail(s"Event updateDidEvent ${updateDidEvent.eventHash.hex} not found after insertion")
        case Some(eventWithRootRef) =>
          given CanEqual[EventHash, EventHash] = ??? // CanEqual.derived
          eventWithRootRef.rootRef == createDidEvent.eventHash
          assertEquals(
            obtained = eventWithRootRef.rootRef.hex,
            expected = createDidEvent.eventHash.hex,
            clue = "The rootRef of the in updateDidEvent the MongoDB must be the same as the one is createDidEvent"
          )
    } yield ()
  }

  prismStateFixture.testZLayered(
    "chain of events (wrong order): Update + Create"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      createDidEvent = aux(0)
      updateDidEvent = aux(4)

      _ <- state.addEvent(updateDidEvent) // Insert update (SHOULD link to create via previousEventHash) so it fails
      _ <- state.addEvent(createDidEvent) // Then insert create first

      // Verify both events are retrievable
      createRetrieved <- state.getEventByHash(createDidEvent.eventHash)
      updateRetrieved <- state.getEventByHash(updateDidEvent.eventHash)

      _ = createRetrieved match
        case None => fail(s"Event createDidEvent ${createDidEvent.eventHash.hex} not found after insertion")
        case Some(signedPrismEvent) => // ok
      _ = updateRetrieved match
        case None                   => // ok
        case Some(signedPrismEvent) =>
          fail(s"Event updateDidEvent ${updateDidEvent.eventHash.hex} SHOULD not be inserted")

      // PrismStateMongoDB specific
      updateRetrievedWithRootRef <- state.asInstanceOf[PrismStateMongoDB].findLostEventByHash(updateDidEvent.eventHash)
      _ = updateRetrievedWithRootRef match
        case None => fail(s"Event updateDidEvent ${updateDidEvent.eventHash.hex} not found in the lostCollection")
        case Some(lostEvent) => // ok
    } yield ()
  }

  prismStateFixture.testZLayered(
    "chain of VDR events: Create + 2 Updates"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      // createDID = aux(0)
      createVDR = aux(1)
      updateVDR1 = aux(2)
      updateVDR2 = aux(3)

      // _ <- state.addEvent(createDID)
      _ <- state.addEvent(createVDR)
      _ <- state.addEvent(updateVDR1)
      _ <- state.addEvent(updateVDR2)

      // Verify all are retrievable
      retrieved1 <- state.getEventByHash(createVDR.eventHash)
      retrieved2 <- state.getEventByHash(updateVDR1.eventHash)
      retrieved3 <- state.getEventByHash(updateVDR2.eventHash)

      _ = assert(retrieved1.isDefined || retrieved2.isDefined || retrieved3.isDefined, "Not all VDR events found")

      seq <- state.getEventsForVDR(RefVDR.fromEventHash(createVDR.eventHash))
      _ = {
        assertEquals(seq.size, 3)
        assertEquals(seq(0).eventHash.hex, createVDR.eventHash.hex)
        assertEquals(seq(1).eventHash.hex, updateVDR1.eventHash.hex)
        assertEquals(seq(2).eventHash.hex, updateVDR2.eventHash.hex)
      }
      vdr <- state.getVDR(RefVDR.fromEventHash(createVDR.eventHash))
      _ = vdr.data match
        case VDR.DataEmpty()              => // ok
        case VDR.DataByteArray(byteArray) => fail("The data is not valid since the DID doesn't exist")
        case _                            => fail("Wrong Data type")
    } yield {}
  }

  prismStateFixture.testZLayered(
    "chain of events: CreateSSI + CreateVDR + UpdatesVDR + UpdatesVDR (with new future key)"
      .tag(fmgp.IntregrationTest)
      .tag(fmgp.DBTest)
  ) {
    for {
      state <- ZIO.service[PrismState]
      createDID = aux(0) // createSSI
      createVDR = aux(1) // createVDR
      updateVDR1 = aux(2) // updateVDR
      updateVDR2 = aux(3) // updateVDR_withTheNewKey

      _ <- state.addEvent(createDID)
      _ <- state.addEvent(createVDR)
      _ <- state.addEvent(updateVDR1)
      _ <- state.addEvent(updateVDR2)

      // Verify all are retrievable
      retrieved1 <- state.getEventByHash(createVDR.eventHash)
      retrieved2 <- state.getEventByHash(updateVDR1.eventHash)
      retrieved3 <- state.getEventByHash(updateVDR2.eventHash)

      _ = assert(retrieved1.isDefined || retrieved2.isDefined || retrieved3.isDefined, "Not all VDR events found")

      seq <- state.getEventsForVDR(RefVDR.fromEventHash(createVDR.eventHash))
      _ = {
        assertEquals(seq.size, 3)
        assertEquals(seq(0).eventHash.hex, createVDR.eventHash.hex)
        assertEquals(seq(1).eventHash.hex, updateVDR1.eventHash.hex)
        assertEquals(seq(2).eventHash.hex, updateVDR2.eventHash.hex)
      }
      vdr <- state.getVDR(RefVDR.fromEventHash(createVDR.eventHash))
      _ = vdr.data match
        case VDR.DataEmpty()              => fail("Data SHOULD be valid and not DataEmpty")
        case VDR.DataByteArray(byteArray) => // ok
          assertNotEquals(
            bytes2Hex(byteArray),
            bytes2Hex(VDRUtilsTestExtra.data3)
          ) // the last event should not take effect
          assertEquals(bytes2Hex(byteArray), bytes2Hex(VDRUtilsTestExtra.data2))
        case _ => fail("Wrong Data type")
    } yield {}
  }

}
