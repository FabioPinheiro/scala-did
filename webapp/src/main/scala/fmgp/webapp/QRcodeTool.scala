package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import com.raquo.airstream.ownership._

import zio._
import zio.json._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.Payload.base64url
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.comm.protocol.oobinvitation.*
import fmgp.did.method.peer.DIDPeer._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.webapp.MyRouter.OOBPage
import com.raquo.laminar.nodes.ReactiveElement

import typings.qrScanner.mod.QrScanner
import typings.qrScanner.mod.QrScanner.Camera
import typings.qrScanner.mod.QrScanner.ScanResult
import typings.qrScanner.mod.{default as QrScannerDefault}
import typings.qrScanner.anon.CalculateScanRegion
import typings.qrcodeGenerator
import typings.qrcodeGenerator.anon.CellSize

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

  val videoContainer = videoTag()

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
    )
    qrScanner = Some(tmp) // side effect
    updateListOfCameras // side effect
    // startQrScanner() // side effect
  }

  @js.annotation.JSExport
  def startQrScanner(cameraId: Option[String] = None) = {
    (qrScanner, cameraId) match
      case (None, _) =>
      case (Some(scanner), None) =>
        scanner.start()
      case (Some(scanner), Some(id)) =>
        scanner.start()
        scanner.setCamera(id)
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

  def updateListOfCameras = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    QrScannerDefault.listCameras(true).toFuture.map(e => e.toSeq).map { c =>
      camerasVar.set(c) // side effect
      cameraSelectedVar.set(c.headOption) // side effect
    }
  }

  def uiQRcodeScanner =
    div(
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
      // div(videoContainer),
      div(children <-- qrcodeVar.signal.map {
        case None => Seq(p("No QRcode detected"))
        case Some(data) =>
          OutOfBand.oob(data) match
            case Left(value) => Seq(p(s"Fail to parce OutOfBand Message due to: $value"))
            case Right(oobMsg) =>
              Seq(
                pre(code(oobMsg.msg.toJsonPretty)),
                p("Open with the ", b("OOB Tool "), MyRouter.navigateTo(MyRouter.OOBPage(oobMsg))),
              )
      })
    )

  // ##################################################################################################################

  val invitation = OOBInvitation(
    from = AgentProvider.alice.id,
    goal_code = Some("request-mediate"),
    goal = Some("RequestMediate"),
    accept = Some(Seq("didcomm/v2")),
  )

  val oobBaseVar: Var[String] = Var(initial = "https://did.fmgp.app/#/")
  val dataVar: Var[String] = Var(initial = invitation.toPlaintextMessage.toJsonPretty)

  val divQRCode = div(width := "80%")

  // setup
  def updateSVG(data: String) = {
    val aux = qrcodeGenerator.mod.^.apply(qrcodeGenerator.TypeNumber.`0`, qrcodeGenerator.ErrorCorrectionLevel.L)
    aux.addData(data)
    aux.make()
    val cellSize = CellSize().setScalable(true)
    divQRCode.ref.innerHTML = aux.createSvgTag(cellSize)
  }

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
        case Right(msg) =>
          val oob = OutOfBand.from(msg)
          val oobStr = oob.makeURI(oobBase)
          updateSVG(oobStr)
          Seq(
            div(code(a(wordBreak.breakAll, oobStr, MyRouter.navigateTo(MyRouter.OOBPage(oob))))),
            divQRCode,
          )
    },
  )

  // ##################################################################################################################

  def apply(): HtmlElement = div(
    onMountCallback { ctx =>
      initQrScanner
      // job(ctx.owner)
    },
    onUnmountCallback { ctx =>
      destroyQrScanner
    },
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
    div(width := "80%", videoContainer)
  )

}
