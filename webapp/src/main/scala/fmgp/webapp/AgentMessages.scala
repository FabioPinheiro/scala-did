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
import typings.std.stdStrings.childList

object AgentMessageStorage {

  val rootElement = div(
    onMountCallback { ctx =>
      // job(ctx.owner)
      ()
    },
    code("AgentMessageStorage"),
    p("Agent Message Storage of ", child <-- Global.agentVar.signal.map(_.map(_.id.string).getOrElse("NONE"))),
    // br(),
    div( // container
      padding := "12pt",
      // outgoingMgs(
      //   MsgID("TODO MSG TYPE"),
      //   Some(FROM("did:peer:123456789qwertyuiopasdfghjklzxcvbnm")),
      //   "TODO WIP",
      // ),
      children <-- Global.agentVar.signal.combineWith(Global.messageStorageVar.signal).map {
        case (None, messageStorage)        => Seq()
        case (Some(agent), messageStorage) =>
          messageStorage.storageItems.reverse.map {
            case StorageItem(msg, plaintext, from, to, timestamp) if from.contains(agent.id.asFROM) =>
              outgoingMgs(msg = msg, plaintext = plaintext, to = to)
            case StorageItem(msg, plaintext, from, to, timestamp) if to.contains(agent.id.asTO) =>
              incomeMgs(msg = msg, plaintext = plaintext, mFrom = from)
            case storageItem =>
              div()
          }
      }
    ),
  )

  def apply(): HtmlElement = rootElement

  def incomeMgs(
      msg: Option[SignedMessage | EncryptedMessage],
      plaintext: PlaintextMessage,
      mFrom: Option[FROM],
  ) = {
    val divFROM = mFrom match
      case None => div(overflowWrap.breakWord, "FROM: <unknown sender>") // overflow.hidden // textOverflow.ellipsis,
      case Some(from) =>
        val name = Global.providerNow.nameOfIdentity(from.toDID).getOrElse(from.value)
        div(overflowWrap.breakWord, "FROM: ", code(name))

    div(
      display.flex,
      div( // message row Left (income)
        backgroundColor := mFrom.map(e => Colors.fromSeed(e.value)).getOrElse("#00ffff66"), // aqua
        className := "mdc-card",
        maxWidth := "90%",
        margin := "4pt",
        padding := "8pt",
        display.flex,
        flexDirection.row,
        div(padding := "8pt", paddingBottom := "6pt", i(className("material-icons"), "face_6")), // avatar
        div(
          width := "90%",
          div("MsgID: ", code(plaintext.id.value)),
          divFROM,
          div(plaintext.`type`.value), // content
        ),
        title := plaintext.toJsonPretty,
      )
    )
  }

  def outgoingMgs(
      msg: Option[SignedMessage | EncryptedMessage],
      plaintext: PlaintextMessage,
      to: Set[TO]
  ) = {
    val divTO =
      if (to.isEmpty) div(overflowWrap.breakWord, "TO: nobody in specific")
      else {
        val names = to.map { v => Global.providerNow.nameOfIdentity(v.toDID).getOrElse(v.value) }.toSeq
        div(overflowWrap.breakWord, "TO: ", names.map(name => code(name + "; ")))
      }

    div(
      display.flex,
      flexDirection.rowReverse,
      div( // message row Right (outgoing)
        backgroundColor := to.headOption.map(e => Colors.fromSeed(e.value)).getOrElse("#bisque"),
        className := "mdc-card",
        maxWidth := "90%",
        margin := "4pt",
        padding := "8pt",
        display.flex,
        flexDirection.rowReverse,
        div(padding := "8pt", paddingBottom := "6pt", i(className("material-icons"), "face")), // avatar
        div(
          width := "90%",
          div("MsgID: ", code(plaintext.id.value)),
          divTO,
          div(plaintext.`type`.value),
          // div(
          //   className := "mdc-card__primary-action",
          //   tabIndex := 0,
          //   "action",
          //   div(className := "mdc-card__ripple")
          // ),
        ),
        title := plaintext.toJsonPretty,
      )
    )
  }
}
