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

import typings.qrScanner.mod.QrScanner
import typings.qrScanner.mod.QrScanner.Camera
import typings.qrScanner.mod.QrScanner.ScanResult
import typings.qrScanner.mod.{default as QrScannerDefault}
import typings.qrScanner.anon.CalculateScanRegion
import fmgp.did.comm.Payload.base64url

@js.annotation.JSExportTopLevel("QRcodeScannerTool")
object QRcodeScannerTool {

  val qrcodeVar = Var[Option[String]](initial = None)

  val oobExtract = qrcodeVar.signal.map(_.flatMap { data =>
    OutOfBand.parse(data)
  })

  val oobMessage = qrcodeVar.signal.map(_.map { data =>
    println(data)
    OutOfBand.oob(data)
  })

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
    startQrScanner() // side effect
  }

  @js.annotation.JSExport
  def startQrScanner(cameraId: Option[String] = None) = {
    (qrScanner, cameraId) match
      case (None, _) =>
        print(s"Missing QrScanner") // DEBUG
      case (Some(scanner), None) =>
        print(s"Start Camera") // DEBUG
        scanner.start()
      case (Some(scanner), Some(id)) =>
        scanner.start()
        print(s"Select Camera id=$id") // DEBUG
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

  def apply(): HtmlElement = div(
    onMountCallback { ctx =>
      initQrScanner
      // job(ctx.owner)
    },
    onUnmountCallback { ctx =>
      destroyQrScanner
    },
    div(code("QrCode Scanner Tool Page")),
    div(
      child <-- camerasVar.signal.map { cameras =>
        select(
          value <-- cameraSelectedVar.signal.map(_.map(_.id).getOrElse("None")),
          onChange.mapToValue.map {
            case "None" =>
              println(s"Deselect Camera") // DEBUG
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
    div(videoContainer),
    div(child <-- qrcodeVar.signal.map {
      case None        => p("No QRcode detected")
      case Some(value) => p("QRcode:", code(value))
    }),
    div(child <-- oobExtract.map {
      case None        => p("Missing _oob")
      case Some(value) => p("_oob match ", code(value.urlBase64))
    }),
    div(children <-- oobMessage.map {
      case None              => Seq()
      case Some(Left(error)) => Seq(p("Parsing error", code(error)))
      case Some(Right(oobMsg)) =>
        Seq(
          pre(code(oobMsg.msg.toJsonPretty)),
          p("Open with the ", b("OOB Tool "), MyRouter.navigateTo(MyRouter.OOBPage(oobMsg))),
        )
    }),
  )

}
