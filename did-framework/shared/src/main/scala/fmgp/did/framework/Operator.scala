package fmgp.did.framework

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._

/** Telecommunications operator for DIDComm */
case class Operator(
    selfOperator: AgentExecutar,
    contacts: Seq[AgentExecutar] // TODO make these list change dynamically
) {
  private def everybody = (selfOperator +: contacts).map(e => e.subject -> e).toMap

  def getAgentExecutar(subject: Set[DIDSubject]): Set[AgentExecutar] =
    subject.flatMap(everybody.get)

  def receiveTransport(transport: TransportDIDComm[Any]): ZIO[Operations & Resolver, DidFail, Unit] =
    transport.inbound
      .mapZIO {
        case sMsg: SignedMessage =>
          ZIO
            .fromEither(sMsg.payloadAsPlaintextMessage)
            .flatMap { pMsg =>
              val recipients = pMsg.to.toSet.flatten.map(_.toDIDSubject)
              val toBeInfor = getAgentExecutar(recipients)
              if (toBeInfor.isEmpty) ZIO.logWarning("No Agent to inform") *> ZIO.succeed(false)
              else ZIO.foreachParDiscard(toBeInfor)(_.receiveMsg(sMsg, transport)) *> ZIO.succeed(true)
            }
        case eMsg: EncryptedMessage =>
          val recipients = eMsg.recipientsSubject
          val toBeInfor = getAgentExecutar(recipients)
          if (toBeInfor.isEmpty) ZIO.logWarning("No Agent to inform") *> ZIO.succeed(false)
          else ZIO.foreachParDiscard(toBeInfor)(_.receiveMsg(eMsg, transport)) *> ZIO.succeed(true)
      }
      .takeUntil(i => i)
      .runDrain
}
