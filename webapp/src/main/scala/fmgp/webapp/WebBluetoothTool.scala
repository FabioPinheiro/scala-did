package fmgp.webapp

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import typings.std.stdStrings.text
import typings.mermaid

import fmgp.did.*
object WebBluetoothTool {

  def apply(): HtmlElement = // rootElement
    div(
      p("WebBluetoothTool"),
      p("The Discord bot is this a WIP (2023-09-10)"),
      p(
        "Specs of ",
        a(
          href := "https://translate.google.pt/?sl=en&tl=pt&text=we%20did%20come%20capabilities&op=translate",
          "Web Bluetooth API"
        ),

        // chrome://flags/
        // Web Bluetooth
        // Enables the Web Bluetooth API on platforms without official support – Linux
        // #enable-web-bluetooth
      ),
    )
}
