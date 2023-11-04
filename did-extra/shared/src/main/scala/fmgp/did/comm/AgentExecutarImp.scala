package fmgp.did.comm

import zio._
import zio.json._
import zio.stream._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.util._

case class AgentExecutarImp(
    agent: Agent,
    transportManager: Ref[TransportManager],
) extends AgentExecutar {
  val scope = Scope.global // TODO do not use global
  val indentityLayer = ZLayer.succeed(agent)
  override def subject: DIDSubject = agent.id.asDIDSubject

  override def receiveMsg(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any]
  ): URIO[Operations, Unit] = {
    for {
      job <- transport.inbound
        .mapZIO(msg => jobExecuterProtocol(msg, transport))
        .runDrain
        .forkIn(scope)
      ret <- jobExecuterProtocol(msg, transport) // Run a single time (for the message already read)
    } yield ()
  }

  def jobExecuterProtocol(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any],
  ): URIO[Operations, Unit] =
    this
      .receiveMessage(msg, transport)
      .tapError(ex => ZIO.log(ex.toString))
      .provideSomeLayer(DynamicResolver.layer ++ AgentExecutarImp.protocolHandlerLayer ++ this.indentityLayer)
      .orDieWith(ex => new RuntimeException(ex.toJson))

  def receiveMessage(msg: SignedMessage | EncryptedMessage, transport: TransportDIDComm[Any]): ZIO[
    Agent & Resolver & ProtocolExecuter[AgentExecutarImp.Services] & Operations,
    DidFail,
    Unit
  ] = ZIO.logAnnotate("msg_sha256", msg.sha256) {
    for {
      _ <- ZIO.logDebug(s"Receive message with sha256: '${msg.sha256}'")
      agent <- ZIO.service[Agent]
      recipientsSubject <- msg match
        case eMsg: EncryptedMessage => ZIO.succeed(eMsg.recipientsSubject)
        case sMsg: SignedMessage =>
          ZIO.fromEither(sMsg.payloadAsPlaintextMessage).map(_.to.toSet.flatten.map(_.toDIDSubject))
      _ <- transportManager.get.flatMap { m =>
        ZIO.foreach(recipientsSubject)(subject => m.publish(subject.asTO, msg))
      }
      _ <-
        if (!recipientsSubject.contains(agent.id.asDIDSubject)) {
          ZIO.logError(s"This agent '${agent.id.asDIDSubject}' is not a recipient") // TODO send a FAIL!!!!!!
        } else AgentExecutarImp.decrypt(msg).flatMap(pMsg => processMessage(pMsg, transport))
    } yield ()
  }

  def processMessage(plaintextMessage: PlaintextMessage, transport: TransportDIDComm[Any]) = for {
    _ <- plaintextMessage.from match
      case None       => ZIO.unit
      case Some(from) => transportManager.update { _.link(from.asFROMTO, transport) }
    protocolHandler <- ZIO.service[ProtocolExecuter[AgentExecutarImp.Services]]
    action <- protocolHandler
      .program(plaintextMessage)
      .tapError(ex => ZIO.logError(s"Error when execute Protocol: $ex"))
    ret <- action match
      case NoReply => ZIO.unit // TODO Maybe infor transport of immediately reply
      case reply: AnyReply =>
        import fmgp.did.comm.Operations._
        for {
          message <- reply.msg.to.toSeq.flatten match {
            case Seq() =>
              reply.msg.from match
                case Some(from) => sign(reply.msg)
                case None => ZIO.logError(s"No sender or recipient: ${reply.msg}") *> ZIO.fail(NoSenderOrRecipient)
            case tos => // TODO FIXME is case is not a response
              reply.msg.from match
                case Some(from) => authEncrypt(reply.msg)
                case None       => anonEncrypt(reply.msg)
          }
          _ <- plaintextMessage.return_route match
            case Some(ReturnRoute.none) | None => transport.send(message) // FIXME transportManager pick the best way
            case Some(ReturnRoute.all) | Some(ReturnRoute.thread) => transport.send(message)
        } yield ()
  } yield ()
}

object AgentExecutarImp {

  type Services = Resolver & Agent & Operations // & MessageDispatcher

  def make(agent: Agent): ZIO[Any, Nothing, AgentExecutar] =
    TransportManager.make.map(AgentExecutarImp(agent, _))

  // TODO move into the class
  val protocolHandlerLayer: ULayer[ProtocolExecuter[Services]] = ZLayer.succeed(
    ProtocolExecuterCollection[Services](
      BasicMessageExecuter,
      new TrustPingExecuter,
    )
  )

  // TODO move to another place & move validations and build a contex
  def decrypt(msg: Message): ZIO[Agent & Resolver & Operations, DidFail, PlaintextMessage] =
    for {
      ops <- ZIO.service[Operations]
      plaintextMessage <- msg match
        case pm: PlaintextMessage => ZIO.succeed(pm)
        case em: EncryptedMessage =>
          {
            em.`protected`.obj match
              case AnonProtectedHeader(epk, apv, typ, enc, alg)            => ops.anonDecrypt(em)
              case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) => ops.authDecrypt(em)
          }.flatMap(decrypt _)
        case sm: SignedMessage =>
          ops.verify(sm).flatMap {
            case false => ZIO.fail(ValidationFailed)
            case true =>
              sm.payload.content.fromJson[Message] match
                case Left(error) => ZIO.fail(FailToParse(error))
                case Right(msg2) => decrypt(msg2)
          }
    } yield (plaintextMessage)

}
