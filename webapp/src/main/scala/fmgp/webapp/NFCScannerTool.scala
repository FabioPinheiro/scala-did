package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import com.raquo.airstream.ownership._

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.comm.protocol.oobinvitation.*
import fmgp.did.method.peer.DIDPeer._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.crypto.error._
import fmgp.webapp.MyRouter.OOBPage
import com.raquo.laminar.nodes.ReactiveElement

import org.scalajs.dom.NDEFReader
import org.scalajs.dom.NDEFMessage
import scala.scalajs.js.ReferenceError

/** Based on https://googlechrome.github.io/samples/web-nfc/
  *
  * API in ttps://developer.mozilla.org/en-US/docs/Web/API/NDEFReader
  */
@js.annotation.JSExportTopLevel("NFCScannerTool")
object NFCScannerTool {

  val notSupported = Var[Boolean](initial = true)
  val dataVar = Var[Either[String, String]](initial = Left("None"))

  @js.annotation.JSExport
  var messageForDebug: NDEFMessage = null

  def initNFCScanner = {
    try {
      val ndef = new NDEFReader()
      ndef.scan().`then`(_ => println("NFC scan started"))

      ndef.onreadingerror = (_) => {
        println("Argh! Cannot read data from the NFC tag. Try another one?");
      }

      ndef.onreading = (event) => {
        println(s"> Serial Number: ${event.serialNumber}")
        println(s"> Message: (${event.message})")
        messageForDebug = event.message
        messageForDebug.records.map {
          case record if record.recordType == "url" =>
            val data = record.data.asInstanceOf[js.typedarray.DataView]
            val dataStr = typings.std.global.TextDecoder().decode(data)
            dataVar.set(Right(dataStr))
            println(s"> Message: (${dataStr})")
          case record => dataVar.set(Left(s"NTC record.recordType not supported: ${record.recordType}"))
        }

      }
    } catch {
      case scala.scalajs.js.JavaScriptException(referenceError: ReferenceError)
          if referenceError.message == "NDEFReader is not defined" =>
        notSupported.set(false)
    }
  }

  val oobExtract = dataVar.signal.map(_.flatMap { data =>
    OutOfBand.parse(data) match
      case None        => Left("Fail parsing OutOfBand")
      case Some(value) => Right(value)
  })

  val oobMessage = dataVar.signal.map(_.flatMap { data =>
    println(data)
    OutOfBand.oob(data)
  })

  def apply(): HtmlElement = div(
    onMountCallback { ctx =>
      initNFCScanner
      // job(ctx.owner)
    },
    onUnmountCallback { ctx =>
      // destroyQrScanner
    },
    div(code("NFC Scanner Tool Page")),
    children <-- notSupported.signal.map {
      case false =>
        Seq(
          div(
            p("NFC not supported on this device."),
            p(
              "None: Web NFC API is a mobule only API. See ",
              a(
                href := "https://developer.mozilla.org/en-US/docs/Web/API/Web_NFC_API#browser_compatibility",
                "Mozilla docs's about Web NFC API"
              ),
              "."
            )
          )
        )
      case true =>
        Seq(
          div(child <-- dataVar.signal.map {
            case Left("None") => p("No NFC detected")
            case Left(error)  => p(s"Fail: $error")
            case Right(data)  => p("QRcode:", code(data))
          }),
          div(child <-- oobExtract.map {
            case Left(left)   => p(s"Missing _oob: $left")
            case Right(value) => p("_oob match ", code(value.urlBase64))
          }),
          div(children <-- oobMessage.map {
            case Left(error) => Seq(p("Parsing error", code(error)))
            case Right(oobMsg) =>
              Seq(
                pre(code(oobMsg.msg.toJsonPretty)),
                p("Open with the ", b("OOB Tool "), MyRouter.navigateTo(MyRouter.OOBPage(oobMsg))),
              )
          }),
        )
    }
  )

}
