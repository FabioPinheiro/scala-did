package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import com.raquo.laminar.api.L._

import fmgp.ServiceWorkerUtils
import fmgp.SettingsFromHTML
import fmgp.did._
import fmgp.Config
import scala.util.Failure
import scala.util.Success

object SandboxSettings {

  def apply(): HtmlElement = // rootElement
    div(
      h1("Sandbox Settings"),
      p(s"This app run running in ${SettingsFromHTML.getModeInfo}"),
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
          onClick --> { _ => ServiceWorkerUtils.runRegisterServiceWorker },
        ),
        "Done by default"
      ),
      // ### Notifications ###
      h2("Notifications"),
      div(
        p(code(s"Subscribe with Public key '${Config.PushNotifications.applicationServerKey}'")),
        button(
          "Subscribe To Notifications",
          onClick --> { _ =>
            implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
            ServiceWorkerUtils
              .runSubscribeToNotifications(Config.PushNotifications.applicationServerKey)
              .onComplete { // side effect
                case Failure(exception)        => Console.err.print("Fail Subscribe To Notifications")
                case Success(pushSubscription) => Global.valuePushSubscriptionVar.set(Some(pushSubscription))
              }
          },
        ),
        p(
          code(
            child.text <-- Global.valuePushSubscriptionVar.signal.map {
              case None                   => "No PushSubscription"
              case Some(pushSubscription) => js.JSON.stringify(pushSubscription.toJSON())
            }
          )
        )
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
      // ### Generate Keys localy ###
      div(
        h2("Transport timeout"),
        p(
          "Use local KeyGenerator or Generate the keys remotaly",
          input(
            `type` := "checkbox",
            checked <-- Global.localKeyGeneratorVar.signal,
            onClick.mapToChecked --> Global.localKeyGeneratorVar,
          ),
          child <-- Global.localKeyGeneratorVar.signal.map(e =>
            if (e) "--> Generate Keys Localy (Working only in some browser)"
            else "--> Generate Keys Remotaly"
          )
        )
      ),
    )

}
