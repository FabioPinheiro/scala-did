package fmgp.webapp

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import typings.std.stdStrings.text
import typings.mermaid

import fmgp.did._
object Home {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val statementVar = Var[Option[Statement]](initial = None)
  def onStatementUpdate(statement: Statement): Unit = statementVar.set(Some(statement))

  // #####################################

  def hack = mermaid.mod.default

  def apply(): HtmlElement = // rootElement
    div(
      p("DID Comm examples and tooling"),
      p(
        "Navigate to ",
        b("DB"),
        " (only works for alice, bob and charlie)",
        MyRouter.navigateTo(MyRouter.AgentDBPage)
      ),
      p("Navigate to ", b("DID Resolver Tool "), MyRouter.navigateTo(MyRouter.ResolverPage)),
      p("Navigate to ", b("Encrypt Tool "), MyRouter.navigateTo(MyRouter.EncryptPage)),
      p("Navigate to ", b("Decrypt Tool "), MyRouter.navigateTo(MyRouter.DecryptPage)),
      p("Navigate to ", b("Basic Message "), MyRouter.navigateTo(MyRouter.BasicMessagePage)),
      p("Navigate to ", b("Trust Ping "), MyRouter.navigateTo(MyRouter.TrustPingPage)),
      br(),
      p("DIDs: "),
      div(child <-- statementVar.signal.map(e => getHtml(e)))
    )
  def getHtml(statement: Option[Statement], indent: Int = 0): ReactiveHtmlElement[HTMLElement] =
    div(className("mermaid"), statementToMermaid(statement), onMountCallback(ctx => { update }))

  def statementToMermaid(s: Option[Statement]): String =
    AgentProvider.usersGraph

  def update = {
    println("MermaidApp Update!!")
    // val config = mermaid.mermaidAPIMod.mermaidAPI.Config().setStartOnLoad(false)
    // mermaid.mod.default.initialize(config)
    mermaid.mod.default.init("div.mermaid")
  }
}
