package fmgp.did.method.prism

import zio.*
import zio.json.*
import fmgp.did.DIDSubject
import fmgp.did.method.prism.*
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto.*

case class PrismStateFS() extends PrismStateRead {

  def cursor: ZIO[Any, Nothing, cardano.EventCursor] = ???

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

  override def getEventByHash(refHash: EventHash): ZIO[Any, Nothing, Option[MySignedPrismEvent[OP]]] = ???

}
