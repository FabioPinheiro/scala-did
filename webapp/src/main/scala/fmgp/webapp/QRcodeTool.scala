package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers.*
import js.JSConverters.*

import com.raquo.laminar.api.L.*
import com.raquo.airstream.ownership.*

import zio.*
import zio.json.*

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.comm.protocol.oobinvitation.*
import fmgp.did.method.peer.DIDPeer.*
import fmgp.did.method.peer.DidPeerResolver
import fmgp.webapp.MyRouter.OOBPage
import fmgp.Utils

import typings.qrScanner.mod.QrScanner
import typings.qrScanner.mod.QrScanner.Camera
import typings.qrScanner.mod.QrScanner.ScanResult
import typings.qrScanner.mod.{default as QrScannerDefault}
import typings.qrScanner.anon.CalculateScanRegion
import typings.qrScanner.mod.QrScanner.ScanRegion

@js.annotation.JSExportTopLevel("QRcodeTool")
object QRcodeTool {

  enum QRTool:
    case Generator, Scanner

  val qrToolVar = Var[QRTool](initial = QRTool.Generator)

  def buttonMakeQRcode(msg: Message, sideEffect: => Unit = ()) = button(
    "Copy to QRcode Tool",
    disabled := msg.isInstanceOf[EncryptedMessage], // TODO REMOVE after https://github.com/raquo/Airstream/issues/113
    onClick --> { _ =>
      sideEffect // side effect
      dataVar.set(msg.toJsonPretty)
    },
    MyRouter.navigateTo(MyRouter.QRcodePage)
  )

  // ##################################################################################################################

  val qrcodeVar = Var[Option[String]](initial = None)

  val videoContainer = videoTag(width := "100%")

  // When sarting will download the file: ./vendors-node_modules_qr-scanner_qr-scanner-worker_min_js-library.js
  var qrScanner: Option[QrScanner] = None

  @js.annotation.JSExport
  def initQrScanner = {
    val tmp = QrScannerDefault(
      video = videoContainer.ref, // : HTMLVideoElement,
      onDecode = (qrCode: ScanResult) => {
        qrcodeVar.set(Some(qrCode.data))
        stopQrScanner
        ()
      },
      CalculateScanRegion()
        .setHighlightCodeOutline(true)
        .setHighlightScanRegion(true)
        .setCalculateScanRegion { htmlVideoElement =>
          ScanRegion() // Check any QRcode in the screen
            .setDownScaledHeight(0)
            .setDownScaledWidth(0)
        }
    )
    qrScanner = Some(tmp) // side effect

    Utils.runProgram(
      ZIO.fromPromiseJS(tmp.start()) *>
        ZIO // updateListOfCameras
          .fromPromiseJS(QrScannerDefault.listCameras(true))
          .map(e => e.toSeq)
          .map { c =>
            camerasVar.set(c) // side effect
            cameraSelectedVar.set(c.headOption) // side effect
          }
    )
  }

  @js.annotation.JSExport
  def startQrScanner(cameraId: Option[String] = None) = {
    (qrScanner, cameraId) match
      case (None, _)                 =>
      case (Some(scanner), None)     => Utils.runProgram(ZIO.fromPromiseJS(scanner.start()))
      case (Some(scanner), Some(id)) =>
        Utils.runProgram(ZIO.fromPromiseJS(scanner.start()) *> ZIO.fromPromiseJS(scanner.setCamera(id)))

  }

  @js.annotation.JSExport
  def stopQrScanner = {
    qrScanner.map(_.stop())
    cameraSelectedVar.set(None)
  }

  @js.annotation.JSExport
  def destroyQrScanner = {
    qrScanner.map(_.destroy())
    cameraSelectedVar.set(None)
  }

  val camerasVar = Var[Seq[Camera]](initial = Seq.empty)
  val cameraSelectedVar = Var[Option[Camera]](initial = None)

  def uiQRcodeScanner =
    div(
      onMountCallback { ctx =>
        initQrScanner
        // job(ctx.owner)
      },
      onUnmountCallback { ctx =>
        destroyQrScanner
      },
      div(
        child <-- camerasVar.signal.map { cameras =>
          select(
            value <-- cameraSelectedVar.signal.map(_.map(_.id).getOrElse("None")),
            onChange.mapToValue.map {
              case "None" =>
                stopQrScanner // side effect
                None
              case id =>
                startQrScanner(cameraId = Some(id))
                cameras.find(_.id == id)
            } --> cameraSelectedVar,
            cameras.map { camera => option(value := camera.id, camera.label) } :+ option(value := "None", "None")
          )
        }
      ),
      div(children <-- qrcodeVar.signal.map {
        case None       => Seq(p("No QRcode detected"))
        case Some(data) =>
          OutOfBand.oob(data) match
            case Left(value)   => Seq(p(s"Fail to parce OutOfBand Message due to: $value"))
            case Right(oobMsg) =>
              Seq(
                pre(code(oobMsg.msg.toJsonPretty)),
                p("Open with the ", b("OOB Tool "), MyRouter.navigateTo(MyRouter.OOBPage(oobMsg))),
              )
      }),
      div(videoContainer),
    )

  // ##################################################################################################################

  val invitation = OOBInvitation(
    from = AgentProvider.alice.id,
    goal_code = Some("request-mediate"),
    goal = Some("RequestMediate"),
    accept = Some(Seq("didcomm/v2")),
  )

  val oobBaseVar: Var[String] = Var(initial = Client.urlHost + "/#/")
  val dataVar: Var[String] = Var(initial = invitation.toPlaintextMessage.toJsonPretty)

  def uiMakeQRcode = div(
    p("Insert the Message below to create the QRcode of a OOB (out of band) Message."),
    p(" Message can be signed encrypted or plaintext, but plaintext is not recommended."),
    div(input(placeholder("Base link"), value <-- oobBaseVar, onInput.mapToValue --> oobBaseVar)),
    div(
      textArea(
        rows := 20,
        cols := 80,
        autoFocus(true),
        value <-- dataVar,
        onInput.mapToValue --> dataVar
      )
    ),
    children <-- dataVar.signal.combineWith(oobBaseVar.signal).map { (data, oobBase) =>
      data.fromJson[Message] match
        case Left(value) => Seq()
        case Right(msg)  =>
          val oob = OutOfBand.from(msg)
          val oobStr = oob.makeURI(oobBase)
          val divQRCode = div(width := "90%")
          def updateSVG = {
            implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
            typings.qrcode.mod
              .toCanvas(oobStr, typings.qrcode.mod.QRCodeRenderersOptions())
              .toFuture
              .onComplete {
                case scala.util.Success(value)                                   => divQRCode.ref.replaceChildren(value)
                case scala.util.Failure(js.JavaScriptException(error: js.Error)) =>
                  val info = p("Unable to create a QRcode becuase: " + error.message)
                  divQRCode.ref.replaceChildren(info.ref)
                case scala.util.Failure(exception) =>
                  exception.printStackTrace()
                  divQRCode.ref.replaceChildren("ERROR: " + exception.getMessage)
              }
          }
          updateSVG
          Seq(
            div(code(a(wordBreak.breakAll, oobStr, MyRouter.navigateTo(MyRouter.OOBPage(oob))))),
            divQRCode,
          )
    },
  )

  // ##################################################################################################################

  def apply(): HtmlElement = div(
    div(code("QrCode Tool")),
    div(
      p(
        "This tool is designed to help use QRcode as a transport for DID Comm.",
        "One example of QRcodes in the DID Comm specification is to use a Out Of Band link format to encode a message of the type ",
        code("https://didcomm.org/out-of-band/2.0/invitation"),
        "."
      )
    ),
    select(
      value <-- qrToolVar.signal.map(_.ordinal.toString), // .map(_.map(_.id).getOrElse("None")),
      onChange.mapToValue.map { e => QRTool.fromOrdinal(e.toInt) } --> qrToolVar,
      option(value := QRTool.Generator.ordinal.toString, QRTool.Generator.toString),
      option(value := QRTool.Scanner.ordinal.toString, QRTool.Scanner.toString)
    ),
    child <-- qrToolVar.signal.map {
      case QRTool.Generator =>
        stopQrScanner
        uiMakeQRcode
      case QRTool.Scanner =>
        startQrScanner()
        uiQRcodeScanner
    },
  )

}
