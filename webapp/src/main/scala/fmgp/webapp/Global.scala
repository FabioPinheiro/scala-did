package fmgp.webapp

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import com.raquo.laminar.api.L._
import zio._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.method.peer.DIDPeer
import fmgp.did.agent.MessageStorage
import fmgp.crypto.error.DidFail
import scala.util.Try
import scala.util.Failure
import scala.util.Success

object Global {

  def runProgram(program: ZIO[Any, DidFail, Unit]) = Unsafe.unsafe { implicit unsafe => // Run side efect
    Runtime.default.unsafe.fork(
      program
    )
  }

  val agentProvider = Var(initial = AgentProvider.provider)
  def providerNow = agentProvider.now()

  /** Agent in use */
  val agentVar = Var[Option[Agent]](initial = None)
  val recipientVar = Var[Option[TO]](initial = None)

  val noneOption = "<none>"
  val dids = agentProvider.signal.map(_.agetnsNames.sorted)
  val didsTO = agentProvider.signal.map(_.identitiesNames.sorted)

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

  def selectAgentByName(didName: String) = Global.agentVar.update(e => Global.providerNow.getAgentByName(didName))

  def selectAgented: Signal[String] = Global.agentVar.signal.combineWith(agentProvider.signal).map {
    case (Some(agent), agentProvider) => agentProvider.nameOfAgent(agent.id).getOrElse(noneOption)
    case (None, agentProvider)        => noneOption
  }

  def getIdentitiesName(mDID: Option[DID], agentProvider: AgentProvider): String =
    mDID.flatMap(did => agentProvider.nameOfIdentity(did)).getOrElse(noneOption)

  def makeSelectElementDID(didVar: Var[Option[DID]]) = select(
    value <-- didVar.signal
      .combineWith(agentProvider.signal)
      .map { (mDID, agentProvider) => getIdentitiesName(mDID, agentProvider) },
    onChange.mapToValue.map(e => providerNow.getDIDByName(e)) --> didVar,
    children <-- Global.didsTO.map {
      _.map { step => option(value := step, step) } :+ option(value := noneOption, selected := true, noneOption)
    }
  )
  def makeSelectElementTO(didVar: Var[Option[TO]]) = select(
    value <-- didVar.signal
      .combineWith(agentProvider.signal)
      .map { (mDID, agentProvider) => getIdentitiesName(mDID.map(_.toDID), agentProvider) },
    onChange.mapToValue.map(e => providerNow.getDIDByName(e)).map(_.map(e => e: TO)) --> didVar,
    children <-- Global.didsTO.map {
      _.map { step => option(value := step, step) } :+ option(value := noneOption, selected := true, noneOption)
    }
  )

  def clipboardSideEffect(text: => String): Any => Unit =
    (_: Any) => { dom.window.navigator.clipboard.writeText(text) }

  @JSExport
  def update(htmlPath: String) = {
    println("MermaidApp Update!!")
    val config = typings.mermaid.configTypeMod.MermaidConfig()
    typings.mermaid.mod.default.init(config, htmlPath)
  }

  val messageStorageVar = Var[MessageStorage](initial = MessageStorage.example)

  def messageSend(msg: SignedMessage | EncryptedMessage, from: FROM, plaintext: PlaintextMessage) =
    messageStorageVar.tryUpdate {
      case Success(messageStorage) =>
        println(s"DEBUG: Store messageSend id: ${plaintext.id}")
        Success(messageStorage.messageSend(msg, from, plaintext))
      case Failure(exception) =>
        println(s"DEBUG: Store messageSend id: ${plaintext.id} FAIL: ${exception}")
        Failure(exception)
    }
  def messageRecive(msg: SignedMessage | EncryptedMessage, plaintext: PlaintextMessage) =
    messageStorageVar.tryUpdate {
      case Success(messageStorage) =>
        println(s"DEBUG: Store messageRecive id: ${plaintext.id}")
        Success(messageStorage.messageRecive(msg, plaintext))
      case Failure(exception) =>
        println(s"DEBUG: Store messageRecive id: ${plaintext.id} FAIL: ${exception}")
        Failure(exception)
    }

}
