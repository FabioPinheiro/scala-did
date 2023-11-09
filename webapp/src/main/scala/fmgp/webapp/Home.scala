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

  def showPage(page: MyRouter.Page, message: String) =
    p("Navigate to ", b(message), MyRouter.navigateTo(page), page.makeI)

  def apply(): HtmlElement = // rootElement
    div(
      p("Sandbox for DID Comm v2.1"),
      showPage(MyRouter.SettingsPage, "Settings Page"),
      showPage(MyRouter.AgentManagementPage, "Agent Management"),
      showPage(MyRouter.AgentMessageStoragePage, "Agent Message Storage"),
      showPage(MyRouter.OOBPage(App.oobExample), "OOB Tool"),
      showPage(MyRouter.QRcodeScannerPage, "DIDComm over QRcode"),
      showPage(MyRouter.NFCScannerPage, "DIDComm over NFC"),
      showPage(MyRouter.ResolverPage(App.didExample), "DID Resolver Tool"),
      showPage(MyRouter.EncryptPage, "Encrypt Tool"),
      showPage(MyRouter.DecryptPage, "Decrypt Tool"),
      // p(
      //   "Navigate to ",
      //   b("Message DB"),
      //   " (only works for alice, bob and charlie)",
      //   MyRouter.navigateTo(MyRouter.AgentDBPage)
      // ),
      // showPage(MyRouter.AgentDBPage, ge("Agent Message DB"),
      br(),
      p("In developing:"),
      showPage(MyRouter.DiscordBotPage, "Discord Bot (with DIDComm capabilities)"),
      showPage(MyRouter.WebBluetoothPage, "DIDComm over Web Bluetooth"),
      br(),
      p("Deprecated:"),
      showPage(MyRouter.BasicMessagePage, "Basic Message"),
      showPage(MyRouter.TrustPingPage, "Trust Ping"),
      showPage(MyRouter.TapIntoStreamPage, "TapIntoStream Tool"),
      showPage(MyRouter.MediatorPage, "Mediator (Alice)"),
      showPage(MyRouter.DocPage, "Documentation for scala-did lib"),
      br(),
      p("Ideas for experiments and applications:"),
      ul(
        li("[Done] DID Comm over Websocket"),
        li("[WIP] DID Comm over Push API"),
        li("DID Comm Protocol to Bootstrap WebRTC"),
        li("DID Comm over WebRTC: This would be indeed very interesting"),
        li("DID Comm over Email (in top of a SMTP server): Prove the control/ownership of emails addresses"),
        li("DID Comm over Power Line: Smart meters"),
        li("DID Comm over Li-Fi: Vehicles communication, Broadcast news, Indoor positioning system"),
        li("DID Comm over Audio Waves"),
      ),
      br(),
      p("DIDs:"),
      div(child <-- statementVar.signal.map(e => getHtml(e)))
    )
  def getHtml(statement: Option[Statement], indent: Int = 0): ReactiveHtmlElement[HTMLElement] =
    div(className("mermaid"), statementToMermaid(statement), onMountCallback(ctx => { Global.update("div.mermaid") }))

  def statementToMermaid(s: Option[Statement]): String =
    AgentProvider.usersGraph

}
