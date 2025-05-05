package fmgp.prism

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.method.prism._

case class PrismStateHTTP(
    httpUtils: HttpUtils,
    pathEventsByDID: String = "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/ops",
) extends PrismStateRead {

  override def getEventsForSSI(ssi: DIDSubject): ZIO[Any, Throwable, Seq[MySignedPrismOperation[OP]]] = {
    val destination = s"$pathEventsByDID/$ssi"
    for {
      proxy <- ZIO.service[HttpUtils]
      ret <- proxy.getSeqT[MySignedPrismOperation[OP]](destination)
    } yield ret
  }.provideEnvironment(ZEnvironment(httpUtils))

  override def getEventsByHash(refHash: String): Option[MySignedPrismOperation[OP]] = ???
  override def getEventsIdBySSI(ssi: DIDSubject): Seq[EventRef] = ???
  override def ssi2eventsId: Map[DIDSubject, Seq[fmgp.prism.EventRef]] = ???

}
