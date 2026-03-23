package fmgp.did.method.prism

import munit.*
import zio.*

import fmgp.did.method.prism.cardano.{*, given}
import fmgp.did.method.prism.proto.*

/** Test for issue #577: MaybeEvent.fromProtoForce silently drops all events when used with PrismStateInMemory.addEvent
  *
  * The root cause: fromProtoForce defaults to EventCursor.fake (-1,-1) == EventCursor.init. SSI.append silently ignores
  * any event whose cursor <= the current cursor (which starts at EventCursor.init).
  *
  * Run with: didResolverPrismJVM/testOnly fmgp.did.method.prism.Issue577Suite
  *
  * TODO move to the Sshared folder
  */
class Issue577Suite extends ZSuite {
  import fmgp.did.method.prism.vdr.KeyConstanceUtils.*

  def prismStateFixture: FunFixture[ULayer[PrismState]] =
    ZTestLocalFixture { _ => PrismStateInMemory.empty.map(s => ZLayer.succeed(s)) }(_ => ZIO.unit)

  prismStateFixture.testZLayered(
    "createDeterministicDID + fromProtoForce + addEvent: getSSI should return a non-empty SSI"
  ) {
    for {
      state <- ZIO.service[PrismState]
      (didPrism, createEvent, _) = DIDExtra.createDeterministicDID(pkMaster, Seq.empty)
      signedEvent = MaybeEvent.fromProtoForce2DIDEvent(createEvent, cursor = EventCursor(0, 0))
      _ <- state.addEvent(signedEvent)
      ssi <- state.getSSI(didPrism)
      _ = assert(ssi.exists, "SSI should exist after addEvent — fromProtoForce silently dropped the event (issue #577)")
      _ = assert(ssi.keys.nonEmpty, "SSI should have at least the master key")
    } yield ()
  }

  prismStateFixture.testZLayered(
    "addEvent with fake cursor (EventCursor.fake default) should fail with an exception"
  ) {
    for {
      state <- ZIO.service[PrismState]
      (_, createEvent, _) = DIDExtra.createDeterministicDID(pkMaster, Seq.empty)
      signedEvent = MaybeEvent.fromProtoForce2DIDEvent(createEvent) // uses EventCursor.fake default
      result <- state.addEvent(signedEvent).either
      _ = assert(result.isLeft, "addEvent with a fake cursor should fail")
    } yield ()
  }
}
