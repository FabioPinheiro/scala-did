package fmgp.webapp

import scala.scalajs.js
import scala.scalajs.js.timers._
import scala.scalajs.js.JSConverters._
import org.scalajs.dom
import com.raquo.laminar.api.L._

import zio._
import zio.json._
import zio.stream.ZStream

import fmgp._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.framework.TransportWSImp

// TODO REMOVE TapIntoStreamTool
object TapIntoStreamTool {

  val messages = Var[Seq[String]](Seq.empty)

  val namesAndDIDs: Seq[(String, DIDSubject)] = Seq( // FIXME
    /*
    ("alice", fmgp.did.AgentProvider.alice.id.asDIDSubject),
    ("bob", fmgp.did.AgentProvider.bob.id.asDIDSubject),
    ("exampleAlice", fmgp.did.AgentProvider.exampleAlice.id.asDIDSubject),
    ("exampleBob", fmgp.did.AgentProvider.exampleBob.id.asDIDSubject),
    ("localhost8080AliceHttp", fmgp.did.AgentProvider.localhost8080AliceHttp.id.asDIDSubject),
     */
  )

  // val ws = WebsocketJSLive.autoReconnect
  def transmissionProgram(
      subjects: Seq[DIDSubject] = namesAndDIDs.map(_._2)
  ) = for {
    _ <- ZIO.log("TransportWS")
    _ = messages.set(Seq.empty) // side effect!
    transport <- ZIO.service[TransportWSImp[String]]
    _ <- transport.send("info")
    _ <- ZIO.foreach(subjects)(s => transport.send(s"link ${s.did}"))
    fiber <- transport.inbound
      .tap(out => ZIO.succeed(messages.update(out +: _))) // side effect!
      .mapZIO(e =>
        e.fromJson[Message] match
          case Left(value)                   => ???
          case Right(eMsg: EncryptedMessage) => Global.tryDecryptVerifyReciveMessagePrograme(eMsg)
          case Right(sMsg: SignedMessage)    => Global.tryDecryptVerifyReciveMessagePrograme(sMsg)
          case Right(pMsg: PlaintextMessage) => ???
      )
      .runCount
      .fork
    _ <- ZIO.logDebug(s"TransportWS JOB started for ${subjects.map(_.did)}")
    _ <- fiber.join.flatMap(l => ZIO.log(s"TransportWS END after $l events"))
  } yield ()

  val rootElement = div(
    onMountCallback { ctx =>
      // job(ctx.owner)
      Utils.runProgram(transmissionProgram().provide(TransportWSImp.layer ++ Scope.default ++ Global.resolverLayer))
      ()
    },
    code("TapIntoStream Tool"),
    p(s"Tap into [${namesAndDIDs.map(_._1).mkString(", ")}] DIDs stream: "),
    br(),
    div( // container
      padding := "12pt",
      // outgoingMgs(
      //   MsgID("TODO MSG TYPE"),
      //   Some(FROM("did:peer:123456789qwertyuiopasdfghjklzxcvbnm")),
      //   "TODO WIP",
      // ),
      children <-- messages.signal // split(_.blockId)
        .map(_.map { data =>
          data.fromJson[EncryptedMessage] match
            case Left(value) => p(s"Not EncryptedMessage (value): $data")
            case Right(eMsg) =>
              val mFrom = eMsg.`protected`.obj.match
                case AnonProtectedHeader(epk, apv, typ, enc, alg)            => None
                case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) => Some(skid.did.asFROM)
              incomeMgs(MsgID(eMsg.sha256), mFrom, eMsg.toJsonPretty)
        })
      // _.map(_.decrypted).map { msg =>
      // BasicMessage.fromPlaintextMessage(msg) match
      //   case Left(ex)  => incomeMgs(msg.id, msg.from, msg.`type`.value)
      //   case Right(bm) => incomeMgs(msg.id, msg.from, bm.content)

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
        div(msgId.value),
        div(did.map(_.value).getOrElse("")),
        div(pre(code(content))),
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
        div(msgId.value),
        div(did.map(_.value).getOrElse("")),
        div(pre(code(content))),
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
