package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import com.raquo.laminar.api.L._

import fmgp.ServiceWorkerUtils
import fmgp.SettingsFromHTML
import fmgp.Utils
import fmgp.Config
import fmgp.did._
import fmgp.crypto._
import scala.util.Failure
import scala.util.Success
import zio._
import zio.json._
import fmgp.did.uniresolver.Uniresolver

object SandboxSettings {
  val keyTest: Var[Option[OKPPrivateKey]] = Var(initial = None)

  def newKeyX25519 = Utils.runProgram(Client.newKeyX25519Local.map(k => keyTest.set(Some(k))))
  def newKeyEd25519 = Utils.runProgram(Client.newKeyEd25519Local.map(k => keyTest.set(Some(k))))

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
      // Revolver
      h2("Prism DID Revolver"),
      p(
        "Choose the URL to be used for fetching the Blockchain Cardano data related with DID PRISM (transactions with the metadata label '21325' - CIP 10)"
      ),
      div(
        input(
          placeholder("https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/diddoc"),
          `type`.:=("textbox"),
          autoFocus(true),
          value <-- Global.baseUrlForDIDPrismResolverVar,
          inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> Global.baseUrlForDIDPrismResolverVar }
        ),
      ),
      ul(
        li(
          div(
            p("Official 'mainnet' - VDR data from the Cardano that follow the DID PRISM specs"),
            ul(
              li(
                a(
                  href := "https://github.com/blockfrost/prism-vdr/tree/main/mainnet/diddoc",
                  "https://raw.githubusercontent.com/blockfrost/prism-vdr/refs/heads/main/mainnet/diddoc"
                )
              ),
              li(
                a(
                  href := "https://github.com/FabioPinheiro/prism-vdr/tree/main/mainnet/diddoc",
                  "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/mainnet/diddoc"
                )
              ),
            )
          )
        ),
        li(
          div(
            p(
              "Unofficial 'preprod' - VDR data from the Cardano 'preprod' that follow the DID PRISM protocol but using 'preprod' network as the source of truth - For testing purpose"
            ),
            ul(
              li(
                a(
                  href := "https://github.com/blockfrost/prism-vdr/tree/main/preprod/diddoc",
                  "https://raw.githubusercontent.com/blockfrost/prism-vdr/refs/heads/main/preprod/diddoc"
                )
              ),
              li(
                a(
                  href := "https://github.com/FabioPinheiro/prism-vdr/tree/main/preprod/diddoc",
                  "https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/preprod/diddoc"
                )
              ),
            )
          )
        ),
        li(
          div(
            p(
              "Unofficial 'preview' -  VDR data from the Cardano 'preview' that  follow the DID PRISM protocol but using 'preview' network as the source of truth - Currently empty"
            ),
            ul(
              li(
                a(
                  href := "https://github.com/FabioPinheiro/prism-vdr/tree/main/preview/diddoc",
                  "https://raw.githubusercontent.com/blockfrost/prism-vdr/refs/heads/main/preview/diddoc"
                ),
              )
            )
          )
        )
      ),
      h2("Uniresolver"),
      p(
        "Choose the URL to be used for fetching DIDs from the Uniresolver"
      ),
      div(
        input(
          placeholder(Uniresolver.defaultEndpoint),
          `type`.:=("textbox"),
          autoFocus(true),
          value <-- Global.urlForUniresolverVar,
          inContext { thisNode => onInput.map(_ => thisNode.ref.value) --> Global.urlForUniresolverVar }
        ),
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
                case Failure(exception)        => scala.Console.err.print("Fail Subscribe To Notifications")
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
        h2("Generate Keys localy"),
        p(
          a(
            href := "https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto",
            b("[Chrome] "),
            "From version 113: "
          ),
          "this feature is behind the ",
          b("#enable-experimental-web-platform-features"),
          " preference (needs to be set to Enabled). " +
            "To change preferences in Chrome, visit chrome://flags. ",
        ),
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
        ),
        button("Generate Keys Localy X25519", onClick --> { (_) => newKeyX25519 }),
        button("Generate Keys Localy Ed25519", onClick --> { (_) => newKeyEd25519 }),
        div(children <-- keyTest.signal.map(_.map(e => code(e.toJson)).toSeq)),
      ),
    )

}
