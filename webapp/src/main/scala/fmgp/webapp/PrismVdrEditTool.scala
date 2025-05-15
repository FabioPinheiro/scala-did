package fmgp.webapp

import scala.scalajs.js.timers._
import scala.scalajs.js.JSConverters._

import org.scalajs.dom
import org.scalajs.dom.HTMLButtonElement
import com.raquo.airstream.core.Sink
import com.raquo.airstream.ownership._
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement

import zio._
import zio.json._

import proto.prism._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.extension._
import fmgp.did.method.peer.DIDPeer2
import fmgp.did.uniresolver.Uniresolver
import fmgp.did.comm.protocol.routing2.ForwardMessage
import fmgp.crypto.error._
import fmgp.Utils
import fmgp.ServiceWorkerUtils
import fmgp.Config
import fmgp.NotificationsSubscription
import scala.annotation.tailrec
import scalapb.GeneratedMessageCompanion
import scalapb.GeneratedMessage
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import fmgp.util.hex2bytes
import scalapb.descriptors._
import scala.collection.View.Single
import fmgp.prism.OP
import fmgp.util.bytes2Hex

object PrismVdrEditTool {

  // sealed trait Command {
  //   def index: Int
  //   def uri: String
  //   def command: String
  //   def copyButton: ReactiveHtmlElement[HTMLButtonElement]
  //   protected def program: ZIO[Resolver, DidFail, Unit]
  //   def executeCommand: Fiber[Nothing, Unit] = Utils.runProgram(program.provide(Global.resolverLayer))
  //   def executeCommandButton =
  //     button(
  //       "Execute Command",
  //       onClick --> Sink.jsCallbackToSink(_ =>
  //         encryptedMessageVar.now() match {
  //           case Some(Right((plaintext, eMsg))) =>
  //             Global.messageSend(eMsg, Global.agentVar.now().map(_.id.asFROM).getOrElse(???), plaintext) // side effect
  //             executeCommand // side effect
  //           case _ => // None
  //         }
  //       )
  //     )
  // }
  // case class CommandHttp(index: Int, uri: String, msg: EncryptedMessage) extends Command {
  //   def command = s"""curl -X POST $uri -H 'content-type: application/didcomm-encrypted+json' -d '${msg.toJson}'"""
  //   def copyButton = button("Copy curl command", onClick --> { _ => Global.copyToClipboard(command) })
  //   def program = Client
  //     .makeDIDCommPost(msg, uri) // TODO this should be a TransportDIDComm
  //     .map { // side effects
  //       case None => Left(s"Zero responses in this Transport in the time frame of ${Global.transportTimeoutVar.now()}s")
  //       case Some(output) =>
  //         import SignedOrEncryptedMessage.given
  //         output.fromJson[SignedOrEncryptedMessage] match
  //           case Left(fail) => Left(s"Fail to parse due to: '$fail'")
  //           case right      => right
  //     }
  //     .tapBoth(
  //       error => ZIO.logError(error.toString) *> ZIO.succeed(outputFromCallVar.update(_ :+ Left(error.toString()))),
  //       item => ZIO.succeed(outputFromCallVar.update(_ :+ item)) // side effect
  //     )
  //     .unit
  // }
  // case class CommandWs(index: Int, uri: String, msg: EncryptedMessage) extends Command {
  //   def command = s"""wscat -c $uri -w 3 -x '${msg.toJson}'"""
  //   def copyButton = button("Copy wscat command", onClick --> { _ => Global.copyToClipboard(command) })
  //   def program =
  //     for {
  //       transport <- Utils.openWsProgram(wsUrl = uri, timeout = Global.transportTimeoutVar.now())
  //       _ <- transport.send(msg)
  //       _ <- transport.inbound.foreach {
  //         case eMsg: EncryptedMessage => ZIO.succeed(outputFromCallVar.update(_ :+ Right(eMsg)))
  //         case sMsg: SignedMessage =>
  //           ZIO.succeed(outputFromCallVar.update(_ :+ Left("UI not implemented for SignedMessage"))) // TODO
  //       }
  //     } yield ()
  // }
  // case class CommandInvalid(index: Int, uri: String) extends Command {
  //   def command = s"unknown protocol: '$uri'"
  //   def copyButton = button("Copy command", disabled := true)
  //   def program = ZIO.fail(FailToParse(command))
  // }

  // val encryptedMessageVar: Var[Option[Either[DidFail, (PlaintextMessage, EncryptedMessage)]]] = Var(initial = None)
  // val signMessageVar: Var[Option[Either[DidFail, (PlaintextMessage, SignedMessage)]]] = Var(initial = None)

  /*
  def fieldDescriptor(field: scalapb.descriptors.FieldDescriptor, level: Int): String = {
    import scalapb.descriptors.ScalaType
    def ret = field.scalaType match
      case ScalaType.Boolean             => "Boolean"
      case ScalaType.ByteString          => "ByteString"
      case ScalaType.Double              => "Double"
      case ScalaType.Float               => "Float"
      case ScalaType.Int                 => "Int"
      case ScalaType.Long                => "Long"
      case ScalaType.String              => "String"
      case ScalaType.Message(descriptor) => messageDescriptor(descriptor, level + 1)
      case ScalaType.Enum(descriptor)    => "Enum"
    s"${field.number} ${field.name} - $ret"
  }
  def messageDescriptor(d: scalapb.descriptors.Descriptor, level: Int): String =
    s"""${d.fullName}:\n${d.fields.map { f => ("  " * (level + 1)) + fieldDescriptor(f, level) }.mkString("\n")}"""
  val protoCreateDIDDescriptor = messageDescriptor(ProtoCreateDID.scalaDescriptor, 0)
   */

  def htmlMessageDescriptor(d: scalapb.descriptors.Descriptor, level: Int): ReactiveHtmlElement[dom.html.Element] = {
    import scalapb.descriptors.ScalaType
    div(
      marginRight.px(20),
      code(d.fullName + ":"),
      ul(
        d.fields.map { field =>
          val ret = field.scalaType match
            case ScalaType.Boolean          => code(s"${field.number} ${field.name} - Boolean")
            case ScalaType.ByteString       => code(s"${field.number} ${field.name} - ByteString")
            case ScalaType.Double           => code(s"${field.number} ${field.name} - Double")
            case ScalaType.Float            => code(s"${field.number} ${field.name} - Float")
            case ScalaType.Int              => code(s"${field.number} ${field.name} - Int")
            case ScalaType.Long             => code(s"${field.number} ${field.name} - Long")
            case ScalaType.String           => code(s"${field.number} ${field.name} - String")
            case ScalaType.Enum(descriptor) => code(s"${field.number} ${field.name} - Enum")
            case ScalaType.Message(descriptor) =>
              div(
                code(s"${field.number} ${field.name} - ${descriptor.fullName}"),
                htmlMessageDescriptor(descriptor, level + 1)
              )
          li(ret)
        }
      )
    )
  }

  val companionVar: Var[Option[scalapb.GeneratedMessageCompanion[PrismOperation]]] =
    Var(initial = Some(PrismOperation.messageCompanion))

  class FieldBuilder(val descriptor: scalapb.descriptors.FieldDescriptor, val builder: AuxBuilder)
  trait AuxBuilder {
    def signalPValue: Signal[PValue]
    def html: ReactiveHtmlElement[dom.html.Element]
  }

  def htmlFieldFieldDescriptor[A <: GeneratedMessage](
      companion: GeneratedMessageCompanion[A],
      level: Int = 0,
      field: FieldDescriptor,
  ): Signal[FieldBuilder] = {
    import scalapb.descriptors._
    field.scalaType match {
      case ScalaType.Boolean =>
        Signal.fromValue(
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilder {
              def signalPValue: Signal[PValue] = Signal.fromValue(PEmpty)
              def html = code(s"${field.number} ${field.name} - Boolean: ")
            }
          )
        )
      case ScalaType.ByteString =>
        Signal.fromValue(
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilder {
              val auxVar: Var[String] = Var("")
              val aux = input(
                placeholder("ByteString in Hex"),
                value <-- auxVar,
                onInput.mapToValue --> auxVar
              )
              def signalPValue =
                auxVar.signal.map { str =>
                  if (str.isEmpty) PByteString(com.google.protobuf.ByteString.EMPTY)
                  else PByteString(com.google.protobuf.ByteString.copyFrom(hex2bytes(str)))
                }

              def html = div(code(s"${field.number} ${field.name} - ByteString: "), aux)
            }
          )
        )
      case ScalaType.Double =>
        Signal.fromValue(
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilder {
              def signalPValue: Signal[PValue] = Signal.fromValue(PEmpty)
              def html = code(s"${field.number} ${field.name} - Double: ")
            }
          )
        )
      case ScalaType.Float =>
        Signal.fromValue(
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilder {
              def signalPValue: Signal[PValue] = Signal.fromValue(PEmpty)
              def html = code(s"${field.number} ${field.name} - Float: ")
            }
          )
        )
      case ScalaType.Int =>
        Signal.fromValue(
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilder {
              def signalPValue: Signal[PValue] = Signal.fromValue(PEmpty)
              def html = code(s"${field.number} ${field.name} - Int: ")
            }
          )
        )
      case ScalaType.Long =>
        Signal.fromValue(
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilder {
              def signalPValue: Signal[PValue] = Signal.fromValue(PEmpty)
              def html = code(s"${field.number} ${field.name} - Long: ")
            }
          )
        )
      case ScalaType.String =>
        Signal.fromValue(
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilder {
              val auxVar: Var[String] = Var("")
              val aux = input(
                placeholder("String"),
                value <-- auxVar,
                onInput.mapToValue --> auxVar
              )
              def signalPValue = auxVar.signal.map(str => PString(str))
              def html = div(code(s"${field.number} ${field.name} - String: "), aux)
            }
          )
        )
      case ScalaType.Enum(descriptor) =>
        Signal.fromValue(
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilder {

              val optionsVec = descriptor.values
              val selectedVar = Var(optionsVec.head)
              val selectHtml = select(
                value <-- selectedVar.signal.map(_.number.toString),
                onChange.mapToValue.map { n => optionsVec.find(_.number == n.toInt).get } --> selectedVar,
                optionsVec.map(d => option(value := d.number.toString(), d.fullName))
              )
              def signalPValue: Signal[PValue] = selectedVar.signal.map(selected => PEnum(selected))
              def html = div(code(s"${field.number} ${field.name} - Enum: "), selectHtml)
            }
          )
        )
      case ScalaType.Message(descriptor) =>
        Try(companion.messageCompanionForFieldNumber(field.number)) match
          case Failure(exception) =>
            Signal.fromValue(
              FieldBuilder(
                descriptor = field,
                builder = new AuxBuilder {
                  def signalPValue: Signal[PValue] = Signal.fromValue(PEmpty)
                  def html = code(
                    s"## ${field.number} ${field.name} - ${descriptor.fullName}: ERROR>  ${exception.getMessage()}"
                  )
                }
              )
            )
          case Success(nextCompanion) =>
            htmlFromCompanionProto(nextCompanion, level + 1).map((tmp, signal) =>
              FieldBuilder(
                descriptor = field,
                builder = new AuxBuilder {
                  def signalPValue: Signal[PValue] = signal
                  def html = div(code(s"${field.number} ${field.name} - ${descriptor.fullName}"), tmp)
                }
              )
            )
    }
  }

  def htmlFromCompanionProto[A <: GeneratedMessage](
      companion: GeneratedMessageCompanion[A],
      level: Int = 0
  ): Signal[(ReactiveHtmlElement[dom.html.Element], Signal[PValue])] = {
    val descriptor = companion.scalaDescriptor

    val selectors = descriptor.oneofs.toSeq.map { oneofDescriptor =>
      val descriptorVec = oneofDescriptor.fields
      val tmpVar = Var(descriptorVec.head)
      val selectHtml = select(
        value <-- tmpVar.signal.map(_.number.toString),
        onChange.mapToValue.map { n => descriptorVec.find(_.number == n.toInt).get } --> tmpVar,
        descriptorVec.map(d => option(value := d.number.toString(), d.fullName))
      )
      (oneofDescriptor, selectHtml, tmpVar)
    }

    val allFD_that_is_oneofs = descriptor.oneofs.flatMap(e => e.fields)
    val allFD_that_is_not_oneofs = descriptor.fields.filterNot(fd => allFD_that_is_oneofs.contains(fd))
    val selectedFDs = Signal.combineSeq(selectors.map(_._3.signal))
    val allActiveFDs = Signal.fromValue(allFD_that_is_not_oneofs).combineWith(selectedFDs).map(e => e._1 ++ e._2)

    allActiveFDs
      .flatMap { fields =>
        val retFields: Vector[Signal[FieldBuilder]] = fields.map { field =>
          htmlFieldFieldDescriptor(companion, level, field)
        }
        val retFields2: Signal[Seq[FieldBuilder]] = Signal.combineSeq(retFields)
        retFields2
      }
      .map { retFields =>
        val tmp = retFields.map { field =>
          val builder = field.builder
          if (!field.descriptor.isRepeated) (field.descriptor, builder.html, builder.signalPValue)
          else
            (
              field.descriptor,
              builder.html,
              Signal.combineSeq(Seq(builder.signalPValue)).map(v => PRepeated(v.toVector))
            )
        }
        (
          div(
            marginRight.px(20),
            div(
              code(descriptor.fullName + ":"),
              if (selectors.isEmpty) Vector.empty[ReactiveHtmlElement[dom.html.Element]]
              else selectors.map(selector => div(label("using "), selector._2))
            ),
            ul(tmp.map(e => li(e._2))),
          ),
          Signal
            .combineSeq(tmp.map((fd, htmlElement, signalPValue) => signalPValue.map(pv => (fd, pv))))
            .map(seq => PMessage(seq.toMap)),
        )
      }
  }

  def htmlInputFromCompanionProto( // [A <: GeneratedMessage](
      companion: GeneratedMessageCompanion[PrismOperation] // [A]
  ): Signal[ReactiveHtmlElement[dom.html.Element]] =
    htmlFromCompanionProto(companion, 0).map { (htmlAux, signalPValue) =>
      div(
        htmlAux,
        hr(),
        pre(
          code(
            child <-- signalPValue.map(pv => companion.messageReads.read(pv)).map(op => bytes2Hex(op.toByteArray))
          )
        ),
        hr(),
        p(child <-- signalPValue.map(pv => pv).map(e => e.toString())),
        hr(),
        p(child <-- signalPValue.map(pv => companion.messageReads.read(pv)).map(e => e.toString())),
        hr(),
        pre(
          code(
            child <-- signalPValue
              .map(pv => OP.fromPrismOperation(companion.messageReads.read(pv)))
              .map(op => op.toJson)
          )
        )
      )
    }

  ProtoCreateDID.messageCompanion.defaultInstance

  val rootElement = div(
    onMountCallback { ctx =>
      // callSignViaRPC(ctx.owner) // side effect
      // callEncryptedViaRPC(ctx.owner) // side effect
      // jobNextForward(ctx.owner) // side effect
      // callCommand(ctx.owner) // side effect
      ()
    },
    div(h1("PrismVdrEdit Tool")),
    hr(),
    div(
      child <-- companionVar.signal.flatMap {
        case None             => Signal.fromValue(div())
        case Some(descriptor) => htmlInputFromCompanionProto(descriptor)
      }
    ),
  )

  def apply(): HtmlElement = rootElement
}
