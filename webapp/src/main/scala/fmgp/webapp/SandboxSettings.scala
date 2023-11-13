package fmgp.webapp

import org.scalajs.dom
import com.raquo.laminar.api.L._

import fmgp.ServiceWorkerUtils
import fmgp.did._
object SandboxSettings {

  def apply(): HtmlElement = // rootElement
    div(
      p("Sandbox Settings"),
      h2("Protocol handler (only work on desktop browsers)"),
      div(
        button(
          "Register URL protocol handler: web",
          onClick --> { _ =>
            val n = dom.window.navigator
            n.registerProtocolHandler("did", "#/resolver/%s")
            println("""registerProtocolHandler("did", "#/resolver/%s")""")
          },
        ),
        button(
          "Unregister URL protocol handler: web",
          onClick --> { _ =>
            val n = dom.window.navigator
            n.unregisterProtocolHandler("did", "#/resolver/%s")
            println("""unregisterProtocolHandler("did", "#/resolver/%s")""")
          },
        ),
        p(
          a(
            href := "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ",
            "Alice DID"
          ),
          nbsp,
          code("""<a href="did:peer:...">Alice DID</a>""")
        )
      ),
      div(
        button(
          "Register URL protocol handler: web+did",
          onClick --> { _ =>
            val n = dom.window.navigator
            n.registerProtocolHandler("web+did", "#/resolver/%s")
            println("""registerProtocolHandler("web+did", "#/resolver/%s")""")
          },
        ),
        button(
          "Unregister URL protocol handler: web+did",
          onClick --> { _ =>
            val n = dom.window.navigator
            n.unregisterProtocolHandler("web+did", "#/resolver/%s")
            println("""unregisterProtocolHandler("web+did", "#/resolver/%s")""")
          },
        ),
        p(
          a(
            href := "web+did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ",
            "Alice DID"
          ),
          nbsp,
          code("""<a href="web+did:peer:...">Alice DID</a>""")
        )
      ),
      // ### ServiceWorker ###
      h2("ServiceWorker"),
      div(
        button(
          "Register Service Worker",
          onClick --> { _ => ServiceWorkerUtils.registerServiceWorker },
        ),
        "Done by default"
      ),
      // ### Transport ###
      div(
        h2("Transport timeout"),
        p(
          "Timeout before auto disconnect websockets (in seconds): ",
          input(
            `type` := "number",
            stepAttr := "1",
            minAttr := "1",
            value <-- Global.transportTimeoutVar.signal.map(_.toString),
            onInput.mapToValue --> { _.toIntOption.foreach(Global.transportTimeoutVar.set(_)) },
          ),
        )
      ),
    )

}
