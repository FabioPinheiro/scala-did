package fmgp.prism

import zio._
import zio.json._
import fmgp.did.method.prism._
import fmgp.did.DIDSubject

case class PrismStateFS(config: IndexerConfig) extends PrismStateRead {

  override def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[MySignedPrismOperation[OP]]] = ???
  override def getEventsByHash(refHash: String): Option[MySignedPrismOperation[OP]] = ???
  override def getEventsIdBySSI(ssi: DIDSubject): Seq[EventRef] = ???

  override def ssi2eventsId: Map[DIDSubject, Seq[EventRef]] = ???

}
