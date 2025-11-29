package fmgp.webapp

import scala.scalajs.js.timers.*
import scala.scalajs.js.JSConverters.*

import org.scalajs.dom
import com.raquo.airstream.core.Sink
import com.raquo.airstream.ownership.*
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement

import zio.*
import zio.json.*

import proto.prism.*
import fmgp.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.extension.*
import fmgp.did.method.peer.DIDPeer2
import fmgp.did.method.prism.proto.*
import fmgp.did.uniresolver.Uniresolver
import fmgp.did.comm.protocol.routing2.ForwardMessage
import fmgp.crypto.error.*
import scala.annotation.tailrec
import scalapb.GeneratedMessageCompanion
import scalapb.GeneratedMessage
import fmgp.util.*
import scalapb.descriptors.*
import scala.collection.View.Single
import fmgp.webapp.utils.ProtoHTML
import com.google.protobuf.ByteString

object PrismVdrEditTool {
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
            case ScalaType.Boolean             => code(s"${field.number} ${field.name} - Boolean")
            case ScalaType.ByteString          => code(s"${field.number} ${field.name} - ByteString")
            case ScalaType.Double              => code(s"${field.number} ${field.name} - Double")
            case ScalaType.Float               => code(s"${field.number} ${field.name} - Float")
            case ScalaType.Int                 => code(s"${field.number} ${field.name} - Int")
            case ScalaType.Long                => code(s"${field.number} ${field.name} - Long")
            case ScalaType.String              => code(s"${field.number} ${field.name} - String")
            case ScalaType.Enum(descriptor)    => code(s"${field.number} ${field.name} - Enum")
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

  val prismEventVar: Var[Option[PrismEvent]] =
    Var(initial =
      Some(
        PrismEvent.parseFrom(
          hex2bytes(
            "12eb010a2000592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae1240303035393261313431613463326263623761366161363931373530353131653265396230343832333138323031323565313561623730623132613231306161651a400a3e0a3c0a0869737375696e673010024a2e0a09736563703235366b31122102a680f17d9b683a9043b45a89989d37fed7a2de8a025eb19790933f59412b64f31a430a410a3f0a0b7265766f636174696f6e3010054a2e0a09736563703235366b3112210384cdd12ac3cf34f241a75281e755e97b35984845b0b7b922df14790b2a9a2266"
          )
        )
      )
    )

  val companionVar: Var[Option[scalapb.GeneratedMessageCompanion[PrismEvent]]] =
    Var(initial = Some(PrismEvent.messageCompanion))

  def htmlInputFromCompanionProto(
      companion: GeneratedMessageCompanion[PrismEvent],
      // maybeOP: Option[PrismEvent]
  ): ReactiveHtmlElement[dom.html.Element] = {
    val mb = ProtoHTML.MessageBuilder(companion)

    val signalPValue = mb.signalPMessage
    def signalPrismEvent = signalPValue
      .map(pv => companion.messageReads.read(pv))
    def signedPrismEvent = signalPrismEvent
      .map(prismEvent =>
        SignedPrismEvent(
          signedWith = "vdr-key-id",
          signature = ByteString.copyFrom("fixme".getBytes()),
          event = Some(prismEvent)
        )
      )

    div(
      mb.html,
      hr(),
      h2("PrismEvent:"),
      pre(
        code(
          child <-- signalPrismEvent.map(op => bytes2Hex(op.toByteArray))
        )
      ),
      hr(),
      p(child <-- signalPValue.map(pv => pv).map(e => e.toString())),
      hr(),
      p(child <-- signalPrismEvent.map(e => e.toString())),
      hr(),
      h2("SignedPrismEvent:"),
      pre(
        code(
          child <-- signalPrismEvent
            .map(prismEvent => OP.fromPrismEvent(prismEvent))
            .map(op => op.toJson)
        )
      ),
      hr(),
      h2("PrismBlock:"),
      pre(
        code(
          child <-- signedPrismEvent
            .map(signedPrismEvent => PrismBlock(Seq(signedPrismEvent)))
            .map(op => bytes2Hex(op.toByteArray))
        )
      )
    )
  }

  val rootElement = div(
    // onMountCallback { ctx => () }, // side effect
    div(h1("PrismVdrEdit Tool")),
    hr(),
    div(
      child <-- companionVar.signal.map {
        case None             => div()
        case Some(descriptor) =>
          htmlInputFromCompanionProto(
            descriptor,
            // Some(
            //   PrismEvent.parseFrom(
            //     hex2bytes(
            //       "0a3f0a3d123b0a076d61737465723010014a2e0a09736563703235366b311221021456f5dd7bcddca7a3e48edad8cc68d0ce6a7a5991492cb48bac817c2e4d9adc"
            //     )
            //   )
            // )
          )
      }
    ),
  )

  def apply(): HtmlElement = rootElement
}
