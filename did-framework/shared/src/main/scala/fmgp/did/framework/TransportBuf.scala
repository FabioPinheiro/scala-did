package fmgp.did.framework

import zio._
import zio.stream._

trait TransportWS[R, M] extends Transport[R, M, M] {
  def ws: Websocket[Throwable]
  override def id = ws.socketID
}
