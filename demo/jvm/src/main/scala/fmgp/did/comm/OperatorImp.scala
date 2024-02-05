package fmgp.did.comm

import zio._
import zio.http._

import fmgp.crypto.error.DidFail
import fmgp.did._
import fmgp.did.comm.protocol.ProtocolExecuter
import fmgp.did.framework._

object OperatorImp {
  val layer: ZLayer[Client & Scope, Nothing, Operator] =
    (TransportFactoryImp.layer ++ AgentExecutarImp.basicProtocolHandlerLayer) >>>
      ZLayer.fromZIO(
        for {
          protocolHandler <- ZIO.service[ProtocolExecuter[Resolver & Agent & Operations, DidFail]]
          local <- AgentExecutarImp.make(AgentProvider.local, protocolHandler)
          alice <- AgentExecutarImp.make(AgentProvider.alice, protocolHandler)
          // aliceWs <- AgentExecutarImp.make(AgentProvider.aliceWs, protocolHandler)
          // aliceHttpWs <- AgentExecutarImp.make(AgentProvider.aliceHttpWs, protocolHandler)
          bob <- AgentExecutarImp.make(AgentProvider.bob, protocolHandler)
          charlie <- AgentExecutarImp.make(AgentProvider.charlie, protocolHandler)
          alice8080 <- AgentExecutarImp.make(AgentProvider.localhost8080Alice, protocolHandler)
          alice8080Ws <- AgentExecutarImp.make(AgentProvider.localhost8080AliceWs, protocolHandler)
          alice8080HttpWs <- AgentExecutarImp.make(AgentProvider.localhost8080AliceHttpWs, protocolHandler)
          alice9000 <- AgentExecutarImp.make(AgentProvider.localhost9000Alice, protocolHandler)
          oldAlice8080 <- AgentExecutarImp.make(AgentProvider.oldLocalhost8080Alice, protocolHandler)
          oldAlice8080Ws <- AgentExecutarImp.make(AgentProvider.oldLocalhost8080AliceWs, protocolHandler)
          oldAlice8080HttpWs <- AgentExecutarImp.make(AgentProvider.oldLocalhost8080AliceHttpWs, protocolHandler)
          oldAlice9000 <- AgentExecutarImp.make(AgentProvider.oldLocalhost9000Alice, protocolHandler)
          operator = Operator(
            selfOperator = local,
            contacts = Seq(
              alice,
              bob,
              charlie,
              alice8080,
              alice8080Ws,
              alice8080HttpWs,
              alice9000,
              oldAlice8080,
              oldAlice8080Ws,
              oldAlice8080HttpWs,
              oldAlice9000,
            )
          )
        } yield operator
      )
}
