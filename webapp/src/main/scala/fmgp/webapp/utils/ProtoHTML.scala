package fmgp.webapp.utils

import scala.util._
import org.scalajs.dom

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import scalapb.descriptors._
import scalapb._

import fmgp.util._
import fmgp.webapp.utils.ProtoHTML.AuxBuilder

object ProtoHTML {

  class FieldBuilder(val descriptor: FieldDescriptor, var builder: AuxBuilder[PValue]) {
    def signalFDPValue =
      if (!descriptor.isRepeated) builder.signalPValue.map(pv => (descriptor, pv))
      else Signal.combineSeq(Seq(builder.signalPValue)).map(pvs => (descriptor, PRepeated(pvs.toVector)))

    def html: ReactiveHtmlElement[dom.html.Element] = builder.html

    // def load(pv: PValue): Unit = pv match
    //   case PEmpty => ???
    //   case PInt(value) =>
    //     new AuxBuilder {
    //       def signalPValue: Signal[PValue] = ???
    //       def html: ReactiveHtmlElement[dom.html.Element] = ???
    //     }
    //   case PLong(value)       =>
    //   case PString(value)     =>
    //   case PDouble(value)     =>
    //   case PFloat(value)      =>
    //   case PByteString(value) =>
    //   case PBoolean(value)    =>
    //   case PEnum(value)       =>
    //   case PMessage(value)    =>
    //   case PRepeated(value)   =>

  }

  sealed trait AuxBuilder[+T <: PValue] {
    def signalPValue: Signal[T]
    def html: ReactiveHtmlElement[dom.html.Element]
    // def load(t: T): Unit
  }
  abstract class AuxBuilderSignal[T <: PValue] extends AuxBuilder[T]

  //
  trait AuxBuilderVar[T <: PValue] extends AuxBuilder[T] {
    type AUX <: T
    def auxVar: Var[AUX]
    def load(t: AUX): Unit = auxVar.update(_ => t)
    override def signalPValue: Signal[T] = auxVar.signal
  }

  class MessageBuilder[A <: GeneratedMessage](companion: GeneratedMessageCompanion[A]) extends AuxBuilder[PValue] {
    type AUX = PMessage
    def load(t: AUX): Unit = ???
    private val descriptor = companion.scalaDescriptor
    private val oneOfs = descriptor.oneofs.toSeq.map { oneofDescriptor => OneOfBuilder(companion, oneofDescriptor) }
    private val allFD_that_is_oneofs = descriptor.oneofs.flatMap(e => e.fields)
    private val allFD_that_is_not_oneofs = descriptor.fields.filterNot(fd => allFD_that_is_oneofs.contains(fd))
    private val fb_that_is_not_oneofs =
      allFD_that_is_not_oneofs.map(fd => FieldBuilder.fromFieldDescriptor(companion, fd))

    def signalPMessage =
      Signal
        .combineSeq(
          (fb_that_is_not_oneofs ++ oneOfs).map {
            case fb: FieldBuilder        => fb.signalFDPValue
            case oneOfB: OneOfBuilder[A] => oneOfB.signalFieldBuilder.flatMap(_.signalFDPValue)
          }
        )
        .map { seq => PMessage(seq.toMap) }
    def signalPValue = signalPMessage.map(i => i.asInstanceOf[PValue]) // FIXME

    def html =
      div(
        marginRight.px(20),
        code(descriptor.fullName + ":"),
        // if (oneOfs.isEmpty) Vector.empty[ReactiveHtmlElement[dom.html.Element]] else oneOfs.map(_.selectHtml)
        ul((fb_that_is_not_oneofs.map(e => e.html) ++ oneOfs.map(_.html)).map(li(_)))
      )

    // def load(a: A) = a.toPMessage.value.map { case (fieldDescriptor, pv) =>
    //   fb_that_is_not_oneofs.find(_.descriptor.number == fieldDescriptor.number) match
    //     case None     => ???
    //     case Some(fb) => fb.load(pv)
    // }

  }

  class OneOfBuilder[A <: GeneratedMessage](
      val companion: GeneratedMessageCompanion[A],
      val oneofDescriptor: OneofDescriptor,
  ) {
    private val descriptorVec = oneofDescriptor.fields
    private val fieldsBuilder = descriptorVec.map(field => FieldBuilder.fromFieldDescriptor(companion, field))
    private val variable = Var(descriptorVec.head)
    val signalFieldBuilder = variable.signal.map(fd => fieldsBuilder.find(fb => fb.descriptor == fd).get)

    def selectHtml =
      select(
        value <-- variable.signal.map(_.number.toString),
        onChange.mapToValue.map { n => descriptorVec.find(_.number == n.toInt).get } --> variable,
        descriptorVec.map(d => option(value := d.number.toString(), d.fullName))
      )

    val html =
      div(
        selectHtml,
        child <-- variable.signal.map(fd => fieldsBuilder.find(fb => fb.descriptor == fd).get.html)
      )

  }

  object FieldBuilder {
    def fromFieldDescriptor[A <: GeneratedMessage](
        companion: GeneratedMessageCompanion[A],
        field: FieldDescriptor,
    ): FieldBuilder = {
      field.scalaType match {
        case ScalaType.Boolean =>
          println("??? Boolean")
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilderVar {
              type AUX = PBoolean
              val auxVar = Var(PBoolean(false))
              def html = code(s"${field.number} ${field.name} - Boolean: ")
            }
          )
        case ScalaType.ByteString =>
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilderVar {
              type AUX = PByteString
              val auxVar = Var(PByteString(com.google.protobuf.ByteString.EMPTY))
              val aux = input(
                placeholder("ByteString in Hex"),
                value <-- auxVar.signal.map { e => bytes2Hex(e.value.bytes) },
                onInput.mapToValue.map { str =>
                  if (str.isEmpty) PByteString(com.google.protobuf.ByteString.EMPTY)
                  else PByteString(com.google.protobuf.ByteString.copyFrom(hex2bytes(str)))
                } --> auxVar
              )
              def html = div(code(s"${field.number} ${field.name} - ByteString: "), aux)
            }
          )
        case ScalaType.Double =>
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilderVar {
              type AUX = PDouble
              val auxVar = Var(PDouble(0))
              def html = code(s"${field.number} ${field.name} - Double: ")
            }
          )
        case ScalaType.Float =>
          println("??? Float")
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilderVar {
              type AUX = PFloat
              val auxVar = Var(PFloat(0))
              def html = code(s"${field.number} ${field.name} - Float: ")
            }
          )
        case ScalaType.Int =>
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilderVar {
              type AUX = PInt
              val auxVar = Var(PInt(0))
              val aux = input(
                `type` := "number",
                placeholder("Int"),
                value <-- auxVar.signal.map(_.value.toString()),
                onInput.mapToValue.map(e => PInt(e.toIntOption.getOrElse(0))) --> auxVar
              )
              def html = div(code(s"${field.number} ${field.name} - Int: "), aux)
            }
          )
        case ScalaType.Long =>
          println("??? Long")
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilderVar {
              type AUX = PLong
              val auxVar = Var(PLong(0))
              def html = code(s"${field.number} ${field.name} - Long: ")
            }
          )
        case ScalaType.String =>
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilderVar {
              type AUX = PString
              val auxVar: Var[PString] = Var(PString(""))
              val aux = input(
                placeholder("String"),
                value <-- auxVar.signal.map(_.value),
                onInput.mapToValue.map(str => PString(str)) --> auxVar
              )
              def html = div(code(s"${field.number} ${field.name} - String: "), aux)
            }
          )
        case ScalaType.Enum(descriptor) =>
          FieldBuilder(
            descriptor = field,
            builder = new AuxBuilderVar {
              val optionsVec = descriptor.values
              type AUX = PEnum
              val auxVar: Var[PEnum] = Var(PEnum(optionsVec.find(_.number == 0).get))

              val selectHtml = select(
                value <-- auxVar.signal.map(_.value.number.toString),
                onChange.mapToValue
                  .map { n => optionsVec.find(_.number == n.toInt).get }
                  .map(enumValueDescriptor => PEnum(enumValueDescriptor)) --> auxVar,
                optionsVec.map(d => option(value := d.number.toString(), d.fullName))
              )
              def html = div(code(s"${field.number} ${field.name} - Enum: "), selectHtml)
            }
          )
        case ScalaType.Message(descriptor) =>
          Try(companion.messageCompanionForFieldNumber(field.number)) match
            case Failure(exception) =>
              FieldBuilder(
                descriptor = field,
                builder = new AuxBuilderSignal[PMessage] {
                  def load(t: PMessage): Unit = ??? // FIXME
                  def signalPValue: Signal[PMessage] = Signal.fromValue(PMessage(Map.empty)) // FIXME
                  def html = code(
                    s"## ${field.number} ${field.name} - ${descriptor.fullName}: ERROR>  ${exception.getMessage()}"
                  )
                }
              )
            case Success(nextCompanion) =>

              FieldBuilder(
                descriptor = field,
                builder = new AuxBuilderSignal[PMessage] {

                  val mb = MessageBuilder(nextCompanion)

                  def load(pMessage: PMessage): Unit = mb.load(pMessage)

                  def signalPValue = mb.signalPMessage
                  def html = div(
                    code(s"${field.number} ${field.name} - ${descriptor.fullName}"),
                    mb.html
                  )
                }
              )
      }
    }
  }
}
