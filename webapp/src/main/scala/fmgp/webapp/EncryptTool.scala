package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import com.raquo.airstream.ownership._
import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.extension._
import fmgp.did.method.peer.DIDPeer2
import fmgp.did.uniresolver.Uniresolver
import fmgp.crypto.error.DidFail
import com.raquo.airstream.core.Sink
import fmgp.did.comm.protocol.routing2.ForwardMessage

object EncryptTool {

  val encryptedMessageVar: Var[Option[Either[DidFail, (PlaintextMessage, EncryptedMessage)]]] = Var(initial = None)
  val signMessageVar: Var[Option[Either[DidFail, (PlaintextMessage, SignedMessage)]]] = Var(initial = None)
  val dataTextVar = Var(initial = MessageTemplate.exPlaintextMessage.toJsonPretty)
  val curlCommandVar: Var[Option[String]] = Var(initial = None)
  val outputFromCallVar = Var[Option[EncryptedMessage]](initial = None)
  val forwardMessageVar = Var[Option[ForwardMessage]](initial = None)

  def plaintextMessage = dataTextVar.signal.map(_.fromJson[PlaintextMessage])

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
    }.provide(ResolverTool.resolverLayer)

    encryptedMessageVar.signal
      .map {
        case Some(Right((pMsg, eMsg))) =>
          Unsafe.unsafe { implicit unsafe =>
            Runtime.default.unsafe.fork(
              program(pMsg: PlaintextMessage, eMsg: EncryptedMessage)
                .map(forwardMessageVar.set(_))
            )
          } // Run side efect
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
      case (_, Left(_))                                              => encryptedMessageVar.set(None)
      case (_, Right(pMsg)) if pMsg.to.flatMap(_.headOption).isEmpty => encryptedMessageVar.set(None)
      case (None, Right(pMsg)) =>
        val program = OperationsClientRPC
          // .encrypt(pMsg) // always use the message data (FROM/TO) to Encrypt
          .anonEncrypt(pMsg) // if the agent is not seleted the message is encrypted anonymously
          .either
          .map(_.map((pMsg, _)))
          .map(e => encryptedMessageVar.update(_ => Some(e)))
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program.provideSomeLayer(ResolverTool.resolverLayer)
          )
        }
      case (Some(agent), Right(pMsg)) =>
        val program = OperationsClientRPC
          .encrypt(pMsg) // .authEncrypt(pMsg)
          .either
          .map(_.map((pMsg, _)))
          .map(e => encryptedMessageVar.update(_ => Some(e)))

        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            program
              .provideSomeLayer(ResolverTool.resolverLayer)
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
        Unsafe.unsafe { implicit unsafe => // Run side efect
          Runtime.default.unsafe.fork(
            programSign
              .provideSomeLayer(ResolverTool.resolverLayer)
              .provideEnvironment(ZEnvironment(agent))
          )
        }
    }
    .observe(owner)

  // FIXME racing problem
  def curlCommand(owner: Owner) = encryptedMessageVar.signal
    .map(_.flatMap(_.toOption))
    .map(_.map { (_, eMsg) =>
      val program = for {
        resolver <- ZIO.service[Resolver]
        doc <- resolver.didDocument(TO(eMsg.recipientsSubject.head.string))
        mDIDServiceDIDCommMessaging = doc.getDIDServiceDIDCommMessaging.headOption
        mURI = mDIDServiceDIDCommMessaging.flatMap(_.getServiceEndpointAsURIs.headOption)
        ret = mURI match
          case None => curlCommandVar.set(None)
          case Some(uri) =>
            curlCommandVar.set(
              Some(s"""curl -X POST $uri -H 'content-type: application/didcomm-encrypted+json' -d '${eMsg.toJson}'""")
            )
      } yield (ret)

      Unsafe.unsafe { implicit unsafe => // Run side efect
        Runtime.default.unsafe.fork(
          program.provide(ResolverTool.resolverLayer)
        )
      }
    })
    .observe(owner)

  def curlProgram(msg: EncryptedMessage, plaintext: PlaintextMessage) = {
    val program = for {
      resolver <- ZIO.service[Resolver]
      doc <- resolver.didDocument(TO(msg.recipientsSubject.head.string))
      mDIDServiceDIDCommMessaging = doc.getDIDServiceDIDCommMessaging.headOption
      mURI = mDIDServiceDIDCommMessaging.flatMap(_.getServiceEndpointAsURIs.headOption)
      call <- mURI match
        case None => ZIO.unit
        case Some(uri) =>
          AgentMessageStorage.messageSend(
            msg,
            Global.agentVar.now().map(_.id.asFROM).getOrElse(???),
            plaintext
          ) // side efect
          Client
            .makeDIDCommPost(msg, uri)
            .map(_.fromJson[EncryptedMessage])
            .map {
              case Left(value)  => outputFromCallVar.set(None)
              case Right(value) => outputFromCallVar.set(Some(value))
            }
    } yield (call)
    Unsafe.unsafe { implicit unsafe => // Run side efect
      Runtime.default.unsafe.fork(
        program.provide(ResolverTool.resolverLayer)
      )
    }
  }

  val rootElement = div(
    onMountCallback { ctx =>
      callSignViaRPC(ctx.owner) // side effect
      callEncryptedViaRPC(ctx.owner) // side effect
      jobNextForward(ctx.owner) // side effect
      curlCommand(ctx.owner) // side effect
      ()
    },
    code("DecryptTool Page"),
    p(
      overflowWrap.:=("anywhere"),
      "Agent: ",
      " ",
      code(child.text <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse(Global.noneOption)))
    ),
    p(
      overflowWrap.:=("anywhere"),
      "Send TO (used by Templates): ",
      Global.makeSelectElementTO(Global.recipientVar),
      " ",
      code(child.text <-- Global.recipientVar.signal.map(_.map(_.toDID.string).getOrElse(Global.noneOption))),
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
              onClick --> Global.clipboardSideEffect(msg.toJson)
            )
          )
      }
    ),
    div(
      h2("Encrypted Message"),
      children <-- encryptedMessageVar.signal.map {
        case None              => Seq(code("None"))
        case Some(Left(error)) => Seq(code("Error when encrypting " + error.toJsonPretty))
        case Some(Right((_, eMsg))) =>
          Seq(
            h6(
              "(NOTE: This is executed as a RPC call to the JVM server, since the JS version has not yet been fully implemented)"
            ),
            pre(code(eMsg.toJsonPretty)),
            button(
              "Copy Encrypted Message to clipboard",
              onClick --> Global.clipboardSideEffect(eMsg.toJson)
            )
          )
      },
    ),
    div(
      h2("Sign Message"),
      children <-- signMessageVar.signal.map {
        case None              => Seq(code("None"))
        case Some(Left(error)) => Seq(code("Error when signing " + error.toJsonPretty))
        case Some(Right((_, sMsg))) =>
          Seq(
            h6("(NOTE: This is executed as a RPC call to the JVM server)"),
            pre(code(sMsg.toJsonPretty)),
            button(
              "Copy Sign Message to clipboard",
              onClick --> Global.clipboardSideEffect(sMsg.toJson)
            )
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
    p(code(child.text <-- curlCommandVar.signal.map(_.getOrElse("curl")))),
    div(
      child <-- curlCommandVar.signal
        .map {
          case Some(curlStr) =>
            div(
              button("Copy to curl", onClick --> Global.clipboardSideEffect(curlStr)),
              button(
                "Make HTTP POST",
                onClick --> Sink.jsCallbackToSink(_ =>
                  encryptedMessageVar.now() match {
                    case Some(Right((plaintext, eMsg))) => curlProgram(eMsg, plaintext)
                    case _                              => // None
                  }
                )
              )
            )
          case None => div("Valid message")
        }
    ),
    div(
      child <-- outputFromCallVar.signal.map {
        case None => new CommentNode("")
        case Some(reply) =>
          div(
            p("Output of the HTTP Call"),
            pre(code(reply.toJsonPretty)),
            button(
              "Copy reply to Decryot Tool",
              onClick --> { _ => DecryptTool.dataVar.set(reply.toJsonPretty) },
              MyRouter.navigateTo(MyRouter.DecryptPage)
            )
          )
      }
    ),
  )

  def apply(): HtmlElement = rootElement
}
