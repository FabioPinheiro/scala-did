package fmgp.did.framework

import zio._
import zio.json._
import zio.stream._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._

class AgentExecutarImp(
    agent: Agent,
    transportManager: Ref[TransportManager],
    protocolHandler: ProtocolExecuter[Resolver & Agent & Operations, DidFail],
) extends AgentExecutar {
  val scope = Scope.global // TODO do not use global
  val indentityLayer = ZLayer.succeed(agent)
  override def subject: DIDSubject = agent.id.asDIDSubject

  override def acceptTransport(transport: TransportDIDComm[Any]): URIO[Operations & Resolver, Unit] =
    transport.inbound
      .mapZIO(msg => jobExecuterProtocol(msg, transport))
      .runDrain
      .forkIn(scope)
      .unit // From Fiber.Runtime[fmgp.util.Transport.InErr, Unit] to Unit

  override def receiveMsg(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any]
  ): URIO[Operations & Resolver, Unit] = {
    for {
      job <- acceptTransport(transport)
      ret <- jobExecuterProtocol(msg, transport) // Run a single time (for the message already read)
    } yield ()
  }

  private def jobExecuterProtocol(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any],
  ): URIO[Operations & Resolver, Unit] =
    this
      .receiveMessage(msg, transport)
      .tapError(ex => ZIO.log(ex.toString))
      .provideSomeLayer(this.indentityLayer)
      .orDieWith(ex => new RuntimeException(ex.toJson))

  def receiveMessage(msg: SignedMessage | EncryptedMessage, transport: TransportDIDComm[Any]): ZIO[
    Agent & Resolver & Operations,
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
        } else {
          for {

            pMsg <- AgentExecutarImp.decrypt(msg)
            _ <- pMsg.from match
              case None       => ZIO.unit
              case Some(from) => transportManager.update { _.link(from.asFROMTO, transport) }
            _ <- processMessage(pMsg, transport)
          } yield ()
        }
    } yield ()
  }

  private def processMessage(plaintextMessage: PlaintextMessage, transport: TransportDIDComm[Any]) = for {
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
            case Some(ReturnRoute.none) | None =>
              for {
                transportDispatcher: TransportDispatcher <- transportManager.get
                _ <- reply.msg.to.toSeq.flatten match {
                  case Seq() => ZIO.logWarning("This reply message will be sented to nobody: " + reply.msg.toJson)
                  case tos =>
                    ZIO.foreachParDiscard(tos) { to =>
                      transportDispatcher.send(to = to, msg = message, thid = reply.msg.thid, pthid = reply.msg.pthid)
                    }
                }
              } yield ()
            case Some(ReturnRoute.all) | Some(ReturnRoute.thread) => transport.send(message)
        } yield ()
  } yield ()
}

object AgentExecutarImp {

  type Services = Resolver & Agent & Operations

  def make[S >: Resolver & Operations](
      agent: Agent,
      protocolHandler: ProtocolExecuter[Resolver & Agent & Operations, DidFail]
  ): ZIO[TransportFactory, Nothing, AgentExecutar] =
    for {
      transportManager <- TransportManager.make
      agentExecutar = new AgentExecutarImp(agent, transportManager, protocolHandler)
    } yield agentExecutar

  // TODO move into the class
  val basicProtocolHandlerLayer: ULayer[ProtocolExecuter[Services, DidFail]] =
    ZLayer.succeed(
      ProtocolExecuterCollection(
        BasicMessageExecuter,
        new TrustPingExecuter,
      )(NullProtocolExecute)
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
