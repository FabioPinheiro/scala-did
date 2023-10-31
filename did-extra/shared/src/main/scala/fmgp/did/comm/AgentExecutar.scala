package fmgp.did.comm

import zio._
import fmgp.did._
import fmgp.util._
import zio.stream.ZStream

case class AgentExecutar(agent: Agent) {
  def subject: DIDSubject = agent.id.asDIDSubject
  val scope = Scope.global // FIXME

  def program(transport: Transport[Any, String]) = transport.inbound
    .mapZIO(eee => ZIO.log(s"AgentExecutar NEW MESSAGE IN inbound $eee"))
    .runDrain
    .forkIn(scope)

  def receiveMsg(msg: EncryptedMessage, transport: Transport[Any, String]): UIO[Unit] =
    for {
      job <- program(transport)
      _ <- transport.send(s"You are now connected to $subject")
    } yield ()
}
