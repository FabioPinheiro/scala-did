package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers.*
import js.JSConverters.*

import com.raquo.laminar.api.L.*
import zio.*
import zio.json.*

import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.method.peer.DIDPeer.*
import fmgp.did.uniresolver.*
import com.raquo.airstream.ownership.*
import fmgp.crypto.error.*
import fmgp.did.method.peer.DIDPeer
import fmgp.did.method.hardcode.HardcodeResolver
import fmgp.webapp.MyRouter.ResolverPage
import fmgp.Utils

object ResolverTool {

  val didVar: Var[Option[DID]] = Var(initial = None)
  // val customVar: Var[String] = Var(initial = "")
  val didDocumentVar: Var[Either[String, DIDDocument]] = Var(initial = Left(""))

  def job(owner: Owner, resolverPagePageSignal: Signal[ResolverPage]) = Signal
    .combine(didVar, resolverPagePageSignal)
    .map { case (mDid, custom) =>
      def program = (
        for {
          fromto <- ZIO
            .fromOption(mDid)
            .map(e => FROMTO(e.string))
            .orElse(ZIO.fromEither(FROMTO.either(custom.did)))
          resolver <- ZIO.service[Resolver]
          doc <- resolver.didDocument(fromto)
        } yield (doc)
      ).mapBoth(
        errorInfo => didDocumentVar.update(_ => Left(errorInfo.toString)),
        doc => didDocumentVar.update(_ => Right(doc))
      ).provide(Global.resolverLayer)
      Utils.runProgram(program)
    }
    .observe(owner)

  def apply(resolverPagePageSignal: Signal[ResolverPage]): HtmlElement = div(
    onMountCallback { ctx =>
      job(ctx.owner, resolverPagePageSignal: Signal[ResolverPage])
      ()
    },
    code("DID Resolver Page"),
    p(
      "Agent: ",
      Global.makeSelectElementDID(didVar),
      " ",
      code(child.text <-- didVar.signal.map(_.map(_.string).getOrElse("custom")))
    ),
    div(child <-- didVar.signal.flatMap {
      case Some(agent) => Signal.fromValue(div())
      case None        =>
        resolverPagePageSignal.map(e =>
          div(
            p("Input the custom did"),
            input(
              placeholder("did:peer:..."),
              `type`.:=("textbox"),
              autoFocus(true),
              value := e.did,
              inContext { thisNode =>
                // Note: mapTo below accepts parameter by-name, evaluating it on every enter key press
                AppUtils.onEnterPress.mapTo(thisNode.ref.value) --> { data =>
                  MyRouter.router.pushState(MyRouter.ResolverPage(did = data))
                }
              }
            ),
          )
        )

    }),
    pre(
      code(
        child.text <-- didDocumentVar.signal.map {
          case Right(doc)      => doc.toJsonPretty
          case Left(errorInfo) => errorInfo
        }
      )
    ),
    button(
      "Copy to clipboard",
      onClick --> { _ =>
        Global.copyToClipboard(
          didDocumentVar.now() match
            case Right(doc)      => doc.toJson
            case Left(errorInfo) => errorInfo
        )
      }
    )
  )
}
