package fmgp.did.framework

import zio._
import zio.json._
import zio.stream._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._

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

  def agentPipeline[In](toBeInfor: Set[AgentProgram]) = // TODO
    toBeInfor.headOption.map(_.pipeMgs)
    ZPipeline.identity[In]

  def pipeline: ZPipeline[Operations & Resolver, String, String, String] =
    ZPipeline.branchAfter[Operations & Resolver, String, String, String](1) { chunk =>
      def nextPipiline[In] = Pipeline
        .parseMsg(chunk.asString)
        .flatMap { msg =>
          for {
            recipients <- msg match {
              case sMsg: SignedMessage =>
                sMsg.payloadAsPlaintextMessage match
                  case Left(error)  => ZIO.fail(error.toText)
                  case Right(value) => ZIO.succeed(value.to.toSet.flatten.map(_.toDIDSubject))
              case eMsg: EncryptedMessage => ZIO.succeed(eMsg.recipientsSubject)
            }
            toBeInfor = getAgentProgram(recipients)
            ret <- if (toBeInfor.isEmpty) ZIO.none else { ZIO.some(agentPipeline[In](toBeInfor)) }
          } yield ret

        }
      ZPipeline.fromChannel(
        ZChannel
          .fromZIO {
            nextPipiline[String].flatMap {
              case None       => ZIO.fail("No Agent for DID")
              case Some(pipe) => ZIO.succeed(ZPipeline.prepend(chunk) >>> pipe)
            }
          }
      )
    }
}
