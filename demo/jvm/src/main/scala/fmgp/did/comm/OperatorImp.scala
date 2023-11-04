package fmgp.did.comm

import zio._

import fmgp.did._
import fmgp.util._

object OperatorImp {
  val layer = ZLayer.fromZIO(
    for {
      local <- AgentExecutarImp.make(AgentProvider.local)
      alice <- AgentExecutarImp.make(AgentProvider.alice)
      bob <- AgentExecutarImp.make(AgentProvider.bob)
      charlie <- AgentExecutarImp.make(AgentProvider.charlie)
      alice8080 <- AgentExecutarImp.make(AgentProvider.localhost8080Alice)
      alice9000 <- AgentExecutarImp.make(AgentProvider.localhost9000Alice)
      operator = Operator(
        selfOperator = local,
        contacts = Seq(alice, bob, charlie, alice8080, alice9000)
      )
    } yield operator
  )
}
