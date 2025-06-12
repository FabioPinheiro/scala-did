package fmgp.webapp

import scala.util.Try
import scala.util.Failure
import scala.util.Success
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import com.raquo.laminar.codecs.StringAsIsCodec
import com.raquo.airstream.ownership._
import zio.json._

import proto.prism.PrismOperation
import fmgp.crypto.SHA256
import fmgp.did.method.prism._
import fmgp.did.method.prism.proto._
import fmgp.util._

@js.annotation.JSExportTopLevel("PrismVdrTool")
object PrismVdrTool {

  val dataVar: Var[String] = Var {
    // """{"tx":"bc688c8d7b1588a0fb10b307b6e975998eb0aedf039440580ddcec1cc51bb66d","b":89,"o":0,"signedWith":"master0","signature":"304502210088333a3046528671e651afda2975546a9ecfa9e5c953a649b8d5b6d8f84507bd02203d9c4f7930c6cf99d37c592b316bf0c2b1563a8525309478a7b9b4e6318d03eb","operation":{"CreateDidOP":{"publicKeys":[{"CompressedECKey":{"id":"master0","usage":"MasterKeyUsage","curve":"secp256k1","data":"021456f5dd7bcddca7a3e48edad8cc68d0ce6a7a5991492cb48bac817c2e4d9adc"}}],"services":[],"context":[]}},"protobuf":"0a3f0a3d123b0a076d61737465723010014a2e0a09736563703235366b311221021456f5dd7bcddca7a3e48edad8cc68d0ce6a7a5991492cb48bac817c2e4d9adc"}
    //   |{"tx":"bc688c8d7b1588a0fb10b307b6e975998eb0aedf039440580ddcec1cc51bb66d","b":89,"o":1,"signedWith":"master0","signature":"3045022100931b1a758cb7a34e7a159de8acb4e52751323e37aadeee2d33d121696ccb29b402204e54027e3ee1b6f38e511b9cf85865c45980e696686ad7548d810d2df05e8ed7","operation":{"UpdateDidOP":{"previousOperationHash":"00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae","id":"00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae","actions":[{"AddKey":{"key":{"CompressedECKey":{"id":"issuing0","usage":"IssuingKeyUsage","curve":"secp256k1","data":"02a680f17d9b683a9043b45a89989d37fed7a2de8a025eb19790933f59412b64f3"}}}},{"AddKey":{"key":{"CompressedECKey":{"id":"revocation0","usage":"RevocationKeyUsage","curve":"secp256k1","data":"0384cdd12ac3cf34f241a75281e755e97b35984845b0b7b922df14790b2a9a2266"}}}}]}},"protobuf":"12eb010a2000592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae1240303035393261313431613463326263623761366161363931373530353131653265396230343832333138323031323565313561623730623132613231306161651a400a3e0a3c0a0869737375696e673010024a2e0a09736563703235366b31122102a680f17d9b683a9043b45a89989d37fed7a2de8a025eb19790933f59412b64f31a430a410a3f0a0b7265766f636174696f6e3010054a2e0a09736563703235366b3112210384cdd12ac3cf34f241a75281e755e97b35984845b0b7b922df14790b2a9a2266"}
    //   |""".stripMargin
    """0a3f0a3d123b0a076d61737465723010014a2e0a09736563703235366b311221021456f5dd7bcddca7a3e48edad8cc68d0ce6a7a5991492cb48bac817c2e4d9adc
      |12eb010a2000592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae1240303035393261313431613463326263623761366161363931373530353131653265396230343832333138323031323565313561623730623132613231306161651a400a3e0a3c0a0869737375696e673010024a2e0a09736563703235366b31122102a680f17d9b683a9043b45a89989d37fed7a2de8a025eb19790933f59412b64f31a430a410a3f0a0b7265766f636174696f6e3010054a2e0a09736563703235366b3112210384cdd12ac3cf34f241a75281e755e97b35984845b0b7b922df14790b2a9a2266
      |""".stripMargin
  }

  def opSignal = dataVar.signal
    .map(_.split("\n").zipWithIndex.toSeq.filterNot(_._1.isBlank()))
    .map(
      _.map((rawData, index) =>
        val htmlElement = rawData.fromJson[MySignedPrismOperation[OP]] match
          case Left(error) =>
            Try {
              val protobufOP = PrismOperation.parseFrom(hex2bytes(rawData))
              val op = OP.fromPrismOperation(protobufOP)
              div(
                pre(code("HASH:"), code(SHA256.digestToHex(protobufOP.toByteArray))),
                pre(code(op.toJsonPretty))
              )
            } match
              case Success(div) => div
              case Failure(exception) =>
                div(
                  s"Fail parse line $index:",
                  ul(
                    li(s"Not a MySignedPrismOperation due to: $error"),
                    li(s"Not a PrismOperation due to: ${exception.getMessage}"),
                  )
                )
          case Right(op: MySignedPrismOperation[OP]) =>
            div(
              pre(code("HASH:"), code(op.opHash)),
              pre(code(op.toJsonPretty))
            )
        div(hr(), h2(s"Line $index: "), htmlElement)
      )
    )

  def apply(): HtmlElement = div(
    h1("PRISM VDR Tool"),
    div(p("Input a list (separated by line) of MySignedPrismOperation[OP] or proto.PrismOperation")), // FIXME REMOVE
    div(
      textArea(
        rows := 10,
        cols := 80,
        htmlProp("wrap", StringAsIsCodec)("off"),
        autoFocus(true),
        value <-- dataVar,
        onInput.mapToValue --> dataVar
      )
    ),
    div(
      children <-- opSignal
    ),
  )
}
