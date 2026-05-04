package fmgp.did.method.prism.cip30

import scala.scalajs.js

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.SplitRender
import org.scalajs.dom

object Cip30App:

  def main(): Unit =
    val container = dom.document.getElementById("app-container")
    if container == null then
      dom.console.error("[Cip30App] missing #app-container in page")
      return

    val injected = readInjectedEvents()
    renderOnDomContentLoaded(container, appView(injected))

  private def appView(injected: List[String]): HtmlElement =
    div(
      cls := "container",
      navBar(),
      child <-- SplitRender(MyRouter.router.currentPageSignal)
        .collectStatic(MyRouter.HomePage)(HomeView())
        .collectStatic(MyRouter.SubmitPage)(SubmitView(injected))
        .collectStatic(MyRouter.SimulatePage)(SimulateView())
        .signal,
    )

  private def navBar(): HtmlElement =
    navTag(
      cls := "nav",
      a(MyRouter.navigateTo(MyRouter.HomePage), "Home"),
      " | ",
      a(MyRouter.navigateTo(MyRouter.SubmitPage), "Submit via wallet"),
      " | ",
      a(MyRouter.navigateTo(MyRouter.SimulatePage), "Simulate"),
    )

  /** Reads the events the server injected as `window.PRISM_CIP30_EVENTS` -- a JS array of hex strings, one per
    * SignedPrismEvent. Returns Nil when nothing was injected (standalone use).
    */
  private def readInjectedEvents(): List[String] =
    val raw = js.Dynamic.global.window.PRISM_CIP30_EVENTS
    if js.isUndefined(raw) || raw == null then Nil
    else raw.asInstanceOf[js.Array[String]].toList
