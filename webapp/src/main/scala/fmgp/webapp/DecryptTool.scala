package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers.*
import js.JSConverters.*

import com.raquo.laminar.api.L.*
import com.raquo.airstream.ownership.*
import zio.*
import zio.json.*

import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.method.peer.DIDPeer.*
import fmgp.crypto.error.*

import fmgp.did.AgentProvider
import fmgp.Utils

object DecryptTool {
  val dataVar: Var[String] = Var(initial = "")
  val messageVar: Signal[Either[String, SignedOrEncryptedMessage]] =
    dataVar.signal.map(_.fromJson[SignedOrEncryptedMessage](SignedOrEncryptedMessage.decoder))
  val decryptDataVar: Var[Option[Array[Byte]]] = Var(initial = None)
  val decryptMessageVar: Var[Option[Either[DidFail, Message]]] = Var(initial = None)

  def job(owner: Owner) =
    Signal
      .combine(
        Global.agentVar,
        messageVar
      )
      .map {
        case (_, Right(sMsg: SignedMessage)) =>
          val program = OperationsClientRPC
            .verify2PlaintextMessage(sMsg)
            .flatMap { plaintext =>
              ZIO.succeed(decryptDataVar.set(Some(sMsg.payload.content.getBytes))) *> // side effect!
                ZIO.succeed(decryptMessageVar.set(Some(Right(plaintext)))) *> // side effect!
                ZIO.succeed(Global.messageRecive(sMsg, plaintext)) // side effect!
            }
          Utils.runProgram(program.provideSomeLayer(Global.resolverLayer))
        case (None, _) =>
          decryptDataVar.set(None) // side effect!
          decryptMessageVar.set(None) // side effect!
        case (Some(agent), Left(error)) =>
          decryptDataVar.set(None) // side effect!
          decryptMessageVar.set(Some(Left(FailToParse("Fail to parse Message: " + error)))) // side effect!
        case (Some(agent), Right(msg: EncryptedMessage)) =>
          val program =
            OperationsClientRPC
              .decryptRaw(msg)
              .flatMap { data =>
                decryptDataVar.set(Some(data)) // side effect!
                Operations.parseMessage(data).map((msg, _))
              }
              .tapError(error => ZIO.succeed(decryptMessageVar.set(Some(Left(error))))) // side effect!
              .flatMap(msg =>
                decryptMessageVar.set(Some(Right(msg._2))) // side effect!
                msg match
                  case (eMsg: EncryptedMessage, plaintext: PlaintextMessage) =>
                    ZIO.succeed(Global.messageRecive(eMsg, plaintext)) // side effect!
                  case (eMsg: EncryptedMessage, sMsg: SignedMessage) =>
                    sMsg.payload.content.fromJson[Message] match
                      case Left(value)                        => ZIO.fail(FailToParse(value))
                      case Right(plaintext: PlaintextMessage) =>
                        ZIO.succeed(Global.messageRecive(eMsg, plaintext)) // side effect!
                      case Right(value: SignedMessage)    => ZIO.fail(FailDecryptDoubleSign(sMsg, value))
                      case Right(value: EncryptedMessage) => ZIO.fail(FailDecryptSignThenEncrypted(sMsg, value))
                  case (outsideMsg: EncryptedMessage, insideMsg: EncryptedMessage) =>
                    ZIO.fail(FailDecryptDoubleEncrypted(outsideMsg, insideMsg))
              )
          Utils.runProgram(program.provideSomeLayer(Global.resolverLayer).provideEnvironment(ZEnvironment(agent)))
      }
      .observe(owner)

  val rootElement = div(
    onMountCallback { ctx =>
      job(ctx.owner)
      ()
    },
    code("Decrypt/Verify Tool"),
    p(
      overflowWrap.:=("anywhere"),
      "Agent: ",
      " ",
      code(
        child.text <-- Global.agentVar.signal
          .map(_.map(_.id.string).getOrElse("NO AGENT IS SELECTED! (It can still verify sign message)"))
      )
    ),
    p("Message Data:"),
    textArea(
      rows := 20,
      cols := 80,
      autoFocus(true),
      placeholder("<SignedMessage or EncryptedMessage>"),
      value <-- dataVar,
      inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> dataVar }
    ),
    p("Message Protected Header (parsed):"),
    div(
      children <-- messageVar.map { mMsg =>
        mMsg.toSeq
          .flatMap {
            case sMsg: SignedMessage =>
              sMsg.signatures.flatMap { e =>
                e.`protected`.obj.kid match
                  case None      => Seq(pre(code("'kid' is missing from Protected Header!"))) // TODO REMOVE case
                  case Some(vmr) =>
                    Seq(
                      pre(
                        code(
                          a(vmr.did.string, MyRouter.navigateTo(MyRouter.ResolverPage(vmr.did.string))),
                          "#",
                          vmr.fragment,
                          ";"
                        )
                      ),
                      pre(code(e.`protected`.obj.toJson)),
                    )
              }
            case eMsg: EncryptedMessage =>
              eMsg.`protected`.obj match
                case header @ AnonProtectedHeader(epk, apv, typ, enc, alg) =>
                  Seq(
                    p("Anoncrypt:"),
                    pre(code(header.toJsonPretty)),
                  )
                case header @ AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
                  Seq(
                    p(
                      "Authcrypt from: ",
                      code(
                        a(skid.did.string, MyRouter.navigateTo(MyRouter.ResolverPage(skid.did.string))),
                        "#",
                        skid.fragment
                      )
                    ),
                    pre(code(header.toJsonPretty))
                  )
          }
      },
    ),
    p("Recipients kid:"),
    ul(
      children <-- messageVar.map(_.toSeq).map {
        case sMsg: SignedMessage =>
          sMsg.payloadAsPlaintextMessage match
            case Left(error) => Seq(li(code(error.toText)))
            case Right(pMsg) =>
              pMsg.to.toSeq.flatten.map { to =>
                li(code(a(to.value, MyRouter.navigateTo(MyRouter.ResolverPage(to.value)))))
              }
        case eMsg: EncryptedMessage =>
          eMsg.recipients.toSeq
            .map(_.header.kid)
            .map(kid =>
              li(
                code(
                  a(kid.did.string, MyRouter.navigateTo(MyRouter.ResolverPage(kid.did.string))),
                  "#",
                  kid.fragment
                )
              )
            )
      }
    ),
    p("Raw Data (as UTF8) after decrypting/verifying:"),
    pre(code(child.text <-- decryptDataVar.signal.map {
      case None        => "<none>"
      case Some(bytes) => String(bytes)
    })),
    p("Message after decrypting/verifying:"),
    pre(code(child.text <-- decryptMessageVar.signal.map {
      case None                => "<none>"
      case Some(Left(didFail)) => didFail.toString
      case Some(Right(msg))    => msg.toJsonPretty
    })),
    button(
      "Copy to clipboard",
      onClick --> { _ =>
        Global.copyToClipboard(
          decryptMessageVar.now() match
            case None                => "None"
            case Some(Left(didFail)) => didFail.toString
            case Some(Right(msg))    => msg.toJson
        )
      }
    )
  )

  def apply(): HtmlElement = rootElement
}
