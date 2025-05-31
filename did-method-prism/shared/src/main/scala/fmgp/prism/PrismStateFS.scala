package fmgp.prism

import zio._
import zio.json._
import fmgp.did.method.prism._
import fmgp.did.DIDSubject

case class PrismStateFS(config: IndexerConfig) extends PrismStateRead {

  override def getEventsIdBySSI(ssi: DIDSubject): Seq[EventRef] = ???
  override def getEventsIdByVDR(ref: RefVDR): Seq[EventRef] = ???

  override def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[
    MySignedPrismOperation[CreateDidOP | UpdateDidOP | DeactivateDidOP]
  ]] = ???

  override def getEventsForVDR(refVDR: RefVDR): ZIO[Any, Throwable, Seq[
    MySignedPrismOperation[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP]
  ]] = ???

  override def getEventsByHash(refHash: String): Option[MySignedPrismOperation[OP]] = ???

  override def ssi2eventsId: Map[DIDSubject, Seq[EventRef]] = ???

}
