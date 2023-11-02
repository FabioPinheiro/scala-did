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
      operator = Operator(
        selfOperator = local,
        contacts = Seq(alice, bob, charlie)
      )
    } yield operator
  )
}
