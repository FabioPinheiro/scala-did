package fmgp.webapp

import scala.scalajs.js.timers._
import scala.scalajs.js.JSConverters._

import org.scalajs.dom
import org.scalajs.dom.HTMLButtonElement
import com.raquo.airstream.core.Sink
import com.raquo.airstream.ownership._
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.extension._
import fmgp.did.method.peer.DIDPeer2
import fmgp.did.uniresolver.Uniresolver
import fmgp.did.comm.protocol.routing2.ForwardMessage
import fmgp.crypto.error._
import fmgp.Utils
import fmgp.ServiceWorkerUtils
import fmgp.Config
import fmgp.NotificationsSubscription

object EncryptTool {

  sealed trait Command {
    def index: Int
    def uri: String
    def command: String
    def copyButton: ReactiveHtmlElement[HTMLButtonElement]
    protected def program: ZIO[Resolver, DidFail, Unit]
    def executeCommand: Fiber[Nothing, Unit] = Utils.runProgram(program.provide(Global.resolverLayer))
    def executeCommandButton =
      button(
        "Execute Command",
        onClick --> Sink.jsCallbackToSink(_ =>
          encryptedMessageVar.now() match {
            case Some(Right((plaintext, eMsg))) =>
              Global.messageSend(eMsg, Global.agentVar.now().map(_.id.asFROM).getOrElse(???), plaintext) // side effect
              executeCommand // side effect
            case _ => // None
          }
        )
      )
  }
  case class CommandHttp(index: Int, uri: String, msg: EncryptedMessage) extends Command {
    def command = s"""curl -X POST $uri -H 'content-type: application/didcomm-encrypted+json' -d '${msg.toJson}'"""
    def copyButton = button("Copy curl command", onClick --> { _ => Global.copyToClipboard(command) })
    def program = Client
      .makeDIDCommPost(msg, uri) // TODO this should be a TransportDIDComm
      .map { // side effects
        case None => Left(s"Zero responses in this Transport in the time frame of ${Global.transportTimeoutVar.now()}s")
        case Some(output) =>
          import SignedOrEncryptedMessage.given
          output.fromJson[SignedOrEncryptedMessage] match
            case Left(fail) => Left(s"Fail to parse due to: '$fail'")
            case right      => right
      }
      .tapBoth(
        error => ZIO.logError(error.toString) *> ZIO.succeed(outputFromCallVar.update(_ :+ Left(error.toString()))),
        item => ZIO.succeed(outputFromCallVar.update(_ :+ item)) // side effect
      )
      .unit
  }
  case class CommandWs(index: Int, uri: String, msg: EncryptedMessage) extends Command {
    def command = s"""wscat -c $uri -w 3 -x '${msg.toJson}'"""
    def copyButton = button("Copy wscat command", onClick --> { _ => Global.copyToClipboard(command) })
    def program =
      for {
        transport <- Utils.openWsProgram(wsUrl = uri, timeout = Global.transportTimeoutVar.now())
        _ <- transport.send(msg)
        _ <- transport.inbound.foreach {
          case eMsg: EncryptedMessage => ZIO.succeed(outputFromCallVar.update(_ :+ Right(eMsg)))
          case sMsg: SignedMessage =>
            ZIO.succeed(outputFromCallVar.update(_ :+ Left("UI not implemented for SignedMessage"))) // TODO
        }
      } yield ()
  }
  case class CommandInvalid(index: Int, uri: String) extends Command {
    def command = s"unknown protocol: '$uri'"
    def copyButton = button("Copy command", disabled := true)
    def program = ZIO.fail(FailToParse(command))
  }

  val encryptedMessageVar: Var[Option[Either[DidFail, (PlaintextMessage, EncryptedMessage)]]] = Var(initial = None)
  val signMessageVar: Var[Option[Either[DidFail, (PlaintextMessage, SignedMessage)]]] = Var(initial = None)
  val dataTextVar: Var[String] = Var(initial = MessageTemplate.exPlaintextMessage.toJsonPretty)
  val commandSeqVar: Var[Seq[Command]] = Var(initial = Seq.empty)
  val outputFromCallVar = Var[Seq[Either[String, SignedMessage | EncryptedMessage]]](initial = Seq.empty)
  val forwardMessageVar = Var[Option[ForwardMessage]](initial = None)

  def plaintextMessage = dataTextVar.signal.map(_.fromJson[PlaintextMessage])

  def cleanupVars = {
    encryptedMessageVar.set(None)
    signMessageVar.set(None)
    commandSeqVar.set(Seq.empty)
    outputFromCallVar.set(Seq.empty)
    forwardMessageVar.set(None)
  }

  def jobNextForward(owner: Owner) = {
    def program(pMsg: PlaintextMessage, eMsg: EncryptedMessage): ZIO[Any, DidFail, Option[ForwardMessage]] = {
      pMsg.to.flatMap(_.headOption) match
        case None => ZIO.none
        case Some(originalTO) =>
          for {
            resolver <- ZIO.service[Resolver]
            doc <- resolver.didDocument(originalTO)
            mMediatorDid = doc.getDIDServiceDIDCommMessaging.headOption.toSeq
              .flatMap(_.getServiceEndpointNextForward)
              .headOption
            forwardMessage = mMediatorDid.flatMap(mediatorDid =>
              ForwardMessage
                .buildForwardMessage(
                  to = Set(mediatorDid.asTO),
                  next = originalTO.asDIDURL.toDID,
                  msg = eMsg,
                )
                .toOption
            )
          } yield forwardMessage
    }.provide(Global.resolverLayer)

    encryptedMessageVar.signal
      .map {
        case Some(Right((pMsg, eMsg))) =>
          Unsafe.unsafe { implicit unsafe =>
            Runtime.default.unsafe.fork(
              program(pMsg: PlaintextMessage, eMsg: EncryptedMessage)
                .map(forwardMessageVar.set(_))
            )
          } // Run side effect
        case _ => None
      }
      .observe(owner)
  }

  def callEncryptedViaRPC(owner: Owner) = Signal
    .combine(
      Global.agentVar,
      plaintextMessage
    )
    .map {
      case (_, Left(_))                                              => cleanupVars
      case (_, Right(pMsg)) if pMsg.to.flatMap(_.headOption).isEmpty => cleanupVars
      case (None, Right(pMsg)) if pMsg.from.isDefined                => cleanupVars
      case (None, Right(pMsg)) =>
        val program = OperationsClientRPC
          // .encrypt(pMsg) // always use the message data (FROM/TO) to Encrypt
          .anonEncrypt(pMsg) // if the agent is not seleted the message is encrypted anonymously
          .either
          .map(_.map((pMsg, _)))
          .map(e => encryptedMessageVar.update(_ => Some(e)))
        Unsafe.unsafe { implicit unsafe => // Run side effect
          Runtime.default.unsafe.fork(
            program.provideSomeLayer(Global.resolverLayer)
          )
        }
      case (Some(agent), Right(pMsg)) if pMsg.from.isDefined && !pMsg.from.contains(agent.id.asFROM) =>
        cleanupVars
      case (Some(agent), Right(pMsg)) =>
        val program = OperationsClientRPC
          .encrypt(pMsg) // .authEncrypt(pMsg)
          .either
          .map(_.map((pMsg, _)))
          .map(e => encryptedMessageVar.update(_ => Some(e)))

        Unsafe.unsafe { implicit unsafe => // Run side effect
          Runtime.default.unsafe.fork(
            program
              .provideSomeLayer(Global.resolverLayer)
              .provideEnvironment(ZEnvironment(agent))
          )
        }
    }
    .observe(owner)

  def callSignViaRPC(owner: Owner) = Signal
    .combine(
      Global.agentVar,
      plaintextMessage
    )
    .map {
      case (_, Left(_))        => signMessageVar.set(None)
      case (None, Right(pMsg)) => signMessageVar.set(None)
      case (Some(agent), Right(pMsg)) =>
        val programSign = OperationsClientRPC
          .sign(pMsg)
          .either
          .map(_.map((pMsg, _)))
          .map(e => signMessageVar.update(_ => Some(e)))
        Unsafe.unsafe { implicit unsafe => // Run side effect
          Runtime.default.unsafe.fork(
            programSign
              .provideSomeLayer(Global.resolverLayer)
              .provideEnvironment(ZEnvironment(agent))
          )
        }
    }
    .observe(owner)

  // FIXME racing problem
  def callCommand(owner: Owner) = encryptedMessageVar.signal
    .map(_.flatMap(_.toOption))
    .map(_.map { (_, eMsg) =>
      val program = for {
        resolver <- ZIO.service[Resolver]
        doc <- resolver.didDocument(TO(eMsg.recipientsSubject.head.string))
        didCommMessagingServices = doc.getDIDServiceDIDCommMessaging
        mURI = didCommMessagingServices.flatMap(_.endpoints.map(e => e.uri))
        ret = commandSeqVar.set(
          mURI.zipWithIndex.map {
            case (uri, index) if uri.startsWith("ws:") | uri.startsWith("wss:")     => CommandWs(index, uri, eMsg)
            case (uri, index) if uri.startsWith("http:") | uri.startsWith("https:") => CommandHttp(index, uri, eMsg)
            case (uri, index)                                                       => CommandInvalid(index, uri)
          }
        )
      } yield (ret)

      Unsafe.unsafe { implicit unsafe => // Run side effect
        Runtime.default.unsafe.fork(
          program.provide(Global.resolverLayer)
        )
      }
    })
    .observe(owner)

  val rootElement = div(
    onMountCallback { ctx =>
      callSignViaRPC(ctx.owner) // side effect
      callEncryptedViaRPC(ctx.owner) // side effect
      jobNextForward(ctx.owner) // side effect
      callCommand(ctx.owner) // side effect
      ()
    },
    code("Encrypt/Sign Tool"),
    p(
      overflowWrap.:=("anywhere"),
      "Agent: ",
      " ",
      child <-- Global.agentVar.signal
        .map(_.map(agent => code(AppUtils.linkToResolveDID(agent.id))).getOrElse(code(Global.noneOption))),
    ),
    p(
      overflowWrap.:=("anywhere"),
      "Send TO (used by Templates only. Does not influence the encryption): ",
      Global.makeSelectElementTO(Global.recipientVar),
      " ",
      child <-- Global.recipientVar.signal
        .map(_.map(to => code(AppUtils.linkToResolveDID(to.toDIDSubject))).getOrElse(code(Global.noneOption))),
    ),
    p(
      "Templates:",
      button(
        "PlaintextMessage",
        onClick --> Observer(_ => dataTextVar.set(MessageTemplate.exPlaintextMessage.toJsonPretty))
      ),
      ul(
        li(
          button(
            "ForwardMessageBase64",
            onClick --> Observer(_ =>
              dataTextVar.set(MessageTemplate.exForwardMessageBase64.toPlaintextMessage.toJsonPretty)
            )
          ),
          button(
            "ForwardMessageJson",
            onClick --> Observer(_ =>
              dataTextVar.set(MessageTemplate.exForwardMessageJson.toPlaintextMessage.toJsonPretty)
            )
          ),
          button(
            "TrustPing",
            onClick --> Observer(_ => dataTextVar.set(MessageTemplate.exTrustPing.toPlaintextMessage.toJsonPretty))
          ),
          button(
            "TrustPingResponse",
            onClick --> Observer(_ =>
              dataTextVar.set(MessageTemplate.exTrustPingResponse.toPlaintextMessage.toJsonPretty)
            )
          ),
          button(
            "BasicMessage",
            onClick --> Observer(_ => dataTextVar.set(MessageTemplate.exBasicMessage.toPlaintextMessage.toJsonPretty))
          ),
          // ReportProblem V2.0
          button(
            "ProblemReport",
            onClick --> Observer(_ =>
              dataTextVar.set(MessageTemplate.ReportProblem2.exProblemReport.toPlaintextMessage.toJsonPretty)
            )
          ),
        ),
        // Mediator Coordination V2.0
        {
          import MessageTemplate.Mediatorcoordination2._
          li(
            button(
              "MediateRequest2",
              onClick --> Observer(_ => dataTextVar.set(exMediateRequest2.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "MediateGrant2",
              onClick --> Observer(_ => dataTextVar.set(exMediateGrant2.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "MediateDeny2",
              onClick --> Observer(_ => dataTextVar.set(exMediateDeny2.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "KeylistUpdate2",
              onClick --> Observer(_ => dataTextVar.set(exKeylistUpdate2.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "KeylistResponse2",
              onClick --> Observer(_ => dataTextVar.set(exKeylistResponse2.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "KeylistQuery2",
              onClick --> Observer(_ => dataTextVar.set(exKeylistQuery2.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Keylist2",
              onClick --> Observer(_ => dataTextVar.set(exKeylist2.toPlaintextMessage.toJsonPretty))
            )
          )
        },
        // Mediator Coordination V3.0
        {
          import MessageTemplate.Mediatorcoordination3._
          li(
            button(
              "MediateRequest3",
              onClick --> Observer(_ => dataTextVar.set(exMediateRequest3.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "MediateGrant3",
              onClick --> Observer(_ => dataTextVar.set(exMediateGrant3.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "MediateDeny3",
              onClick --> Observer(_ => dataTextVar.set(exMediateDeny3.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "RecipientUpdate3",
              onClick --> Observer(_ => dataTextVar.set(exRecipientUpdate3.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "RecipientResponse3",
              onClick --> Observer(_ => dataTextVar.set(exRecipientResponse3.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "RecipientQuery3",
              onClick --> Observer(_ => dataTextVar.set(exRecipientQuery3.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Recipient3",
              onClick --> Observer(_ => dataTextVar.set(exRecipient3.toPlaintextMessage.toJsonPretty))
            )
          )
        },
        // Pickup V3.0
        {
          import MessageTemplate.Pickup3._
          li(
            button(
              "Status",
              onClick --> Observer(_ => dataTextVar.set(exStatus.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "StatusRequest",
              onClick --> Observer(_ => dataTextVar.set(exStatusRequest.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "DeliveryRequest",
              onClick --> Observer(_ => dataTextVar.set(exDeliveryRequest.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "MessageDelivery",
              onClick --> Observer(_ => dataTextVar.set(exMessageDelivery.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "MessagesReceived",
              onClick --> Observer(_ => dataTextVar.set(exMessagesReceived.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "LiveModeChange",
              onClick --> Observer(_ => dataTextVar.set(exLiveModeChange.toPlaintextMessage.toJsonPretty))
            ),
          )
        },
        // DiscoverFeatures V2.0
        {
          import MessageTemplate.DiscoverFeatures2._
          li(
            button(
              "FeatureQuery",
              onClick --> Observer(_ => dataTextVar.set(exFeatureQuery.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "FeatureDisclose",
              onClick --> Observer(_ => dataTextVar.set(exFeatureDisclose.toPlaintextMessage.toJsonPretty))
            ),
          )
        },
        // PubSub (Draft Version)
        {
          import MessageTemplate.PubSub._
          li(
            button(
              "RequestToSubscribe",
              onClick --> Observer(_ => dataTextVar.set(exRequestToSubscribe.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "SetupToSubscribe",
              onClick --> Observer(_ => dataTextVar.set(exSetupToSubscribe.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Subscribe (Fake data)",
              onClick --> Observer(_ => dataTextVar.set(exSubscribe.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Subscribe",
              onClick --> Observer(_ =>
                Unsafe.unsafe { implicit unsafe => // Run side effect
                  Runtime.default.unsafe.runToFuture(
                    ServiceWorkerUtils
                      .subscribeToNotifications(Config.PushNotifications.applicationServerKey)
                      .map(ps => NotificationsSubscription.unsafeFromPushSubscription(ps))
                      .map(ns => dataTextVar.set(exSubscribe(ns).toPlaintextMessage.toJsonPretty))
                  )
                }
              )
            ),
            button(
              "Subscription",
              onClick --> Observer(_ => dataTextVar.set(exSubscription.toPlaintextMessage.toJsonPretty))
            ),
          )
        },
        // ProveControl (Draft Version)
        {
          import MessageTemplate.ProveControl._
          li(
            button(
              "RequestVerification",
              onClick --> Observer(_ => dataTextVar.set(exRequestVerification.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "VerificationChallenge",
              onClick --> Observer(_ => dataTextVar.set(exVerificationChallenge.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Prove",
              onClick --> Observer(_ => dataTextVar.set(exProve.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "ConfirmVerification",
              onClick --> Observer(_ => dataTextVar.set(exConfirmVerification.toPlaintextMessage.toJsonPretty))
            ),
          )
        },
        // ChatriqubeRegistry (Draft Version)
        {
          import MessageTemplate.ChatriqubeRegistry._
          li(
            button(
              "Enroll",
              onClick --> Observer(_ => dataTextVar.set(exEnroll.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Account",
              onClick --> Observer(_ => dataTextVar.set(exAccount.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "SetId",
              onClick --> Observer(_ => dataTextVar.set(exSetId.toPlaintextMessage.toJsonPretty))
            ),
          )
        },
        // ChatriqubeDiscovery (Draft Version)
        {
          import MessageTemplate.ChatriqubeDiscovery._
          li(
            button(
              "AskIntroduction",
              onClick --> Observer(_ => dataTextVar.set(exAskIntroduction.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "IntroductionStatus",
              onClick --> Observer(_ => dataTextVar.set(exIntroductionStatus.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "ForwardRequest",
              onClick --> Observer(_ => dataTextVar.set(exForwardRequest.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Request",
              onClick --> Observer(_ => dataTextVar.set(exRequest.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Answer",
              onClick --> Observer(_ => dataTextVar.set(exAnswer.toPlaintextMessage.toJsonPretty))
            ),
            button(
              "Handshake",
              onClick --> Observer(_ => dataTextVar.set(exHandshake.toPlaintextMessage.toJsonPretty))
            ),
          )
        }
      ),
    ),
    textArea(
      rows := 20,
      cols := 80,
      autoFocus(true),
      value <-- dataTextVar,
      inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> dataTextVar }
    ),
    div(child <-- plaintextMessage.map {
      case Left(error) => pre(code(""))
      case Right(msg) =>
        msg.return_route match
          case None =>
            div(
              pre(code("return_route is undefined (default)")),
              button(
                """Add "return_route":"all"""",
                onClick --> { _ =>
                  dataTextVar.set(
                    msg
                      .asInstanceOf[PlaintextMessageClass] // FIXME
                      .copy(return_route = Some(ReturnRoute.all))
                      .toJsonPretty
                  )
                }
              ),
            )
          case Some(value) => new CommentNode("")
    }),
    div(
      h2("Plaintext Message"),
      children <-- plaintextMessage.map {
        case Left(error) => Seq(pre(code(s"Error: $error")))
        case Right(msg) =>
          Seq(
            pre(code(msg.toJsonPretty)),
            button(
              "Copy Plaintext Message to clipboard",
              onClick --> { _ => Global.copyToClipboard(msg.toJson) }
            ),
            QRcodeTool.buttonMakeQRcode(msg),
          )
      }
    ),
    div(
      h2("Encrypted Message"),
      children <-- encryptedMessageVar.signal.map {
        case None              => Seq(code("None"))
        case Some(Left(error)) => Seq(code("Error when encrypting " + error.toJsonPretty))
        case Some(Right((plaintext, eMsg))) =>
          def sideEffectMessageSend = Global.agentVar
            .now()
            .map(_.id.asFROM)
            .foreach(from => Global.messageSend(eMsg, from, plaintext)) // side effect
          Seq(
            h6(
              "(NOTE: This is executed as a RPC call to the JVM server, since the JS version has not yet been fully implemented)"
            ),
            pre(code(eMsg.toJsonPretty)),
            button(
              "Copy Encrypted Message to clipboard",
              onClick --> (_ => { sideEffectMessageSend; Global.copyToClipboard(eMsg.toJson) })
            ),
            QRcodeTool.buttonMakeQRcode(eMsg, sideEffect = sideEffectMessageSend),
          )
      },
    ),
    div(
      h2("Sign Message"),
      children <-- signMessageVar.signal.map {
        case None              => Seq(code("None"))
        case Some(Left(error)) => Seq(code("Error when signing " + error.toJsonPretty))
        case Some(Right((plaintext, sMsg))) =>
          def sideEffectMessageSend = Global.agentVar
            .now()
            .map(_.id.asFROM)
            .foreach(from => Global.messageSend(sMsg, from, plaintext)) // side effect
          Seq(
            h6("(NOTE: This is executed as a RPC call to the JVM server)"),
            pre(code(sMsg.toJsonPretty)),
            button(
              "Copy Sign Message to clipboard",
              onClick --> (_ => { sideEffectMessageSend; Global.copyToClipboard(sMsg.toJson) })
            ),
            QRcodeTool.buttonMakeQRcode(sMsg, sideEffect = sideEffectMessageSend),
          )
      }
    ),
    h2("DIDCommMessaging"),
    p("DIDCommMessaging is the DID Comm transmission specified by the service endpoint in the DID Document"),
    child <-- forwardMessageVar.signal.map {
      case None => "No ForwardMessage"
      case Some(forwardMsg) =>
        val pMsg = forwardMsg.toPlaintextMessage.toJsonPretty
        div(
          button("Copy ForwardMessage into textbox", onClick --> { _ => dataTextVar.set(pMsg) }),
          pre(code(pMsg))
        )
    },
    div(
      children <-- commandSeqVar.signal
        .map(_.map { case c => //  new CommentNode("")
          div(
            p(code(c.command)),
            div(c.copyButton, c.executeCommandButton)
          )
        })
    ),
    hr(),
    div(button("Clean output replies", onClick --> { _ => outputFromCallVar.set(Seq.empty) })),
    ul(
      children <-- outputFromCallVar.signal.map(_.map {
        case Left(value) => li(pre(code(value)))
        case Right(reply) =>
          li(
            title := (reply: Message).toJsonPretty,
            "Got a message",
            button("Copy to clipboard", onClick --> { _ => Global.copyToClipboard((reply: Message).toJson) }),
            button(
              "Copy to Decryot/Verify Tool",
              onClick --> { _ => DecryptTool.dataVar.set((reply: Message).toJsonPretty) },
              MyRouter.navigateTo(MyRouter.DecryptPage)
            )
          )
      })
    ),
  )

  def apply(): HtmlElement = rootElement
}
