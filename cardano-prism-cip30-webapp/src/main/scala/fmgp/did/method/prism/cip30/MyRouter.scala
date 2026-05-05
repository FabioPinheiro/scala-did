package fmgp.did.method.prism.cip30

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import org.scalajs.dom
import upickle.default.*

object MyRouter:

  sealed trait Page
  case object HomePage extends Page
  case object SubmitPage extends Page
  case object SimulatePage extends Page

  given homeRW: ReadWriter[HomePage.type] = macroRW
  given submitRW: ReadWriter[SubmitPage.type] = macroRW
  given simulateRW: ReadWriter[SimulatePage.type] = macroRW
  given pageRW: ReadWriter[Page] = macroRW

  private val routes = List(
    Route.static(HomePage, root / "home" / endOfSegments, Router.localFragmentBasePath),
    Route.static(SubmitPage, root / "submit" / endOfSegments, Router.localFragmentBasePath),
    Route.static(SimulatePage, root / "simulate" / endOfSegments, Router.localFragmentBasePath),
    Route.static(HomePage, root / endOfSegments, Router.localFragmentBasePath),
  )

  val router: Router[Page] = new Router[Page](
    routes = routes,
    getPageTitle = {
      case HomePage     => "cardano-prism: playground"
      case SubmitPage   => "cardano-prism: submit"
      case SimulatePage => "cardano-prism: simulate"
    },
    serializePage = page => write(page),
    deserializePage = str => read[Page](str),
    routeFallback = _ => HomePage,
    popStateEvents = windowEvents(_.onPopState),
    owner = unsafeWindowOwner,
  )

  def navigateTo(page: Page): Binder[HtmlElement] = Binder { el =>
    val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]
    if isLinkElement then el.amend(href(router.absoluteUrlForPage(page)))
    (onClick
      .filter(ev => !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey)))
      .preventDefault
      --> (_ => router.pushState(page))).bind(el)
  }
