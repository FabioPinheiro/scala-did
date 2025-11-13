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

  prismStateFixture.testZLayered("addEvent: insert CreateDidOP event".tag(fmgp.IntregrationTest)) {
    for {
      state <- ZIO.service[PrismState]
      createDidEvent = aux(0) // CreateDidOP
      _ <- state.addEvent(createDidEvent) // no exceptions thrown
    } yield ()
  }

  prismStateFixture.testZLayered("addEvent CreateDidOP & findEventByHash".tag(fmgp.IntregrationTest)) {
    for {
      state <- ZIO.service[PrismState]
      createDidEvent = aux(0)
      eventHash = createDidEvent.eventHash

      _ <- state.addEvent(createDidEvent)
      retrieved <- state.getEventsByHash(eventHash)

      _ = retrieved match
        case None =>
          fail(s"Event ${eventHash.hex} not found after insertion")
        case Some(mySignedPrismEvent) =>
          assertEquals(
            obtained = retrieved.get.eventHash.hex,
            expected = eventHash.hex,
            clue = s"SSI event hash mismatch: ${retrieved.get.eventHash.hex} != ${eventHash.hex}"
          )
    } yield ()
  }

  prismStateFixture.testZLayered("addEvent CreateStorageEntryOP & findEventByHash".tag(fmgp.IntregrationTest)) {
    for {
      state <- ZIO.service[PrismState]
      createVDREvent = aux(1)
      eventHash = createVDREvent.eventHash

      _ <- state.addEvent(createVDREvent)
      retrieved <- state.getEventsByHash(eventHash)

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

  prismStateFixture.testZLayered("chain of events: Create + Update".tag(fmgp.IntregrationTest)) {
    for {
      state <- ZIO.service[PrismState]
      createDidEvent = aux(0)
      updateDidEvent = aux(4)

      _ <- state.addEvent(createDidEvent) // Insert create first
      _ <- state.addEvent(updateDidEvent) // Then insert update (should link to create via previousEventHash)

      // Verify both events are retrievable
      createRetrieved <- state.getEventsByHash(createDidEvent.eventHash)
      updateRetrieved <- state.getEventsByHash(updateDidEvent.eventHash)

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

  prismStateFixture.testZLayered("chain of events (wrong order): Update + Create".tag(fmgp.IntregrationTest)) {
    for {
      state <- ZIO.service[PrismState]
      createDidEvent = aux(0)
      updateDidEvent = aux(4)

      _ <- state.addEvent(updateDidEvent) // Insert update (SHOULD link to create via previousEventHash) so it fails
      _ <- state.addEvent(createDidEvent) // Then insert create first

      // Verify both events are retrievable
      createRetrieved <- state.getEventsByHash(createDidEvent.eventHash)
      updateRetrieved <- state.getEventsByHash(updateDidEvent.eventHash)

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

  prismStateFixture.testZLayered("chain of VDR events: Create + 2 Updates".tag(fmgp.IntregrationTest)) {
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
      retrieved1 <- state.getEventsByHash(createVDR.eventHash)
      retrieved2 <- state.getEventsByHash(updateVDR1.eventHash)
      retrieved3 <- state.getEventsByHash(updateVDR2.eventHash)

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
    "chain of events: CreateSSI + CreateVDR + UpdatesVDR + UpdatesVDR (with new future key)".tag(fmgp.IntregrationTest)
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
      retrieved1 <- state.getEventsByHash(createVDR.eventHash)
      retrieved2 <- state.getEventsByHash(updateVDR1.eventHash)
      retrieved3 <- state.getEventsByHash(updateVDR2.eventHash)

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

  // prismStateFixture.testZLayered("addEvent: all 5 events") {
  //   for {
  //     state <- ZIO.service[PrismState]
  //     _ <- ZIO.foreach(aux)(state.addEvent)

  //     // Verify all are retrievable
  //     allRetrieved <- ZIO.foreach(aux)(event => state.getEventsByHash(event.eventHash))

  //     _ <- ZIO.when(allRetrieved.exists(_.isEmpty))(
  //       ZIO.fail(new AssertionError("Some events not found after bulk insertion"))
  //     )
  //   } yield ()
  // }

  // // Tests for getEventsIdBySSI
  // prismStateFixture.testZLayered("getEventsIdBySSI: debug event hash") {
  //   for {
  //     state <- ZIO.service[PrismState]
  //     // Clean database before test
  //     _ <- state match {
  //       case mongoState: PrismStateMongoDB =>
  //         (clearCollection(mongoState.collection) *> clearCollection(mongoState.lostCollection))
  //           .catchAll(_ => ZIO.unit)
  //       case _ => ZIO.unit
  //     }

  //     createDidEvent = aux(0)
  //     _ <- ZIO.logInfo(s"CreateDID event hash: ${createDidEvent.eventHash.hex}")

  //     didSubject = VDRUtilsTestExtra.didPrism.asDIDSubject
  //     expectedHash = EventHash.fromPRISM(VDRUtilsTestExtra.didPrism)
  //     _ <- ZIO.logInfo(s"Expected hash from DID: ${expectedHash.hex}")
  //     _ <- ZIO.logInfo(s"Hashes match: ${createDidEvent.eventHash.hex == expectedHash.hex}")

  //     // Insert create event
  //     _ <- state.addEvent(createDidEvent)

  //     // Try to retrieve directly by hash
  //     retrieved <- state.getEventsByHash(createDidEvent.eventHash)
  //     _ <- ZIO.logInfo(s"Retrieved by direct hash: ${retrieved.isDefined}")

  //     // Now try getEventsIdBySSI
  //     eventRefs <- state.getEventsIdBySSI(didSubject)
  //     _ <- ZIO.logInfo(s"getEventsIdBySSI returned ${eventRefs.length} events")
  //   } yield ()
  // }

  // prismStateFixture.testZLayered("getEventsIdBySSI: retrieve event refs for DID") {
  //   for {
  //     state <- ZIO.service[PrismState]
  //     // Clean database before test
  //     _ <- state match {
  //       case mongoState: PrismStateMongoDB =>
  //         (clearCollection(mongoState.collection) *> clearCollection(mongoState.lostCollection))
  //           .catchAll(_ => ZIO.unit)
  //       case _ => ZIO.unit
  //     }

  //     createDidEvent = aux(0)
  //     updateDidEvent = aux(4)

  //     // Insert create and update DID events
  //     _ <- state.addEvent(createDidEvent)
  //     _ <- state.addEvent(updateDidEvent)

  //     // Get EventRefs for the DID
  //     didSubject = VDRUtilsTestExtra.didPrism.asDIDSubject
  //     eventRefs <- state.getEventsIdBySSI(didSubject)

  //     // Should return 2 events sorted by (b, o)
  //     _ <- ZIO.when(eventRefs.length != 2)(
  //       ZIO.fail(new AssertionError(s"Expected 2 event refs, got ${eventRefs.length}"))
  //     )

  //     // Verify they're sorted by block and operation index
  //     _ <- ZIO.when(
  //       eventRefs(0).b > eventRefs(1).b || (eventRefs(0).b == eventRefs(1).b && eventRefs(0).o > eventRefs(1).o)
  //     )(
  //       ZIO.fail(new AssertionError("Event refs not sorted by (b, o)"))
  //     )

  //     // Verify the event hashes match
  //     _ <- ZIO.when(eventRefs(0).eventHash.hex != createDidEvent.eventHash.hex)(
  //       ZIO.fail(new AssertionError("First event hash doesn't match create event"))
  //     )
  //     _ <- ZIO.when(eventRefs(1).eventHash.hex != updateDidEvent.eventHash.hex)(
  //       ZIO.fail(new AssertionError("Second event hash doesn't match update event"))
  //     )
  //   } yield ()
  // }

  // prismStateFixture.testZLayered("getEventsIdBySSI: empty for non-existent DID") {
  //   for {
  //     state <- ZIO.service[PrismState]

  //     // Query for a non-existent DID
  //     fakeDID = DIDPrism("0000000000000000000000000000000000000000000000000000000000000000").asDIDSubject
  //     eventRefs <- state.getEventsIdBySSI(fakeDID)

  //     // Should return empty sequence
  //     _ <- ZIO.when(eventRefs.nonEmpty)(
  //       ZIO.fail(new AssertionError(s"Expected empty sequence, got ${eventRefs.length} events"))
  //     )
  //   } yield ()
  // }

  // // Tests for getEventsIdByVDR
  // prismStateFixture.testZLayered("getEventsIdByVDR: retrieve event refs for VDR") {
  //   for {
  //     state <- ZIO.service[PrismState]
  //     // Clean database before test
  //     _ <- state match {
  //       case mongoState: PrismStateMongoDB =>
  //         (clearCollection(mongoState.collection) *> clearCollection(mongoState.lostCollection))
  //           .catchAll(_ => ZIO.unit)
  //       case _ => ZIO.unit
  //     }

  //     createVDR = aux(1)
  //     updateVDR1 = aux(2)
  //     updateVDR2 = aux(3)

  //     // Insert VDR events
  //     _ <- state.addEvent(createVDR)
  //     _ <- state.addEvent(updateVDR1)
  //     _ <- state.addEvent(updateVDR2)

  //     // Get EventRefs for the VDR
  //     eventRefs <- state.getEventsIdByVDR(VDRUtilsTestExtra.refVDR)

  //     // Should return 3 events sorted by (b, o)
  //     _ <- ZIO.when(eventRefs.length != 3)(
  //       ZIO.fail(new AssertionError(s"Expected 3 event refs, got ${eventRefs.length}"))
  //     )

  //     // Verify they're sorted
  //     _ <- ZIO.when(
  //       eventRefs(0).b > eventRefs(1).b ||
  //         eventRefs(1).b > eventRefs(2).b ||
  //         (eventRefs(0).b == eventRefs(1).b && eventRefs(0).o > eventRefs(1).o)
  //     )(
  //       ZIO.fail(new AssertionError("Event refs not sorted by (b, o)"))
  //     )

  //     // Verify the event hashes match
  //     _ <- ZIO.when(eventRefs(0).eventHash.hex != createVDR.eventHash.hex)(
  //       ZIO.fail(new AssertionError("First event hash doesn't match create event"))
  //     )
  //   } yield ()
  // }

  // prismStateFixture.testZLayered("getEventsIdByVDR: empty for non-existent VDR") {
  //   for {
  //     state <- ZIO.service[PrismState]

  //     // Query for a non-existent VDR
  //     fakeVDR = RefVDR("0000000000000000000000000000000000000000000000000000000000000000")
  //     eventRefs <- state.getEventsIdByVDR(fakeVDR)

  //     // Should return empty sequence
  //     _ <- ZIO.when(eventRefs.nonEmpty)(
  //       ZIO.fail(new AssertionError(s"Expected empty sequence, got ${eventRefs.length} events"))
  //     )
  //   } yield ()
  // }

  // // Tests for getEventsForVDR
  // prismStateFixture.testZLayered("getEventsForVDR: retrieve storage events for VDR") {
  //   for {
  //     state <- ZIO.service[PrismState]
  //     // Clean database before test
  //     _ <- state match {
  //       case mongoState: PrismStateMongoDB =>
  //         (clearCollection(mongoState.collection) *> clearCollection(mongoState.lostCollection))
  //           .catchAll(_ => ZIO.unit)
  //       case _ => ZIO.unit
  //     }

  //     createVDR = aux(1)
  //     updateVDR1 = aux(2)
  //     updateVDR2 = aux(3)

  //     // Insert VDR events
  //     _ <- state.addEvent(createVDR)
  //     _ <- state.addEvent(updateVDR1)
  //     _ <- state.addEvent(updateVDR2)

  //     // Get full events for the VDR
  //     events <- state.getEventsForVDR(VDRUtilsTestExtra.refVDR)

  //     // Should return 3 storage events
  //     _ <- ZIO.when(events.length != 3)(
  //       ZIO.fail(new AssertionError(s"Expected 3 storage events, got ${events.length}"))
  //     )

  //     // Verify first event is CreateStorageEntryOP
  //     _ <- events.headOption match {
  //       case Some(event) =>
  //         event.event match {
  //           case _: CreateStorageEntryOP => ZIO.unit
  //           case _                       => ZIO.fail(new AssertionError("First event is not CreateStorageEntryOP"))
  //         }
  //       case None => ZIO.fail(new AssertionError("No events returned"))
  //     }

  //     // Verify remaining events are UpdateStorageEntryOP
  //     _ <- ZIO.foreach(events.tail) { event =>
  //       event.event match {
  //         case _: UpdateStorageEntryOP => ZIO.unit
  //         case _                       => ZIO.fail(new AssertionError("Event is not UpdateStorageEntryOP"))
  //       }
  //     }
  //   } yield ()
  // }

  // prismStateFixture.testZLayered("getEventsForVDR: fails for non-existent VDR") {
  //   for {
  //     state <- ZIO.service[PrismState]

  //     // Query for a non-existent VDR - should return empty sequence
  //     fakeVDR = RefVDR("0000000000000000000000000000000000000000000000000000000000000000")
  //     events <- state.getEventsForVDR(fakeVDR)

  //     _ <- ZIO.when(events.nonEmpty)(
  //       ZIO.fail(new AssertionError(s"Expected empty sequence, got ${events.length} events"))
  //     )
  //   } yield ()
  // }

}
