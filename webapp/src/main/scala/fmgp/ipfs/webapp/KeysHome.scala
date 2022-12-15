package fmgp.ipfs.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import zio._
import zio.json._

import fmgp.did._
import fmgp.crypto._
import com.raquo.laminar.CollectionCommand

object KeysHome {
  def onEnterPress = onKeyPress.filter(_.keyCode == dom.ext.KeyCode.Enter)

  val keyStoreVar: Var[KeyStore] = Var(initial = KeyStore(Set.empty))
  def childrenSignal: Signal[Seq[Node]] = keyStoreVar.signal.map(_.keys.toSeq.map(_.toJson).map(code(_)))

//   val commandBus = new EventBus[ChildrenCommand]
//   val commandStream = commandBus.events
// //   val countSignal = commandStream.foldLeft(initial = 0)(_ + 1)
//         button(
//           "Add a child",
//           onClick.map(_ => CollectionCommand.Append(span(s"Just another child"))) --> commandBus
//         ),

  private val commandObserver = Observer[String] { case str =>
    str.fromJson[PrivateKey] match
      case Left(error)   => dom.window.alert(s"Fail to parse key: $error")
      case Right(newKey) => keyStoreVar.update(ks => ks.copy(ks.keys + newKey))
  }

  val rootElement = div(
    code("Keys Page"),
    div(
      div(
        p(
          "You can use folowing website to ",
          a(href := "https://8gwifi.org/jwkfunctions.jsp", target := "_blank", "Generate Keys"),
          ": X25519; Ed25519; P-256; P-384; P-521; P-256K (secp256k1)",
        ),
      ),
      div(
        input(
          placeholder("Add new key (JWT format)"),
          autoFocus(true),
          inContext { thisNode =>
            // Note: mapTo below accepts parameter by-name, evaluating it on every enter key press
            onEnterPress.mapTo(thisNode.ref.value).filter(_.nonEmpty) -->
              commandObserver.contramap[String] { text =>
                thisNode.ref.value = "" // clear input
                text
              }
          }
        )
      )
    ),
    div(
      div(child.text <-- keyStoreVar.signal.map(_.keys.size).map(c => s"KeyStore (with $c keys):")),
      div(children <-- childrenSignal)
    ),
    table(
      tr(th("type"), th("isPointOnCurve"), th("Keys Id")),
      children <-- keyStoreVar.signal.map(
        _.keys.toSeq
          .map { key =>
            key match
              case k @ OKPPrivateKey(kty, crv, d, x, kid) =>
                tr(
                  td(code(kty.toString)),
                  td(code("N/A")),
                  td(pre(code(kid.getOrElse("missing")))),
                )
              case k @ ECPrivateKey(kty, crv, d, x, y, kid) =>
                tr(
                  td(code(kty.toString)),
                  td(code(k.isPointOnCurve)),
                  td(pre(code(kid.getOrElse("missing")))),
                )

          }
      ),
    )
  )
  def apply(): HtmlElement = rootElement
}
