package fmgp.webapp

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import com.raquo.laminar.api.L._

import fmgp.did._
import fmgp.did.method.peer.DIDPeer
import fmgp.did.comm.TO

object Global {

  val agentProvider = Var(initial = AgentProvider.provider)
  def providerNow = agentProvider.now()

  /** Agent in use */
  val agentVar = Var[Option[Agent]](initial = None)
  val recipientVar = Var[Option[TO]](initial = None)

  val dids = agentProvider.signal.map(_.agetnsNames.sorted :+ "<none>")
  val didsTO = agentProvider.signal.map(_.identitiesNames.sorted :+ "<none>")

  def agent2Host: Option[String] = {
    Global.agentVar
      .now() // TODO REMOVE .now()
      .flatMap(agent =>
        agentProvider.now().nameOfAgent(agent.id) match { // TODO REMOVE .now()
          case Some("alice")   => Some("alice.did.fmgp.app")
          case Some("bob")     => Some("bob.did.fmgp.app")
          case Some("charlie") => Some("charlie.did.fmgp.app")
          case _               => None
        }
      )
  }

  def selectAgent: Signal[String] = Global.agentVar.signal.combineWith(agentProvider.signal).map {
    case (Some(agent), agentProvider) => agentProvider.nameOfAgent(agent.id).getOrElse("<none>")
    case (None, agentProvider)        => "<none>"
  }

  def getIdentitiesName(mDID: Option[DID], agentProvider: AgentProvider): String =
    mDID.flatMap(did => agentProvider.nameOfIdentity(did)).getOrElse("<none>")

  def makeSelectElementDID(didVar: Var[Option[DID]]) = select(
    value <-- didVar.signal
      .combineWith(agentProvider.signal)
      .map { (mDID, agentProvider) => getIdentitiesName(mDID, agentProvider) },
    onChange.mapToValue.map(e => providerNow.getDIDByName(e)) --> didVar,
    children <-- Global.didsTO.map(_.map { step => option(value := step, step) })
  )
  def makeSelectElementTO(didVar: Var[Option[TO]]) = select(
    value <-- didVar.signal
      .combineWith(agentProvider.signal)
      .map { (mDID, agentProvider) => getIdentitiesName(mDID.map(_.toDID), agentProvider) },
    onChange.mapToValue.map(e => providerNow.getDIDByName(e)).map(_.map(e => e: TO)) --> didVar,
    children <-- Global.didsTO.map(_.map { step => option(value := step, step) })
  )

  def clipboardSideEffect(text: => String): Any => Unit =
    (_: Any) => { dom.window.navigator.clipboard.writeText(text) }

  @JSExport
  def update(htmlPath: String) = {
    println("MermaidApp Update!!")
    val config = typings.mermaid.configTypeMod.MermaidConfig()
    typings.mermaid.mod.default.init(config, htmlPath)
  }
}
