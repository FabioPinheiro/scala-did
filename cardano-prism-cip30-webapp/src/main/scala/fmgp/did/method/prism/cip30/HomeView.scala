package fmgp.did.method.prism.cip30

import com.raquo.laminar.api.L.*

object HomeView:

  def apply(): HtmlElement =
    div(
      cls := "view home",
      h1("cardano-prism playground"),
      p("Local-only tools for inspecting and submitting PRISM events on Cardano."),
      div(
        cls := "cards",
        card(
          "Simulate",
          "Run PRISM events through an in-memory state and inspect the resulting SSI / DID Document. " +
            "Accepts SignedPrismEvent, PrismObject, or Cardano metadata-map (label 21325) hex.",
          MyRouter.SimulatePage,
        ),
        card(
          "Submit via wallet",
          "Sign and submit a prepared set of PRISM events using your CIP-30 wallet (Lace / Eternl / " +
            "Nami / Yoroi). Requires events to be injected by the CLI.",
          MyRouter.SubmitPage,
        ),
      ),
    )

  private def card(title: String, body: String, target: MyRouter.Page): HtmlElement =
    a(
      cls := "card",
      MyRouter.navigateTo(target),
      h3(title),
      p(body),
    )
