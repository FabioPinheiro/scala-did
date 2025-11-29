package fmgp.did.framework

import zio.*
import zio.json.*
import zio.http.*
import fmgp.did.*
import fmgp.crypto.error.DidFail
import fmgp.did.comm.protocol.routing2.ForwardMessage.makeForwardMessage

object TransportFactoryImp {

  def make(client: Client, scope: Scope): TransportFactory =
    new TransportFactory {
      override def openTransport(uri: String): UIO[TransportDIDComm[Any]] = {
        uri match {
          case p if p.startsWith("https:") => openTransportHTTP(client, scope, p)
          case p if p.startsWith("http:")  =>
            ZIO.logWarning(s"The transport to ${p} may not be secure") *> openTransportHTTP(client, scope, p)
          // case p if p.startsWith("wss:")   => openTransportWS(p).orDie //TODO
          // case p if p.startsWith("ws:") => //TODO
          //   ZIO.logWarning(s"The transport to ${p} may not be secure") *> openTransportWS(p).orDie

        }
      }
    }
  def layer = ZLayer.fromFunction(make)

  // private def openTransportWS(uri: String): UIO[TransportDIDCommWS[Any]] = ??? // FIXME

  private def openTransportHTTP(client: Client, scope: Scope, uri: String): UIO[TransportDIDComm[Any]] =
    TransportDIDCommOverHTTP.makeWithEnvironment(
      destination = uri,
      boundSize = 1,
      ZEnvironment(client, scope)
    )
}
