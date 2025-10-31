package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did.method.prism._
import fmgp.did.DIDSubject
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto._

case class PrismStateFS() extends PrismStateRead {

  def ssi2eventsRef: ZIO[Any, Nothing, Map[DIDSubject, Seq[EventRef]]] = ???
  def vdr2eventsRef: ZIO[Any, Nothing, Map[RefVDR, Seq[EventRef]]] = ???

  def getEventsIdBySSI(ssi: DIDSubject): ZIO[Any, Nothing, Seq[EventRef]] = ???
  def getEventsIdByVDR(id: RefVDR): ZIO[Any, Nothing, Seq[EventRef]] = ???

  override def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[
    MySignedPrismEvent[CreateDidOP | UpdateDidOP | DeactivateDidOP]
  ]] = ???

  override def getEventsForVDR(refVDR: RefVDR): ZIO[Any, Throwable, Seq[
    MySignedPrismEvent[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP]
  ]] = ???

  override def getEventsByHash(refHash: EventHash): ZIO[Any, Nothing, Option[MySignedPrismEvent[OP]]] = ???

}
