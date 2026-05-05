package fmgp.did.method.prism.cip30

import scala.util.{Failure, Success, Try}

import com.raquo.laminar.api.L.*

import zio.*
import zio.json.*

import io.bullet.borer.Cbor

import _root_.proto.prism.{PrismBlock, PrismObject, SignedPrismEvent}
import fmgp.did.method.prism.{PrismStateInMemory, SSI}
import fmgp.did.method.prism.proto.MaybeEvent
import fmgp.did.method.prism.cardano.CardanoTransactionMetadataPrismCBOR

object SimulateView:

  def apply(): HtmlElement =
    val rawInput = Var[String]("")
    val result = Var[Option[Either[String, Seq[SignedPrismEvent]]]](None)

    div(
      cls := "view simulate",
      h1("Simulate PRISM events"),
      p(
        "Paste hex below — one ",
        code("SignedPrismEvent"),
        ", ",
        code("PrismObject"),
        ", or Cardano metadata-map (label 21325) per line. Mixing is allowed.",
      ),
      textArea(
        cls := "hex-input",
        rows := 12,
        cols := 80,
        placeholder := "Paste SignedPrismEvent / PrismObject / Cardano metadata-map hex, one per line",
        controlled(value <-- rawInput.signal, onInput.mapToValue --> rawInput),
      ),
      div(
        cls := "actions",
        button(
          "Simulate",
          onClick --> { _ => result.set(Some(parseInput(rawInput.now()))) },
        ),
        " ",
        button(
          "Clear",
          onClick --> { _ =>
            rawInput.set("")
            result.set(None)
          },
        ),
      ),
      child <-- result.signal.map {
        case None             => emptyNode
        case Some(Left(err))  => p(cls := "error", err)
        case Some(Right(evs)) => simulatedStateBlock(evs)
      },
    )

  /** Per-line auto-detect, tried in order:
    *   1. Cardano metadata-map CBOR (label 21325) — strict; fails fast when the bytes are not a PRISM-labelled metadata
    *      map.
    *   2. SignedPrismEvent protobuf hex.
    *   3. PrismObject protobuf hex (its inner block events are extracted).
    *
    * All accepted events from all lines are concatenated.
    */
  private def parseInput(raw: String): Either[String, Seq[SignedPrismEvent]] =
    val lines = raw.linesIterator.map(_.trim).filter(_.nonEmpty).toList
    if lines.isEmpty then Left("Paste at least one hex line.")
    else
      val parsed: List[Either[String, Seq[SignedPrismEvent]]] = lines.zipWithIndex.map { (hex, i) =>
        Try(Cip30.hexToBytes(hex)).toEither.left
          .map(t => s"Line ${i + 1}: invalid hex (${Option(t.getMessage).getOrElse(t.getClass.getSimpleName)})")
          .flatMap { bs =>
            tryMetadataCbor(bs)
              .orElse(Try(SignedPrismEvent.parseFrom(bs)).map(Seq(_)))
              .orElse(Try(PrismObject.parseFrom(bs)).map(po => po.blockContent.toSeq.flatMap(_.events)))
              .toEither
              .left
              .map(t =>
                s"Line ${i + 1}: not a SignedPrismEvent, PrismObject, or Cardano metadata-map " +
                  s"(${Option(t.getMessage).getOrElse(t.getClass.getSimpleName)})"
              )
          }
      }
      parsed.collectFirst { case Left(e) => e } match
        case Some(e) => Left(e)
        case None    => Right(parsed.collect { case Right(v) => v }.flatten)

  private def tryMetadataCbor(bs: Array[Byte]): Try[Seq[SignedPrismEvent]] =
    Try(Cbor.decode(bs).to[CardanoTransactionMetadataPrismCBOR].value)
      .map(meta => meta.toPrismObject.blockContent.toSeq.flatMap(_.events))

  /** Renders the result of running PrismStateInMemory over the given events. Used both by the Simulate page and by the
    * Submit page (as an inline preview).
    */
  def simulatedStateBlock(events: Seq[SignedPrismEvent]): HtmlElement =
    simulate(events) match
      case Left(reason) =>
        div(
          cls := "section",
          h2("Simulated state"),
          p(cls := "error", s"Simulation failed: $reason"),
        )
      case Right(ssis) if ssis.isEmpty =>
        div(
          cls := "section",
          h2("Simulated state"),
          p(em("No DID state to simulate (no CreateDid events).")),
        )
      case Right(ssis) =>
        div(
          cls := "section",
          h2(s"Simulated state (${ssis.size} DID${if ssis.size == 1 then "" else "s"})"),
          ul(
            ssis.map { ssi =>
              li(
                strong(code(ssi.did.string)),
                detailsTag(
                  summaryTag("show SSI"),
                  pre(cls := "json", ssi.toJsonPretty),
                ),
                detailsTag(
                  summaryTag("show DID Document"),
                  pre(cls := "json", didDocumentJson(ssi)),
                ),
              )
            }
          ),
        )

  private def simulate(events: Seq[SignedPrismEvent]): Either[String, Seq[SSI]] =
    val prismObject = PrismObject(blockContent = Some(PrismBlock(events = events)))
    val maybeEvents = MaybeEvent.fromProto(prismObject, "sim-tx", 0)
    val program =
      for
        state <- PrismStateInMemory.empty
        _ <- ZIO.foreach(maybeEvents)(state.addMaybeEvent(_))
        ssis <- state.makeSSI
      yield ssis
    Try(Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(program).getOrThrow() }).toEither.left
      .map(t => Option(t.getMessage).getOrElse(t.getClass.getSimpleName))

  private def didDocumentJson(ssi: SSI): String =
    Try(ssi.didDocument.map(_.toJsonPretty).getOrElse("(no DID Document)")) match
      case Success(s) => s
      case Failure(t) =>
        s"(DID Document not available in browser: ${Option(t.getMessage).getOrElse(t.getClass.getSimpleName)})"
