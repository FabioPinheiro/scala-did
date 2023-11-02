package fmgp.util

import zio._
import zio.stream._
import fmgp.did.comm._

type TransportDIDComm[R, M] = Transport[R, Message]

/** The goal is to make this DID Comm library Transport-agnostic */
trait Transport[R, M] {

  /** Send to the other side. Out going Messages */
  def outbound: ZSink[R, Transport.OutErr, M, Nothing, Unit]

  /** Reciving from the other side. Income Messages */
  def inbound: ZStream[R, Transport.InErr, M]

  def id: String

  def send(message: M): ZIO[R, Nothing, Unit] =
    ZStream.succeed(message).run(outbound)
  // def recive[R, E](process: (MSG) => ZIO[R, E, Unit]) =
  //   inbound.runForeach(process(_))
}

object Transport {
  type OutErr = Nothing
  type InErr = Nothing

  val TRANSPORT_ID = "TRANSPORT_ID"
  private var transportCounter = 1
  // TODO use scala.util.Random.nextLong().toString
  def nextTransportCounter = "transport:" + this.synchronized { transportCounter += 1; transportCounter }
  def logAnnotation(transportID: String = nextTransportCounter) = LogAnnotation(TRANSPORT_ID, transportID)
}
