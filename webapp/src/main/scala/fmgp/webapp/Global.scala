package fmgp.webapp

import scala.util._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.agent.MessageStorage
import fmgp.did.method.web.DIDWebResolver
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.method.prism.{HttpUtils, DIDPrismResolver}
import fmgp.did.method.hardcode.HardcodeResolver
import fmgp.did.uniresolver.Uniresolver
import fmgp.Utils

object Global {

  val urlForUniresolverVar = Var[String](initial = Uniresolver.defaultEndpoint)
  def urlForUniresolver = Global.urlForUniresolverVar.now() // TODO improve

  val baseUrlForDIDPrismResolverVar = // /FabioPinheiro/prism-vdr/
    Var[String](initial = "https://raw.githubusercontent.com/blockfrost/prism-vdr/refs/heads/main/mainnet/diddoc")

  def baseUrlForDIDPrismResolver = Global.baseUrlForDIDPrismResolverVar.now() // TODO improve

  def resolverLayer: ZLayer[Any, Nothing, MultiFallbackResolver] =
    (
      ZLayer.empty
        ++ Uniresolver.layerUniresolver(urlForUniresolver)
        ++ (HttpUtils.layer >>> DIDPrismResolver.layerDIDPrismResolver(baseUrlForDIDPrismResolver))
    ) >>> ZLayer.fromZIO(makeResolver)

  def makeResolver: ZIO[DIDPrismResolver & Uniresolver, Nothing, MultiFallbackResolver] = for {
    // FIX -> has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource
    uniresolver <- ZIO.service[Uniresolver]
    didPrismResolver <- ZIO.service[DIDPrismResolver]
    multiResolver = MultiFallbackResolver(
      HardcodeResolver.default,
      DidPeerResolver.default,
      didPrismResolver,
      DIDWebResolver.default,
      uniresolver,
    )
  } yield multiResolver

  val agentProvider = Var(initial = AgentProvider.provider)
  def providerNow = agentProvider.now()

  // TODO rename to transportTimeFrame
  val transportTimeoutVar = Var[Int](initial = 10)
  val localKeyGeneratorVar = Var[Boolean](initial = false)

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

  val messageStorageVar = Var[MessageStorage](initial = MessageStorage.empty)

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

  // ### Notifications ###
  @JSExport
  val valuePushSubscriptionVar = Var[Option[org.scalajs.dom.PushSubscription]](initial = None)

  // {
  //   "endpoint":"https://fcm.googleapis.com/fcm/send/d_OCCj-5sA8:APA91bE0x8qcQXj35JdU8zZFFha8ZiBYLlDw10PDxwoiseB9IIB3Owws_xaOkm6cJWa0NwZQcW6hN7HwU_MBAFeLIFOPbBJ4VTajrBARu7zsXPTUHAh-lng9WDyAz5C33u6PHPfpwWnR",
  //   "expirationTime":null,
  //   "keys":{
  //     "p256dh":"BDpHL0b3paDBBdoALWIoBw5OoiQ7tPW3ZxK1BfwcMNYF3cFoBec8QYMZCkBdwX6AW_Up7QI6_BSddm8Byml0KtE",
  //     "auth":"cStDqeDeHH4Pa9I38t6Zlw"
  //   }
  // }
}
