package fmgp.webapp

import org.scalajs.dom
import com.raquo.laminar.api.L._
import scala.scalajs.js
import scala.scalajs.js.timers._
import scala.scalajs.js.JSConverters._

import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did.method.peer._
import fmgp.did.AgentProvider.AgentWithShortName

object AgentManagement {

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

  def newPeerDID(service: Option[DIDPeerServiceEncoded]) = {
    val programe = ZIO
      .collectAllPar(
        Seq(
          Client.newKeyX25519,
          Client.newKeyEd255199
        )
      )
      .map { case Seq(x25519, ed255199) =>
        val newAgent = DIDPeer2.makeAgent(Seq(x25519, ed255199), service.toSeq)
        Global.agentProvider.update(e => e.withAgent(AgentWithShortName("Agent" + e.agents.size, newAgent)))
      }
    // .tap(_ => ZIO.succeed(urlEndpoint.ref.value = "")) // clean up

    Unsafe.unsafe { implicit unsafe => // Run side efect
      Runtime.default.unsafe.runToFuture(
        programe.mapError(DidException(_))
      )
    }
  }

  val rootElement = div(
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
              td(code(element.value.keys.size)),
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
              td(a(element.value.did, MyRouter.navigateTo(MyRouter.ResolverPage(element.value.did)))),
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
      button(
        "New DID Peer",
        onClick --> { (_) =>
          val services =
            Some(urlEndpoint.ref.value.trim).filterNot(_.isEmpty).map(endpoint => DIDPeerServiceEncoded(s = endpoint))
          newPeerDID(services)
        }
      ),
      " with the follow endpoint ",
      child <-- mediatorDIDVar.signal.map {
        case None              => urlEndpoint
        case Some(mediatorDID) => code(mediatorDID.did)
      }
    ),
    hr(),
    h2("Selected Agent: "),
    div(
      child <-- Global.agentVar.signal.map {
        case None        => "none"
        case Some(agent) => code(agent.id.string)
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
    )
  )
  def apply(): HtmlElement = rootElement
}
