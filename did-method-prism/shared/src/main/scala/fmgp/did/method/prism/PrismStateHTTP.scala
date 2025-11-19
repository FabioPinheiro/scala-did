package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.method.prism._
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto._

case class PrismStateHTTP(
    httpUtils: HttpUtils,
    pathEventsByDID: String =
      "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/events",
) extends PrismStateRead {

  def cursor: ZIO[Any, Nothing, cardano.EventCursor] = ???

  def ssi2eventsRef: ZIO[Any, Nothing, Map[DIDSubject, Seq[EventRef]]] = ???
  def vdr2eventsRef: ZIO[Any, Nothing, Map[RefVDR, Seq[EventRef]]] = ???

  override def getEventsIdBySSI(ssi: DIDSubject): ZIO[Any, Nothing, Seq[EventRef]] = ???
  override def getEventsIdByVDR(id: RefVDR): ZIO[Any, Nothing, Seq[EventRef]] = ???

  override def getEventsForSSI(
      ssi: DIDSubject
  ): ZIO[Any, Throwable, Seq[MySignedPrismEvent[CreateDidOP | UpdateDidOP | DeactivateDidOP]]] = {
    val destination = s"$pathEventsByDID/${ssi.specificId}"
    for {
      proxy <- ZIO.service[HttpUtils]
      ret <- proxy.getSeqT[MySignedPrismEvent[OP]](destination)
      retTyped <- PrismState.forceType2DidEvent(ret)
    } yield retTyped
  }.provideEnvironment(ZEnvironment(httpUtils))

  override def getEventsForVDR(refVDR: RefVDR): ZIO[Any, Throwable, Seq[
    MySignedPrismEvent[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP]
  ]] = ???

  override def getEventByHash(refHash: EventHash): ZIO[Any, Nothing, Option[MySignedPrismEvent[OP]]] = ???

}
