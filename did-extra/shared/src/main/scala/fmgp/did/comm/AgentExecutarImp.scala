package fmgp.did.comm

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.util._
import zio.stream.ZStream

case class AgentExecutarImp(
    agent: Agent,
    transportManager: Ref[TransportManager],
) extends AgentExecutar {
  val scope = Scope.global // TODO do not use global
  val indentityLayer = ZLayer.succeed(agent)
  override def subject: DIDSubject = agent.id.asDIDSubject

  override def receiveMsg(msg: EncryptedMessage, transport: Transport[Any, String]): URIO[Operations, Unit] = {
    def longRunningProgram(transport: Transport[Any, String]) = transport.inbound
      .mapZIO(data =>
        data.fromJson[EncryptedMessage] match // TODO sign message as also ok
          case Left(value) => ZIO.log(s"AgentExecutar inbound: '$data'") *> ZIO.none
          case Right(eMsg) => jobExecuterProtocol(eMsg, transport)
      )
      .runDrain
      .forkIn(scope)
    for {
      job <- longRunningProgram(transport)
      _ <- transport.send(s"You are now connected to $subject")
      ret <- jobExecuterProtocol(msg, transport) // Run a single time (for the message already read)
    } yield ()
  }

  // FIXME REMOVE THIS
  def messageDispatcher = new MessageDispatcher {
    def send(
        msg: EncryptedMessage,
        /*context*/
        destination: String
    ): ZIO[Any, DidFail, String] = ZIO.log(s"SEND=${msg.toJson}") *> ZIO.succeed("/return data/")
  }

  def jobExecuterProtocol(
      eMsg: EncryptedMessage,
      transport: Transport[Any, String],
  ): URIO[Operations, Option[EncryptedMessage]] =
    this
      .receiveMessage(eMsg, transport: Transport[Any, String])
      .tapError(ex => ZIO.log(ex.toString))
      .provideSomeLayer(DynamicResolver.layer ++ AgentExecutarImp.protocolHandlerLayer ++ this.indentityLayer)
      .provideSomeEnvironment((env: ZEnvironment[Operations]) => env.add(messageDispatcher))
      .orDieWith(ex => new RuntimeException(ex.toJson))

  def receiveMessage(msg: EncryptedMessage, transport: Transport[Any, String]): ZIO[
    Agent & Resolver & ProtocolExecuter[AgentExecutarImp.Services] & Operations & MessageDispatcher,
    DidFail,
    Option[EncryptedMessage]
  ] =
    ZIO.logAnnotate("msg_sha256", msg.sha256) {
      for {
        _ <- ZIO.log(s"receiveMessage with sha256: ${msg.sha256}")
        agent <- ZIO.service[Agent]
        _ <- transportManager.get.flatMap { m =>
          ZIO.foreach(msg.recipientsSubject)(subject => m.publish(subject.asTO, msg.toJson))
        }
        maybeSyncReplyMsg <-
          if (!msg.recipientsSubject.contains(agent.id.asDIDSubject))
            ZIO.logError(
              s"This agent '${agent.id.asDIDSubject}' is not a recipient"
            ) *> ZIO.none // TODO send a FAIL!!!!!!
          else
            for {
              plaintextMessage <- AgentExecutarImp.decrypt(msg)
              _ <- plaintextMessage.from match
                case None       => ZIO.unit
                case Some(from) => transportManager.update { _.link(from.asFROMTO, transport) }
              // TODO Store context of the decrypt unwarping
              // TODO SreceiveMessagetore context with MsgID and PIURI
              protocolHandler <- ZIO.service[ProtocolExecuter[AgentExecutarImp.Services]]
              ret <- protocolHandler
                .execute(plaintextMessage)
                .tapError(ex => ZIO.logError(s"Error when execute Protocol: $ex"))
            } yield ret
      } yield maybeSyncReplyMsg
    }
}

object AgentExecutarImp {

  type Services = Resolver & Agent & Operations & MessageDispatcher

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
