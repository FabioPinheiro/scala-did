package fmgp.did.comm

import zio._
import zio.json._
import zio.http._
import zio.http.Header.{AccessControlAllowOrigin, AccessControlAllowMethods}

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.method.peer.DIDPeer.AgentDIDPeer
import fmgp.did.demo.AgentByHost
import io.netty.handler.codec.http.HttpHeaderNames
import fmgp.util.TransportWSImp

//FIXME REMOVE AgentWithSocketManager
case class AgentWithSocketManager(
    override val id: DID,
    override val keyStore: KeyStore, // Should we make it lazy with ZIO? I think so in the future we might want to separate the keys from the agent
    transportManager: Ref[TransportManager],
) extends Agent {

  type Services = Resolver & Agent & Operations & MessageDispatcher
  val protocolHandlerLayer: ULayer[ProtocolExecuter[Services]] =
    ZLayer.succeed(
      ProtocolExecuterCollection[Services](
        BasicMessageExecuter,
        new TrustPingExecuter,
      )
    )

  private def _didSubjectAux = id
  private def _keyStoreAux = keyStore

  val indentityLayer = ZLayer.succeed(this)
  // val indentityLayer = ZLayer.succeed(new Agent {
  //   override def id: DID = _didSubjectAux
  //   override def keyStore: KeyStore = _keyStoreAux
  // })

  val messageDispatcherLayer: ZLayer[Client & Scope, DidFail, MessageDispatcher] =
    MessageDispatcherJVM.layer.mapError(ex => SomeThrowable(ex))

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

  def receiveMessage(
      data: String,
      mSocketID: Option[SocketID],
  ): ZIO[Operations & MessageDispatcher, DidFail, Option[EncryptedMessage]] =
    for {
      msg <- data.fromJson[EncryptedMessage] match // TODO support SignedMessage
        case Left(error) => ZIO.fail(FailToParse(error))
        case Right(message) =>
          ZIO.logDebug(
            "Message's recipients KIDs: " + message.recipientsKid.mkString(",") +
              "; DID: " + "Message's recipients DIDs: " + message.recipientsSubject.mkString(",")
          ) *> ZIO.succeed(message)
      maybeSyncReplyMsg <- receiveMessage(msg, mSocketID)
    } yield (maybeSyncReplyMsg)

  def receiveMessage(
      msg: EncryptedMessage,
      mSocketID: Option[SocketID]
  ): ZIO[Operations & MessageDispatcher, DidFail, Option[EncryptedMessage]] =
    ZIO
      .logAnnotate("msg_sha256", msg.sha256) {
        for {
          _ <- ZIO.log(s"receiveMessage with sha256: ${msg.sha256}")
          _ <- transportManager.get.flatMap { m =>
            ZIO.foreach(msg.recipientsSubject)(subject => m.publish(subject.asTO, msg.toJson))
          }
          maybeSyncReplyMsg <-
            if (!msg.recipientsSubject.contains(id))
              ZIO.logError(s"This agent '${id.string}' is not a recipient")
                *> ZIO.none // TODO send a FAIL!!!!!!
            else
              for {
                plaintextMessage <- decrypt(msg)
                _ <- mSocketID match
                  case None => ZIO.unit
                  case Some(socketID) =>
                    plaintextMessage.from match
                      case None       => ZIO.unit
                      case Some(from) => transportManager.update { _.link(from.asFROMTO, socketID) }
                // TODO Store context of the decrypt unwarping
                // TODO SreceiveMessagetore context with MsgID and PIURI
                protocolHandler <- ZIO.service[ProtocolExecuter[Services]]
                ret <- protocolHandler
                  .execute(plaintextMessage)
                  .tapError(ex => ZIO.logError(s"Error when execute Protocol: $ex"))
              } yield ret
        } yield maybeSyncReplyMsg
      }
      .provideSomeLayer(DynamicResolver.layer ++ indentityLayer ++ protocolHandlerLayer)

}

object AgentWithSocketManager {

  def make(id: DID, keyStore: KeyStore): ZIO[Any, Nothing, AgentWithSocketManager] = for {
    sm <- TransportManager.make
  } yield AgentWithSocketManager(id, keyStore, sm)

  def make(agent: AgentDIDPeer): ZIO[Any, Nothing, AgentWithSocketManager] = for {
    sm <- TransportManager.make
  } yield AgentWithSocketManager(agent.id, agent.keyStore, sm)

  // def didCommApp: HttpApp[Hub[String] & AgentByHost & Operations & MessageDispatcher] = Routes(
  def didCommApp = Routes(
    Method.GET / "ws" -> handler { (req: Request) =>
      for {
        annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
        webSocketApp = TransportWSImp.createWebSocketAppWithOperator(annotationMap)
        ret <- webSocketApp.toResponse
      } yield (ret)
    },
    Method.POST / trailing -> handler { (req: Request) =>
      if (
        req
          // FIXME after https://github.com/zio/zio-http/issues/2416
          // .header(Header.ContentType)
          // .exists { h =>
          //   h.mediaType.mainType == MediaTypes.mainType &&
          //   (h.mediaType.subType == MediaTypes.SIGNED.subType || h.mediaType.subType == MediaTypes.ENCRYPTED.subType)
          .headers
          .get("content-type")
          .exists { h =>
            h == MediaTypes.SIGNED.typ || h == MediaTypes.ENCRYPTED.typ
          }
      ) {
        (for {
          agent <- AgentByHost.getAgentFor(req)
          data <- req.body.asString
          maybeSyncReplyMsg <- agent
            .receiveMessage(data, None)
            .mapError(fail => DidException(fail))
            .provideSomeEnvironment((env: ZEnvironment[Operations & MessageDispatcher]) => env.add(agent))
          ret = maybeSyncReplyMsg match
            case None        => Response.ok
            case Some(value) => Response.json(value.toJson)
        } yield ret).orDie
      } else
        (for {
            agent <- AgentByHost.getAgentFor(req)
            data <- req.body.asString
            ret <- agent
              .receiveMessage(data, None)
              .provideSomeEnvironment((env: ZEnvironment[Operations & MessageDispatcher]) => env.add(agent))
              .mapError(fail => DidException(fail))
          } yield Response
            .text(s"The content-type must be ${MediaTypes.SIGNED.typ} or ${MediaTypes.ENCRYPTED.typ}")
            // .copy(status = Status.BadRequest) but ok for now
        ).orDie
    },
  ).toHttpApp
}
