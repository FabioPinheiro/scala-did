package fmgp.util

import zio._
import zio.stream._

trait TransportWS[R, M] extends Transport[R, M] {
  def outboundBuf: Queue[M]
  def inboundBuf: Hub[M]
  def ws: Websocket[Throwable]

  override def id = ws.socketID
  override def outbound: ZSink[Any, Transport.OutErr, M, Nothing, Unit] = ZSink.fromQueue(outboundBuf)
  override def inbound: ZStream[Any, Transport.InErr, M] = ZStream.fromHub(inboundBuf)
}
