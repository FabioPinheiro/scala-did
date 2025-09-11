package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did.method.prism._
import fmgp.did.DIDSubject
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto._

case class PrismStateFS() extends PrismStateRead {

  override def getEventsIdBySSI(ssi: DIDSubject): Seq[EventRef] = ???
  override def getEventsIdByVDR(ref: RefVDR): Seq[EventRef] = ???

  override def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[
    MySignedPrismEvent[CreateDidOP | UpdateDidOP | DeactivateDidOP]
  ]] = ???

  override def getEventsForVDR(refVDR: RefVDR): ZIO[Any, Throwable, Seq[
    MySignedPrismEvent[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP]
  ]] = ???

  override def getEventsByHash(refHash: EventHash): Option[MySignedPrismEvent[OP]] = ???

  override def ssi2eventsId: Map[DIDSubject, Seq[EventRef]] = ???

}
