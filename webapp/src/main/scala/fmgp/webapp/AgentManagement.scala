package fmgp.webapp

import org.scalajs.dom
import com.raquo.laminar.api.L._
import scala.scalajs.js
import scala.scalajs.js.timers._
import scala.scalajs.js.JSConverters._

import zio._
import zio.json._

import fmgp.Utils
import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did.comm._
import fmgp.did.comm.protocol.mediatorcoordination2
import fmgp.did.comm.protocol.mediatorcoordination3
import fmgp.did.comm.protocol.pickup3._
import fmgp.did.method.peer._
import fmgp.did.AgentProvider.AgentWithShortName

object AgentManagement {

  val providerTextVar: Var[String] = Var(initial =
    AgentProvider( // AgentProvider.provider
      Seq(AgentWithShortName("alice", AgentProvider.alice)),
      Seq(AgentProvider.DIDWithShortName("exampleAlice", AgentProvider.exampleAlice.id))
    ).toJsonPretty
  )
  def maybeAgentProviderSignal = providerTextVar.signal.map(_.fromJson[AgentProvider])

  def nameVar = Global.agentVar.signal.map {
    case None        => "none"
    case Some(agent) => agent.id
  }

  def keyStoreVar = Global.agentVar.signal.map {
    case None        => KeyStore(Set.empty)
    case Some(agent) => agent.keyStore
  }
  def childrenSignal: Signal[Seq[Node]] = keyStoreVar.map(_.keys.toSeq.map(_.toJson).map(code(_)))

  val mediatorDIDVar: Var[Option[DID]] = Var(initial = None)
  val keyStore2Var: Var[KeyStore] = Var(initial = KeyStore(Set.empty))

  private val commandObserver = Observer[String] { case str =>
    str.fromJson[PrivateKey] match
      case Left(error)   => dom.window.alert(s"Fail to parse key: $error")
      case Right(newKey) => keyStore2Var.update(ks => ks.copy(ks.keys + newKey))
  }

  val urlEndpoint = input(
    placeholder("Peer DID's service endpoint"),
    `type`.:=("textbox"),
    autoFocus(true),
    value := "https://sit-prism-mediator.atalaprism.io",
  )

  def newPeerDID(service: Option[DIDPeerServiceEncoded], localKeys: Boolean) = {
    val programe = ZIO
      .collectAllPar(
        if (localKeys) Seq(Client.newKeyX25519Local, Client.newKeyEd25519Local)
        else Seq(Client.newKeyX25519Remote, Client.newKeyEd25519Remote)
      )
      .map { case Seq(x25519, ed255199) =>
        val newAgent = DIDPeer2.makeAgent(Seq(x25519, ed255199), service.toSeq)
        Global.agentProvider.update(e => e.withAgent(AgentWithShortName("Agent" + e.agents.size, newAgent)))
        Global.selectAgentDID(newAgent.id)
      }
    // .tap(_ => ZIO.succeed(urlEndpoint.ref.value = "")) // clean up
    Utils.runProgram(programe)
  }

  object V2 {
    import mediatorcoordination2._
    def mediateRequest(msg: => MediateRequest) = Utils
      .sendAndReceiveProgram(msg.toPlaintextMessage)
      .map { response =>
        response.toMediateGrantOrDeny match
          case Left(value)            => println(s"ERROR: $value")
          case Right(m: MediateGrant) => println(MediateGrant)
          case Right(m: MediateDeny)  => println(MediateDeny)
      }

    def recipientUpdate(msg: => KeylistUpdate) = Utils
      .sendAndReceiveProgram(msg.toPlaintextMessage)
      .map { response =>
        response.toKeylistResponse match
          case Left(value)               => println(s"ERROR: $value")
          case Right(m: KeylistResponse) => println(value)
      }
  }

  object V3 {
    import mediatorcoordination3._
    def mediateRequest(msg: => MediateRequest) = Utils
      .sendAndReceiveProgram(msg.toPlaintextMessage)
      .map(_.toMediateGrantOrDeny match {
        case Left(value)            => println(s"ERROR: $value")
        case Right(m: MediateGrant) => println(m)
        case Right(m: MediateDeny)  => println(m)
      })

    def recipientUpdate(msg: => RecipientUpdate) = Utils
      .sendAndReceiveProgram(msg.toPlaintextMessage)
      .map(_.toRecipientResponse match {
        case Left(value)                 => println(s"ERROR: $value")
        case Right(m: RecipientResponse) => println(value) // TODO
      })
  }

  val rootElement = div(
    onMountCallback { ctx =>
      Global.agentProvider.signal
        .map(provider => providerTextVar.set(provider.toJsonPretty))
        .observe(ctx.owner) // side effect
      ()
    },
    code("Agent Management"),
    h2("All Agents and Identities"),
    table(
      tr(
        th("Short Name"),
        th("#Keys"),
        th("DID"),
      ),
      children <-- Global.agentProvider.signal.map(provider =>
        provider.agentsAndIdentities.map {
          case element: AgentProvider.AgentWithShortName =>
            tr(
              td(
                button(
                  code(element.name),
                  onClick --> { (ev) => Global.selectAgentByName(element.name) }
                )
              ),
              td(code(element.value.keyStore.keys.size)),
              td(a(element.value.id.did, MyRouter.navigateTo(MyRouter.ResolverPage(element.value.id.did)))),
            )
          case element: AgentProvider.DIDWithShortName =>
            tr(
              td(
                button(
                  code(element.name),
                  onClick --> { (ev) => Global.selectAgentByName(element.name) }
                )
              ),
              td(code("N/A")),
              td(AppUtils.linkToResolveDID(element.value)),
            )
        }
      ),
    ),
    hr(),
    p(
      overflowWrap.:=("anywhere"),
      b("Selected Mediator: "),
      Global.makeSelectElementDID(mediatorDIDVar),
      " ",
      code(child.text <-- mediatorDIDVar.signal.map(_.map(_.string).getOrElse("custom")))
    ),
    div(
      b("Create Agent: "),
      child <-- mediatorDIDVar.signal.combineWith(Global.localKeyGeneratorVar.signal).map {
        case (None, useLocalkeyGenerator) =>
          div(
            button(
              "New DID Peer",
              onClick --> { (_) =>
                newPeerDID(
                  Some(urlEndpoint.ref.value.trim)
                    .filterNot(_.isEmpty)
                    .map(endpoint => DIDPeerServiceEncoded.fromEndpoint(endpoint)),
                  useLocalkeyGenerator
                )
              }
            ),
            " with the follow endpoint ",
            urlEndpoint
          )
        case (Some(mediatorDID), useLocalkeyGenerator) =>
          div(
            button(
              "New DID Peer",
              onClick --> { (_) =>
                newPeerDID(Some(DIDPeerServiceEncoded.fromEndpoint(mediatorDID.did)), useLocalkeyGenerator)
              }
            ),
            " with the follow endpoint ",
            code(mediatorDID.did)
          )
      }
    ),
    hr(),
    h2("Selected Agent"),
    div(
      child <-- Global.agentVar.signal.map {
        case None        => "none"
        case Some(agent) => div(code(agent.id.string))
      }
    ),
    div(
      child <-- Global.agentVar.signal.combineWith(mediatorDIDVar.signal).map {
        case (Some(agent), Some(mediatorDID)) =>
          val env = ZEnvironment(agent, DidPeerResolver())
          val (mrV2, mrpV2, ruV2, rupV2) = {
            import mediatorcoordination2._
            import V2._
            def mr = MediateRequest(from = agent.id, to = mediatorDID)
            def mrp = mediateRequest(mr).provideEnvironment(env)
            def ru = KeylistUpdate(
              from = agent.id,
              to = mediatorDID,
              updates = Seq((agent.id.asFROMTO, mediatorcoordination2.KeylistAction.add))
            )
            def rup = recipientUpdate(ru).provideEnvironment(env)
            (mr, mrp, ru, rup)
          }
          val (mrV3, mrpV3, ruV3, rupV3) = {
            import mediatorcoordination3._
            import V3._
            def mr = MediateRequest(from = agent.id, to = mediatorDID)
            def mrp = mediateRequest(mr).provideEnvironment(env)
            def ru = RecipientUpdate(
              from = agent.id,
              to = mediatorDID,
              updates = Seq((agent.id.asFROMTO, RecipientAction.add))
            )
            def rup = recipientUpdate(ru).provideEnvironment(env)
            (mr, mrp, ru, rup)
          }
          val pickupMsg = DeliveryRequest(from = agent.id, to = mediatorDID, limit = 100, recipient_did = None)
          val pickupStatus = StatusRequest(from = agent.id, to = mediatorDID, recipient_did = None)

          def pickupProgram(msg: DeliveryRequest) = Utils
            .sendAndReceiveProgram(msg.toPlaintextMessage)
            .map(_.toMessageDelivery match {
              case Left(value)               => println(s"ERROR: $value")
              case Right(m: MessageDelivery) => println(value) // TODO
            })
            .provideEnvironment(env)

          def pickupStatusProgram(msg: StatusRequest) = Utils
            .sendAndReceiveProgram(msg.toPlaintextMessage)
            .map(_.toStatus match {
              case Left(value)      => println(s"ERROR: $value")
              case Right(m: Status) => println(value) // TODO
            })
            .provideEnvironment(env)

          div(
            div(
              button(
                title := mrV2.toPlaintextMessage.toJsonPretty,
                "Send Mediate Request v2",
                onClick --> { (_) => Utils.runProgram(mrpV2) }
              ),
              button(
                title := ruV2.toPlaintextMessage.toJsonPretty,
                "Send Recipient Update v2 (self add as recipient)",
                onClick --> { (_) => Utils.runProgram(rupV2) }
              ),
            ),
            div(
              button(
                title := mrV3.toPlaintextMessage.toJsonPretty,
                "Send Mediate Request v3",
                onClick --> { (_) => Utils.runProgram(mrpV2) }
              ),
              button(
                title := ruV3.toPlaintextMessage.toJsonPretty,
                "Send Mediate Request v3 (self add as recipient)",
                onClick --> { (_) => Utils.runProgram(rupV3) }
              ),
            ),
            div(
              button(
                title := pickupMsg.toPlaintextMessage.toJsonPretty,
                "Delivery Request",
                onClick --> { (_) => Utils.runProgram(pickupProgram(pickupMsg)) }
              ),
              button(
                title := pickupStatus.toPlaintextMessage.toJsonPretty,
                "Status Request",
                onClick --> { (_) => Utils.runProgram(pickupStatusProgram(pickupStatus)) }
              ),
            ),
          )

        case _ => ""
      }
    ),
    div(
      div(child.text <-- keyStoreVar.map(_.keys.size).map(c => s"KeyStore (with $c keys):")),
      div(children <-- childrenSignal)
    ),
    /*
    table(
      tr(th("type"), th("isPointOnCurve"), th("Keys Id")),
      children <-- keyStoreVar.map(
        _.keys.toSeq
          .map { key =>
            key match
              case k @ OKPPrivateKey(kty, crv, d, x, kid) =>
                tr(
                  td(code(kty.toString)),
                  td(code("N/A")),
                  td(code(kid.getOrElse("missing"))),
                )
              case k @ ECPrivateKey(kty, crv, d, x, y, kid) =>
                tr(
                  td(code(kty.toString)),
                  td(code(k.isPointOnCurve)),
                  td(code(kid.getOrElse("missing"))),
                )
          }
      ),
    ),
     */
    div(
      h2("KeyStore:"),
      child <-- keyStoreVar.map(keyStore => pre(code(keyStore.keys.toJsonPretty)))
    ),
    div(
      h2("Edit Agents"),
      div(
        button(
          "Clear AgentProvider",
          onClick --> { _ => Global.agentProvider.set(AgentProvider(Seq.empty, Seq.empty)) }
        ),
        button(
          "Reset default AgentProvider",
          onClick --> { _ => Global.agentProvider.set(AgentProvider.provider) }
        ),
        child <-- maybeAgentProviderSignal.map {
          case Left(error) => pre(code(s"Can't import AgentProvider because: $error"))
          case Right(newProvider) =>
            button("Import AgentProvider", onClick --> { _ => Global.agentProvider.set(newProvider) })
        },
        child <-- Global.agentProvider.signal.map { agentProvider =>
          button(
            "Export",
            onClick --> { _ =>
              {
                val blobParts: js.Iterable[String] = Seq(agentProvider.toJsonPretty).toJSIterable
                val options = js.Dictionary("type" -> "application/json").asInstanceOf[dom.BlobPropertyBag]
                val file = dom.Blob(blobParts, options)
                val url = dom.URL.createObjectURL(file)
                val tmp = a(href := url, download := "AgentProvider.export.json")
                tmp.ref.click()
                dom.URL.revokeObjectURL(url)
              }
            }
          )
        },
        div(
          "Import AgentProvider from file: ",
          input(
            `type` := "file",
            onChange.mapToFiles --> {
              _.head
                .text()
                .`then`(data =>
                  data.fromJson[AgentProvider] match {
                    case Left(error)        => println(s"Fail to import file due to: $error") // TODO Error log
                    case Right(newProvider) => Global.agentProvider.set(newProvider)
                  }
                )
            }
            // onInput --> { e => println(e.asInstanceOf[org.scalajs.dom.InputEvent].data) }
            // inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> println("11111") }
          )
        )
      ),
      div(
        textArea(
          rows := 40,
          cols := 80,
          autoFocus(true),
          value <-- providerTextVar,
          inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> providerTextVar }
        ),
      ),
    ),
  )
  def apply(): HtmlElement = rootElement
}
