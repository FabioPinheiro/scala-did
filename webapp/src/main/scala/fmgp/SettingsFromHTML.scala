package fmgp

import org.scalajs.dom

object SettingsFromHTML {
  def getMode = org.scalajs.dom.document
    .getElementsByTagName("meta")
    .find(_.getAttribute("name") == "mode")
    .map(_.getAttribute("content"))
  def getModeInfo = "running in " + getMode.getOrElse("unknown") + " mode"

  def serviceWorkerContainer = getMode match
    case Some("development") =>
      dom.window.navigator.serviceWorker.register(
        "/dev-sw.js?dev-sw",
        new dom.ServiceWorkerRegistrationOptions { `type` = org.scalajs.dom.WorkerType.module }
      )
    case _ => dom.window.navigator.serviceWorker.register("/sw.js")
}

// val globalObject: js.Dynamic = {
//   import js.Dynamic.{global => g}
//   if (js.typeOf(g.global) != "undefined" && (g.global.Object eq g.Object)) {
//     // Node.js environment detected
//     g.global
//   } else {
//     // In all other well-known environment, we can use the global `this`
//     js.special.fileLevelThis.asInstanceOf[js.Dynamic]
//   }
// }
