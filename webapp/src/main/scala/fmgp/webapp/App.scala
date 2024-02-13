package fmgp.webapp

import scala.scalajs.js.annotation._

import scalajs.js
import org.scalajs.dom
import com.raquo.laminar.api.L._
import com.raquo.waypoint._

import MyRouter._
import com.raquo.airstream.ownership.ManualOwner

import fmgp.webapp.Home
import fmgp.did.DidExample
import org.scalajs.dom.ServiceWorkerRegistration
import scala.scalajs.js.JSON
import fmgp.webapp.AgentManagement
import org.scalajs.dom.ServiceWorkerRegistrationOptions

object App {

  val oobExample =
    "eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNTk5ZjM2MzgtYjU2My00OTM3LTk0ODctZGZlNTUwOTlkOTAwIiwiZnJvbSI6ImRpZDpleGFtcGxlOmFsaWNlIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJzdHJlYW1saW5lZC12cCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ"

  /** Alice */
  val didExample =
    "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"

  def main( /*args: Array[String]*/ ): Unit = {

    // This div, its id and contents are defined in index-fastopt.html and index-fullopt.html files
    lazy val container = dom.document.getElementById("app-container")

    lazy val appElement = {
      div(
        AppUtils.drawer(
          currentPage = MyRouter.router.currentPageSignal,
          linkPages = linkPages,
          wipLinkPages = wipLinkPages,
          deprecatedLinkPages = deprecatedLinkPages
        ),
        AppUtils.drawerScrim,
        AppUtils.topBarHeader(MyRouter.router.currentPageSignal.map {
          case p: HomePage.type => "DID Comm v2 - Playground"
          case p                => p.title
        }),
        mainTag(
          className("mdc-top-app-bar--fixed-adjust"),
          child <-- $selectedApp.signal
        )
      )
    }

    // Wait until the DOM is loaded, otherwise app-container element might not exist
    renderOnDomContentLoaded(container, appElement)

    // Register the service worker
    if (!js.isUndefined(dom.window.navigator.serviceWorker)) {
      println(s"Registering ServiceWorker (${fmgp.SettingsFromHTML.getModeInfo})")
      fmgp.SettingsFromHTML.serviceWorkerContainer
        .`then`((resp: ServiceWorkerRegistration) => {
          println(s"ServiceWorker registered successfully : ${JSON.stringify(resp)}")
        })
        .`catch`((err: Any) => println(s"service worker failed ${err}"))
    } else {
      println("ServiceWorker not there yet!")
    }

  }

  private val $selectedApp = SplitRender(MyRouter.router.currentPageSignal)
    .collectStatic(HomePage)(Home())
    .collectStatic(SettingsPage)(SandboxSettings())
    .collectSignal[OOBPage](page => OutOfBandTool(page))
    .collectStatic(QRcodePage)(QRcodeTool())
    .collectStatic(NFCScannerPage)(NFCScannerTool())
    .collectStatic(WebBluetoothPage)(WebBluetoothTool())
    .collectStatic(DiscordBotPage)(DiscordBotInfo())
    // .collectSignal[DocPage](page => Doc(page))
    .collectStatic(AgentManagementPage)(AgentManagement())
    // .collectStatic(AgentDBPage)(AgentDB())
    .collectStatic(AgentMessageStoragePage)(AgentMessageStorage())
    .collectSignal[ResolverPage](page => ResolverTool(page))
    .collectStatic(EncryptPage)(EncryptTool())
    .collectStatic(DecryptPage)(DecryptTool())
    .collectStatic(BasicMessagePage)(BasicMessageTool())
    .collectStatic(TrustPingPage)(TrustPingTool())
    .collectStatic(TapIntoStreamPage)(TapIntoStreamTool())
    .collectStatic(DAppStorePage)(DAppStore())

  private val linkPages: List[Page] = List(
    HomePage,
    SettingsPage,
    QRcodePage,
    NFCScannerPage,
    OOBPage(oobExample),
    ResolverPage(didExample),
    AgentManagementPage,
    AgentMessageStoragePage,
    EncryptPage,
    DecryptPage,
  )

  private val wipLinkPages: List[Page] = List(
    WebBluetoothPage,
    DiscordBotPage,
  )

  private val deprecatedLinkPages: List[Page] = List(
    TapIntoStreamPage,
    // AgentDBPage,
    // DocPage(Seq()),
    BasicMessagePage,
    TrustPingPage,
    // DAppStorePage,
  )

}
