package fmgp.util

import zio._
import zio.stream._

trait TransportWS[T, M] extends Transport[T, M] {
  def outboundBuf: Queue[M]
  def inboundBuf: Hub[M]
  def ws: Websocket[Throwable]

  override def id = ws.socketID
  override def outbound: ZSink[Any, OutErr, M, Nothing, Unit] = ZSink.fromQueue(outboundBuf)
  override def inbound: ZStream[Any, InErr, M] = ZStream.fromHub(inboundBuf)
}
