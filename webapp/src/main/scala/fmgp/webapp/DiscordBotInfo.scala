package fmgp.webapp

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import typings.std.stdStrings.text
import typings.mermaid

import fmgp.did.*
object DiscordBotInfo {

  def apply(): HtmlElement = // rootElement
    div(
      p("Discord Bot"),
      p("The Discord bot is this a WIP (2023-11-05)"),
      p(
        "The Bot id is ",
        code("1170442329883168788"),
        ". ",
        a(
          href := "https://discord.com/api/oauth2/authorize?client_id=1170442329883168788&permissions=8&scope=bot",
          "Click here to add the Bot"
        ),
        ". The permission need to be refine (is Administrator for now)."
      ),
      p("Capabilities:"),
      ul(
        li("[WIP] As a Discord user proved that you control the DID"),
        li("[TODO] Setup Discord ROLEs based on DID Comm interactions"),
        li("[TODO] As a DID get the Verifiable Credentials that you control a Discord user"),
        li("[TODO] Setup Discord ROLEs based on Verifiable Credentials"),
      )
    )

}
