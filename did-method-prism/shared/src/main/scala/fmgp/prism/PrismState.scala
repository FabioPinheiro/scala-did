package fmgp.prism

import zio._
import zio.json._

trait PrismStateRead {
  def lastSyncedBlockEpochSecondNano: (Long, Int) = {
    val now = java.time.Instant.now // FIXME
    (now.getEpochSecond, now.getNano)
  }

  // TODO REMOVE
  def ssi2eventsId: Map[String, Seq[EventRef]]
  // TODO REMOVE
  def ssiCount: Int = ssi2eventsId.size

  def getEventsIdBySSI(ssi: String): Seq[EventRef]

  def getEventsByHash(refHash: String): Option[MySignedPrismOperation[OP]]

  def getEventsForSSI(ssi: String): Seq[MySignedPrismOperation[OP]] =
    getEventsIdBySSI(ssi).map { opId =>
      getEventsByHash(opId.opHash) match
        case None        => throw new RuntimeException("impossible state: missing Event/Operation Hash") // TODO
        case Some(value) => value
    }
}

object PrismState {
  def empty: PrismState = PrismStateInMemory.empty
}

trait PrismState extends PrismStateRead {
  def addEvent(op: MySignedPrismOperation[OP]): PrismState
}
