package fmgp.prism

import zio._
import zio.json._
import fmgp.did.method.prism._

case class PrismStateFS(config: IndexerConfig) extends PrismStateRead {

  override def getEventsForSSI(ssi: String): ZIO[Any, Throwable, Seq[MySignedPrismOperation[OP]]] = ???
  override def getEventsByHash(refHash: String): Option[MySignedPrismOperation[OP]] = ???
  override def getEventsIdBySSI(ssi: String): Seq[EventRef] = ???
  override def ssi2eventsId: Map[String, Seq[EventRef]] = ???

}
