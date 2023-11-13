package fmgp.util

import zio._
import zio.stream._
import fmgp.did.comm._

/** The goal is to make this DID Comm library Transport-agnostic */
trait Transport[R, IN, OUT] {

  /** Send to the other side. Out going Messages */
  def outbound: ZSink[R, Transport.OutErr, OUT, Nothing, Unit]

  /** Reciving from the other side. Income Messages */
  def inbound: ZStream[R, Transport.InErr, IN]

  def id: TransportID

  def send(message: OUT): ZIO[R, Nothing, Unit] =
    ZStream.succeed(message).run(outbound)
}

object Transport {
  type OutErr = Nothing
  type InErr = Nothing
}

object TransportID:
  val TRANSPORT_ID = "TRANSPORT_ID"
  private var transportCounter = 1 // TODO use scala.util.Random.nextLong().toString
  private def nextTransportCounter = +this.synchronized { transportCounter += 1; transportCounter }
  def apply(value: String): TransportID = value
  def apply(): TransportID = "transport:" + nextTransportCounter
  def ws: TransportID = "transport:ws:" + nextTransportCounter
  def http: TransportID = "transport:http:" + nextTransportCounter
  def http(requestID: String): TransportID = http + ":" + requestID
  def http(requestID: Option[String]): TransportID = http + requestID.map(":" + _).getOrElse("")
  extension (value: TransportID)
    def id: String = value
    def logAnnotation = LogAnnotation(TRANSPORT_ID, value)
