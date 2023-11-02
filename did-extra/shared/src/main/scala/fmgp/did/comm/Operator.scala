package fmgp.did.comm

import zio._
import zio.json._
import fmgp.did._
import fmgp.util._

/** Telecommunications operator for DIDComm */
case class Operator(
    selfOperator: AgentExecutar,
    contacts: Seq[AgentExecutar] // TODO make these list change dynamically
) {
  private def everybody = (selfOperator +: contacts).map(e => e.subject -> e).toMap

  def getAgentExecutar(subject: Set[DIDSubject]): Set[AgentExecutar] =
    subject.flatMap(everybody.get)

  def receiveTransport(transport: Transport[Any, String]): ZIO[Operations, Nothing, Unit] =
    transport.inbound
      .map(_.fromJson[EncryptedMessage]) // TODO sign message are also ok ...
      .collectRight // This filter any msg not EncryptedMessage
      .mapZIO { msg =>
        val toBeInfor = getAgentExecutar(msg.recipientsSubject)
        ZIO.foreachParDiscard(toBeInfor)(contact => contact.receiveMsg(msg, transport)) *>
          ZIO.succeed(true)
      }
      .takeUntil(i => i)
      .runDrain
}
