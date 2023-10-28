package fmgp.util

import zio._
import zio.stream._
import fmgp.did.comm._

type TransportDIDComm[R, MSG] = Transport[R, Message]

/** The goal is to make this DID Comm library Transport-agnostic */
trait Transport[R, MSG] {
  type OutErr = Nothing
  type InErr = Nothing

  def outbound: ZSink[R, OutErr, MSG, Nothing, Unit]
  def inbound: ZStream[R, InErr, MSG]
}
