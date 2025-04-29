package fmgp.prism

import zio._
import zio.json._
import fmgp.did.method.prism._

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

  def getEventsForSSI(ssi: String): ZIO[Any, Throwable, Seq[MySignedPrismOperation[OP]]] =
    getEventsIdBySSI(ssi)
      .foldLeft(Right(Seq.empty): Either[String, Seq[MySignedPrismOperation[OP]]])((acc, eventRef) =>
        acc match
          case Left(errors) => Left(errors)
          case Right(seq) =>
            getEventsByHash(eventRef.opHash) match
              case None => Left(s"impossible state: missing Event/Operation Hash '${eventRef.opHash}'")
              case Some(signedPrismOperation) => Right(seq :+ signedPrismOperation)
      ) match {
      case Left(error) => ZIO.fail(new RuntimeException(error))
      case Right(seq)  => ZIO.succeed(seq)
    }

  def getEventsByHash(refHash: String): Option[MySignedPrismOperation[OP]]
}

object PrismState {
  def empty: PrismState = PrismStateInMemory.empty
}

trait PrismState extends PrismStateRead {
  def addEvent(op: MySignedPrismOperation[OP]): PrismState
}
