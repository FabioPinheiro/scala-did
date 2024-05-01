package fmgp.did.comm

import zio._
import zio.http._

import fmgp.crypto.error.DidFail
import fmgp.did._
import fmgp.did.comm.protocol.ProtocolExecuter
import fmgp.did.framework._

object OperatorImp {
  val layer: ZLayer[Client & Scope, Nothing, Operator] =
    (TransportFactoryImp.layer ++ AgentProgramImp.basicProtocolHandlerLayer) >>>
      ZLayer.fromZIO(
        for {
          protocolHandler <- ZIO.service[ProtocolExecuter[Resolver & Agent & Operations, DidFail]]
          local <- AgentProgramImp.make(AgentProvider.local, protocolHandler)
          alice <- AgentProgramImp.make(AgentProvider.alice, protocolHandler)
          bob <- AgentProgramImp.make(AgentProvider.bob, protocolHandler)
          charlie <- AgentProgramImp.make(AgentProvider.charlie, protocolHandler)
          alice8080 <- AgentProgramImp.make(AgentProvider.localhost8080Alice, protocolHandler)
          alice8080Ws <- AgentProgramImp.make(AgentProvider.localhost8080AliceWs, protocolHandler)
          alice8080HttpWs <- AgentProgramImp.make(AgentProvider.localhost8080AliceHttpWs, protocolHandler)
          alice9000 <- AgentProgramImp.make(AgentProvider.localhost9000Alice, protocolHandler)
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
            )
          )
        } yield operator
      )
}
