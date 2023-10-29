package fmgp.did.demo

import zio._
import zio.json._
import zio.stream._
import zio.http.Request

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import zio.http.Header

object MyHeaders { // extends HeaderNames {
  final val xForwardedHost: CharSequence = "x-forwarded-host"
}

object AgentByHost {

  def getAgentFor(req: Request) = ZIO.serviceWithZIO[AgentByHost](_.agentFromRequest(req))

  def getAgentFor(host: Host) = ZIO
    .serviceWithZIO[AgentByHost](_.agentFromHost(host))
    .tapError(ex => ZIO.logError(ex.toString()))
    .mapError(ex => DidException(ex))

  def provideAgentFor[R, E <: Exception, A](req: Request, job: ZIO[R & AgentWithSocketManager, E, A]) =
    for {
      agent <- ZIO.serviceWithZIO[AgentByHost](_.agentFromRequest(req))
      ret <- job.provideSomeEnvironment((env: ZEnvironment[R]) => env.add(agent))
    } yield ()

  def hostFromRequest(req: Request): Option[Host] =
    req.headers
      .get(MyHeaders.xForwardedHost)
      .map(_.toString()) // CharSequence -> String
      .map { // A bit of a hack to support a not standards http client
        case str if str.endsWith(":443") => str.dropRight(4)
        case str if str.endsWith(":80")  => str.dropRight(3)
        case str                         => str
      }
      .orElse(req.header(Header.Host).map(_.hostAddress))
      .map(Host(_))

  val layer = ZLayer(
    for {
      // Host.fabio -> AgentProvider.fabio TODO
      alice <- AgentWithSocketManager.make(AgentProvider.alice)
      bob <- AgentWithSocketManager.make(AgentProvider.bob)
      charlie <- AgentWithSocketManager.make(AgentProvider.charlie)
      local <- AgentWithSocketManager.make(AgentProvider.local)
    } yield AgentByHost(
      defaultAgent = local,
      agents = Map(
        Host.alice -> alice,
        Host.bob -> bob,
        Host.charlie -> charlie,
      )
    )
  )
}

case class AgentByHost(defaultAgent: AgentWithSocketManager, agents: Map[Host, AgentWithSocketManager]) {

  def agentFromRequest(req: Request): zio.ZIO[Any, Nothing, AgentWithSocketManager] =
    AgentByHost
      .hostFromRequest(req)
      .flatMap { host => agents.get(host).map(agent => ZIO.succeed(agent)) }
      .getOrElse(ZIO.succeed(defaultAgent))

  def agentFromHost(host: Host): zio.ZIO[Any, NoAgent, AgentWithSocketManager] =
    agents.get(host) match
      case None        => ZIO.fail(NoAgent(s"No Agent config for '$host'"))
      case Some(agent) => ZIO.succeed(agent)
}
