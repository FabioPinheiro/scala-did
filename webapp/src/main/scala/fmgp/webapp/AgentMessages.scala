package fmgp.webapp

import scala.scalajs.js
import scala.scalajs.js.timers._
import scala.scalajs.js.JSConverters._
import org.scalajs.dom
import com.raquo.laminar.api.L._

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.method.peer.DIDPeer
import fmgp.did.agent._

object AgentMessageStorage {

  val messageStorageVar = Var[MessageStorage](initial = MessageStorage.example)

  def messageSend(msg: EncryptedMessage, from: FROM, plaintext: PlaintextMessage) =
    messageStorageVar.update(_.messageSend(msg, from, plaintext))
  def messageRecive(msg: EncryptedMessage, plaintext: PlaintextMessage) =
    messageStorageVar.update(_.messageRecive(msg, plaintext))

  val rootElement = div(
    onMountCallback { ctx =>
      // job(ctx.owner)
      ()
    },
    code("AgentMessageStorage"),
    p("Agent Message Storage of ", child <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse("NONE"))),
    br(),
    div( // container
      padding := "12pt",
      // outgoingMgs(
      //   MsgID("TODO MSG TYPE"),
      //   Some(FROM("did:peer:123456789qwertyuiopasdfghjklzxcvbnm")),
      //   "TODO WIP",
      // ),
      children <-- Global.agentVar.signal.combineWith(messageStorageVar.signal).map {
        case (None, messageStorage) => Seq()
        case (Some(agent), messageStorage) =>
          messageStorage.storageItems.reverse.map {
            case StorageItem(msg, plaintext, from, to, timestamp) if from.contains(agent.id.asFROM) =>
              outgoingMgs(
                msgId = plaintext.id,
                did = from,
                content = plaintext.`type`.value
              )
            case StorageItem(msg, plaintext, from, to, timestamp) if to.contains(agent.id.asTO) =>
              incomeMgs(
                msgId = plaintext.id,
                did = from,
                content = plaintext.`type`.value
              )
            case storageItem =>
              div()
          }
      }
    ),
  )

  def apply(): HtmlElement = rootElement

  def incomeMgs(msgId: MsgID, did: Option[FROM], content: String) = div(
    display.flex,
    div( // message row Left (income)
      className := "mdc-card",
      maxWidth := "90%",
      margin := "4pt",
      padding := "8pt",
      display.flex,
      flexDirection.row,
      div(padding := "8pt", paddingBottom := "6pt", i(className("material-icons"), "face")), // avatar
      div(
        width := "90%",
        div(" - ", code(msgId.value)),
        div(overflowWrap.breakWord, did.map(_.value).getOrElse("")),
        div(content),
      )
    )
  )

  def outgoingMgs(msgId: MsgID, did: Option[FROM], content: String) = div(
    display.flex,
    flexDirection.rowReverse,
    div( // message row Right (outgoing)
      className := "mdc-card",
      maxWidth := "90%",
      margin := "4pt",
      padding := "8pt",
      display.flex,
      flexDirection.rowReverse,
      div(padding := "8pt", paddingBottom := "6pt", i(className("material-icons"), "face")), // avatar
      div(
        width := "90%",
        div(" - ", code(msgId.value)),
        div(overflowWrap.breakWord, code(did.map(_.value).getOrElse(""))),
        div(content),
        // div(
        //   className := "mdc-card__primary-action",
        //   tabIndex := 0,
        //   "action",
        //   div(className := "mdc-card__ripple")
        // ),
      )
    )
  )
}
