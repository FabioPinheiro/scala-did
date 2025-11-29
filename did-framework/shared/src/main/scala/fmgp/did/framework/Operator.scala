package fmgp.did.framework

import zio.*
import zio.json.*
import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*

/** Telecommunications operator for DIDComm */
case class Operator(
    selfOperator: AgentProgram,
    contacts: Seq[AgentProgram] // TODO make these list change dynamically
) {
  private def everybody = (selfOperator +: contacts).map(e => e.subject -> e).toMap

  def getAgentProgram(subject: Set[DIDSubject]): Set[AgentProgram] =
    subject.flatMap(everybody.get)

  def receiveTransport(transport: TransportDIDComm[Any]): ZIO[Operations & Resolver, DidFail, Unit] =
    transport.inbound
      .mapZIO {
        case sMsg: SignedMessage =>
          ZIO
            .fromEither(sMsg.payloadAsPlaintextMessage)
            .flatMap { pMsg =>
              val recipients = pMsg.to.toSet.flatten.map(_.toDIDSubject)
              val toBeInfor = getAgentProgram(recipients)
              if (toBeInfor.isEmpty) ZIO.logWarning("No Agent to inform") *> ZIO.succeed(false)
              else ZIO.foreachParDiscard(toBeInfor)(_.receiveMsg(sMsg, transport)) *> ZIO.succeed(true)
            }
        case eMsg: EncryptedMessage =>
          val recipients = eMsg.recipientsSubject
          val toBeInfor = getAgentProgram(recipients)
          if (toBeInfor.isEmpty) ZIO.logWarning("No Agent to inform") *> ZIO.succeed(false)
          else ZIO.foreachParDiscard(toBeInfor)(_.receiveMsg(eMsg, transport)) *> ZIO.succeed(true)
      }
      .takeUntil(i => i)
      .runDrain
}
