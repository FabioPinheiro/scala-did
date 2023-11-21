package fmgp.webapp

import scala.util._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.method.peer.DIDPeer
import fmgp.did.agent.MessageStorage
import fmgp.crypto.error._
import fmgp.Utils

object Global {

  val resolverLayer = ZLayer.succeed(
    MultiResolver(
      fmgp.did.method.hardcode.HardcodeResolver.default,
      // Uniresolver.default, //FIX -> has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource
      fmgp.did.method.peer.DidPeerResolver.default,
    )
  )

  val agentProvider = Var(initial = AgentProvider.provider)
  def providerNow = agentProvider.now()

  // TODO rename to transportTimeFrame
  val transportTimeoutVar = Var[Int](initial = 10)

  /** Agent in use */
  val agentVar = Var[Option[Agent]](initial = None)
  val recipientVar = Var[Option[TO]](initial = None)

  val noneOption = "<none>"
  val dids = agentProvider.signal.map(_.agetnsNames.sorted)
  val didsTO = agentProvider.signal.map(_.identitiesNames.sorted)

  def selectAgentByName(didName: String) = Global.agentVar.update(e => Global.providerNow.getAgentByName(didName))
  def selectAgentDID(subject: DIDSubject) = Global.agentVar.update(e => Global.providerNow.getAgentByDID(subject))

  def selectAgent: Signal[String] = Global.agentVar.signal.combineWith(agentProvider.signal).map {
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

  /** side effect of writing the text to the clipboard */
  def copyToClipboard(text: => String): Unit =
    dom.window.navigator.clipboard.writeText(text) // side effect

  @JSExport
  def update(htmlPath: String) = {
    println("MermaidApp Update!!")
    val config = typings.mermaid.configTypeMod.MermaidConfig()
    typings.mermaid.mod.default.init(config, htmlPath)
  }

  val messageStorageVar = Var[MessageStorage](initial = MessageStorage.example)

  /** Store in BD */
  def messageSend(msg: SignedMessage | EncryptedMessage, from: FROM, plaintext: PlaintextMessage) =
    messageStorageVar.tryUpdate {
      case Success(messageStorage) =>
        Success(messageStorage.messageSend(msg, from, plaintext))
      case Failure(exception) =>
        Failure(exception)
    }

  /** Store in BD */
  def messageRecive(msg: SignedMessage | EncryptedMessage, plaintext: PlaintextMessage) =
    messageStorageVar.tryUpdate {
      case Success(messageStorage) =>
        Success(messageStorage.messageRecive(msg, plaintext))
      case Failure(exception) =>
        Failure(exception)
    }

  def tryDecryptVerifyReciveMessagePrograme(msg: SignedMessage | EncryptedMessage): ZIO[Resolver, DidFail, Unit] =
    for {
      _ <- ZIO.log("")
      tmpAgentProvider = agentProvider.now()
      jobs = msg match
        case sMsg: SignedMessage =>
          Seq(
            Utils
              .verifyProgram(sMsg)
              .map(e => Global.messageRecive(e._1, e._2)) // side effect!
          )
        case eMsg: EncryptedMessage =>
          eMsg.recipientsSubject.toSeq.map { did =>
            tmpAgentProvider.getAgentByDID(did) match
              case None => ZIO.unit
              case Some(agent) =>
                Utils
                  .decryptProgram(eMsg)
                  .map(e => Global.messageRecive(e._1, e._2)) // side effect!
                  .provideSomeEnvironment((e: ZEnvironment[Resolver]) => e ++ ZEnvironment(agent))
          }
      job <- ZIO.foreachDiscard(jobs)(e => e)
    } yield () // Utils.runProgram(program.provideSomeLayer(Global.resolverLayer))

}
