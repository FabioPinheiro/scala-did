package fmgp.webapp

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.timers._
import js.JSConverters._

import com.raquo.laminar.api.L._
import com.raquo.airstream.ownership._

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2.BasicMessage
import fmgp.did.comm.protocol.oobinvitation.*
import fmgp.did.method.peer.DIDPeer._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.crypto.error._
import fmgp.webapp.MyRouter.OOBPage
import com.raquo.laminar.nodes.ReactiveElement

object OutOfBandTool {

  def apply(oobPageSignal: Signal[OOBPage]): HtmlElement = div(
    onMountCallback { ctx =>
      // job(ctx.owner)
      ()
    },
    code("OutOfBand Tool Page"),
    p(
      overflowWrap.:=("anywhere"),
      div(child <-- oobPageSignal.map { e =>
        div(
          p(
            overflowWrap.:=("anywhere"),
            pre(code(s"OOB data: '${e.query_oob}'"))
          ),
          input(
            placeholder("OOB data"),
            autoFocus(true),
            value := e.query_oob,
            inContext { thisNode =>
              // Note: mapTo below accepts parameter by-name, evaluating it on every enter key press
              AppUtils.onEnterPress.mapTo(thisNode.ref.value) --> { data =>
                MyRouter.router.pushState(MyRouter.OOBPage(data))
              }
            }
          ),
          (
            OutOfBand.safeBase64(e.query_oob) match
              case Left(value)                                   => value
              case Right(OutOfBand(msg: EncryptedMessage, data)) => pre(code(msg.toJsonPretty))
              case Right(OutOfBand(msg: SignedMessage, data))    => pre(code(msg.toJsonPretty))
              case Right(OutOfBand(msg: PlaintextMessage, data)) =>
                Seq(
                  p("Message:"),
                  pre(code(msg.toJsonPretty)),
                  msg.toOOBInvitation match
                    case Left(value) => p(s"The OBB is not a invitation due to: $value")
                    case Right(oobInvitation) =>
                      Seq(
                        p(
                          "This is a OutOfBand Invitation of the type '",
                          code(oobInvitation.goal_code),
                          "'",
                          oobInvitation.goal
                            .map(g =>
                              Seq(
                                textToTextNode(", this was the goal of: "),
                                code(g),
                                textToTextNode(".")
                              ): Modifier[ReactiveElement.Base]
                            )
                            .getOrElse(Modifier.empty),
                          "."
                        ),
                        oobInvitation.goal match
                          case None => p("goal code is missing")
                          case Some(goal) =>
                            if (OOBInvitation.wellKnowGoal(goal))
                              p("'", code(goal), "' is a well know goal")
                            else
                              p(
                                "'",
                                code(goal),
                                "' is a not a well know goal (this app is not prepared to executive goal"
                              )
                      ): Modifier[ReactiveElement.Base]
                  ,
                  msg.from
                    .map(from =>
                      p(
                        "Use the ",
                        b("DID Resolver Tool"),
                        s" on the sender 'from': ",
                        a(code(from.toDID.did), MyRouter.navigateTo(MyRouter.ResolverPage(from.toDID.did))),
                      )
                    )
                    .getOrElse(Modifier.empty),
                  msg.to.toSeq.flatten
                    .map(to =>
                      p(
                        "Use the ",
                        b("DID Resolver Tool"),
                        s" on the reciver 'to': ",
                        a(code(to.toDID.did), MyRouter.navigateTo(MyRouter.ResolverPage(to.toDID.did)))
                      )
                    ): Modifier[ReactiveElement.Base],
                )
          ),
        )
      })
    ),
  )

}
