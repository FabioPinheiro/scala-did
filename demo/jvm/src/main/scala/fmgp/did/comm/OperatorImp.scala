package fmgp.did.comm

import zio._

import fmgp.did._
import fmgp.util._

object OperatorImp {
  val layer = ZLayer.succeed(
    Operator(
      selfOperator = AgentExecutar(AgentProvider.local),
      contacts = Seq(
        AgentExecutar(AgentProvider.alice),
        AgentExecutar(AgentProvider.bob),
        AgentExecutar(AgentProvider.charlie),
      )
    )
  )
}
