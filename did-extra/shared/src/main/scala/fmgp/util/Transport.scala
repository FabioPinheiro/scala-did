package fmgp.util

import zio._
import zio.stream._
import fmgp.did.comm._

type TransportDIDComm[R, M] = Transport[R, Message]

/** The goal is to make this DID Comm library Transport-agnostic */
trait Transport[R, M] {
  type OutErr = Nothing
  type InErr = Nothing

  def outbound: ZSink[R, OutErr, M, Nothing, Unit]
  def inbound: ZStream[R, InErr, M]
  def id: String

  def send(message: M): ZIO[R, Nothing, Unit] =
    ZStream.succeed(message).run(outbound)
  // def recive[R, E](process: (MSG) => ZIO[R, E, Unit]) =
  //   inbound.runForeach(process(_))
}
