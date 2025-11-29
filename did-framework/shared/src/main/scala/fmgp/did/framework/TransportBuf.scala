package fmgp.did.framework

import zio.*
import zio.stream.*

trait TransportWS[R, M] extends Transport[R, M, M] {
  def ws: Websocket[Throwable]
  override def id = ws.socketID
}
