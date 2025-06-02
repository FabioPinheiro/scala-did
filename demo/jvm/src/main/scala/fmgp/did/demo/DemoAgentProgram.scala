package fmgp.did.demo

import zio._
import fmgp.did._
import fmgp.did.framework._
import fmgp.did.comm._

object DemoAgentProgram {}

case class DemoAgentProgram(
    agent: Agent,
    // transportManager: Ref[MediatorTransportManager],
    // protocolHandler: ProtocolExecuter[OperatorImp.MediatorServices, MediatorError | StorageError],
    // userAccountRepo: UserAccountRepo,
    // messageItemRepo: MessageItemRepo,
    // scope: Scope,
    // problemReportBuilder: ProblemReportBuilder,
) extends AgentProgram {
  val indentityLayer = ZLayer.succeed(agent)
  override def subject: DIDSubject = agent.id.asDIDSubject

  override def acceptTransport(
      transport: TransportDIDComm[Any]
  ): URIO[Operations & Resolver, Unit] = ???
  // for {
  //   _ <- transportManager.update { _.registerTransport(transport) }
  //   _ <- transport.inbound
  //     .mapZIO(msg => jobExecuterProtocol(msg, transport))
  //     .runDrain
  //     .forkIn(scope)
  //     .unit // From Fiber.Runtime[fmgp.util.Transport.InErr, Unit] to Unit
  // } yield ()

  def receiveMsg(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any]
  ): URIO[Operations & Resolver, Unit] = ???
}
