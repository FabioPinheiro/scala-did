package fmgp.did.method.prism.cip30

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import com.raquo.laminar.api.L.*
import org.scalajs.dom

import zio.*
import zio.json.*

import _root_.proto.prism.{PrismBlock, PrismEvent, PrismObject, SignedPrismEvent}
import fmgp.did.method.prism.{DIDPrism, PrismStateInMemory, SSI}
import fmgp.did.method.prism.proto.{MaybeEvent, OP}

object Cip30App:

  /** UI state machine. */
  enum TxState:
    case Idle
    case Connecting(walletId: String)
    case Connected(walletId: String, api: js.Dynamic, networkId: Int, changeAddrHex: String)
    case Submitting(walletId: String, api: js.Dynamic)
    case Submitted(txHash: String, networkId: Int)
    case Failed(reason: String)

  def main(): Unit =
    val container = dom.document.getElementById("app-container")
    if container == null then
      dom.console.error("[Cip30App] missing #app-container in page")
      return

    val eventsHex = readInjectedEvents()
    val parsed    = parseEvents(eventsHex)

    parsed match
      case Left(err) =>
        renderOnDomContentLoaded(container, errorView(err))
      case Right(events) =>
        renderOnDomContentLoaded(container, mainView(events))

  // ---- DOM I/O ----------------------------------------------------------

  /** Reads the events the server injected as `window.PRISM_CIP30_EVENTS` -- a
    * JS array of hex strings, one per SignedPrismEvent.
    */
  private def readInjectedEvents(): List[String] =
    val raw = js.Dynamic.global.window.PRISM_CIP30_EVENTS
    if js.isUndefined(raw) || raw == null then Nil
    else raw.asInstanceOf[js.Array[String]].toList

  private def parseEvents(hexList: List[String]): Either[String, Seq[SignedPrismEvent]] =
    if hexList.isEmpty then Left("No PRISM events were provided by the server.")
    else
      val parsed = hexList.zipWithIndex.map { (hex, i) =>
        try Right(SignedPrismEvent.parseFrom(Cip30.hexToBytes(hex)))
        catch case t: Throwable => Left(s"Event #$i parse failed: ${t.getMessage}")
      }
      parsed.collectFirst { case Left(e) => e } match
        case Some(e) => Left(e)
        case None    => Right(parsed.collect { case Right(v) => v })

  // ---- views ------------------------------------------------------------

  private def errorView(msg: String): HtmlElement =
    div(
      cls := "error",
      h1("cardano-prism CIP-30 submitter"),
      p(s"Error: $msg"),
    )

  private def mainView(events: Seq[SignedPrismEvent]): HtmlElement =
    val state = Var[TxState](TxState.Idle)

    div(
      cls := "container",
      h1("cardano-prism: submit via browser wallet"),
      p(
        s"${events.size} PRISM event${if events.size == 1 then "" else "s"} ready to submit. ",
        "Connect a Cardano wallet (Lace / Eternl / Nami / Yoroi) and press Submit.",
      ),
      didsBlock(events),
      simulatedStateBlock(events),
      eventsBlock(events),
      walletBlock(state, events),
    )

  private def didsBlock(events: Seq[SignedPrismEvent]): HtmlElement =
    val dids = extractCreatedDIDs(events)
    if dids.isEmpty then
      div(cls := "section", h2("DIDs"), p(em("No `CreateDid` events in this submission.")))
    else
      div(
        cls := "section",
        h2(s"DID${if dids.size == 1 then "" else "s"} that will be created"),
        ul(dids.map(d => li(code(d.string)))),
      )

  private def simulatedStateBlock(events: Seq[SignedPrismEvent]): HtmlElement =
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
        _     <- ZIO.foreach(maybeEvents)(state.addMaybeEvent(_))
        ssis  <- state.makeSSI
      yield ssis
    Try(Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(program).getOrThrow() }).toEither.left
      .map(t => Option(t.getMessage).getOrElse(t.getClass.getSimpleName))

  private def didDocumentJson(ssi: SSI): String =
    Try(ssi.didDocument.map(_.toJsonPretty).getOrElse("(no DID Document)")) match
      case Success(s) => s
      case Failure(t) =>
        s"(DID Document not available in browser: ${Option(t.getMessage).getOrElse(t.getClass.getSimpleName)})"

  private def eventsBlock(events: Seq[SignedPrismEvent]): HtmlElement =
    div(
      cls := "section events",
      h2("PRISM events (hex)"),
      ul(
        events.zipWithIndex.map { (ev, i) =>
          li(
            strong(s"#$i"),
            " ",
            span(cls := "type", eventTypeLabel(ev)),
            detailsTag(
              summaryTag("show hex"),
              code(cls := "hex", Cip30.bytesToHex(ev.toByteArray)),
            ),
          )
        }
      ),
    )

  private def walletBlock(state: Var[TxState], events: Seq[SignedPrismEvent]): HtmlElement =
    val available = Cip30.available
    div(
      cls := "section wallet",
      h2("Wallet"),
      if available.isEmpty then
        p(em("No CIP-30 compatible wallet detected. Install Lace / Eternl / Nami / Yoroi."))
      else walletPicker(available, state, events),
      child <-- state.signal.map(stateView),
    )

  private def walletPicker(
      available: List[String],
      state: Var[TxState],
      events: Seq[SignedPrismEvent],
  ): HtmlElement =
    val selected = Var(available.head)
    div(
      cls := "picker",
      label(
        "Wallet:",
        select(
          onChange.mapToValue --> selected,
          available.map(id => option(value := id, walletDisplayName(id))),
        ),
      ),
      button(
        "Submit",
        onClick --> { _ =>
          val walletId = selected.now()
          state.set(TxState.Connecting(walletId))
          submitFlow(walletId, events, state)
        },
        disabled <-- state.signal.map {
          case TxState.Idle | TxState.Failed(_) | _: TxState.Connected => false
          case _                                                       => true
        },
      ),
    )

  private def stateView(s: TxState): HtmlElement = s match
    case TxState.Idle             => div()
    case TxState.Connecting(id)   =>
      p(cls := "status", s"Connecting to $id...")
    case TxState.Connected(id, _, networkId, _) =>
      p(cls := "status", s"Connected ($id) on ${networkLabel(networkId)}.")
    case TxState.Submitting(id, _) =>
      p(cls := "status", s"Building, signing & submitting via $id...")
    case TxState.Submitted(txHash, networkId) =>
      div(
        cls := "status success",
        p(s"Submitted on ${networkLabel(networkId)}."),
        p("Tx hash: ", code(txHash)),
        p(
          a(
            href := explorerUrl(networkId, txHash),
            target := "_blank",
            "Open in CardanoScan",
          )
        ),
        p(em("This window can be closed; the CLI process will exit shortly.")),
      )
    case TxState.Failed(reason) =>
      div(cls := "status error", p(s"Failed: $reason"))

  // ---- transaction flow -------------------------------------------------

  private def submitFlow(
      walletId: String,
      events: Seq[SignedPrismEvent],
      state: Var[TxState],
  ): Unit =
    import scala.concurrent.ExecutionContext.Implicits.global
    val flow =
      for
        api           <- stage("enable",           Cip30.enable(walletId))
        networkId     <- stage("getNetworkId",     Cip30.getNetworkId(api))
        changeAddrHex <- stage("getChangeAddress", Cip30.getChangeAddress(api))
        utxosHex      <- stage("getUtxos",         Cip30.getUtxos(api))
        _ = state.set(TxState.Connected(walletId, api, networkId, changeAddrHex))
        snap = MetadataTx.WalletSnapshot(
          networkId          = networkId,
          utxosCbor          = utxosHex.map(Cip30.hexToBytes),
          changeAddressBytes = Cip30.hexToBytes(changeAddrHex),
        )
        unsignedTx <- stageSync("buildPrismTx",
          MetadataTx.buildPrismTx(snap, events) match
            case Left(reason) => throw new RuntimeException(reason)
            case Right(tx)    => tx,
        )
        unsignedHex <- stageSync("encode unsignedTx", Cip30.bytesToHex(unsignedTx.toCbor))
        _           = state.set(TxState.Submitting(walletId, api))
        witnessHex  <- stage("signTx", Cip30.signTx(api, unsignedHex, partialSign = false))
        signed      <- stageSync("attachWitnesses",
          MetadataTx.attachWitnesses(unsignedTx, Cip30.hexToBytes(witnessHex)),
        )
        signedHex   <- stageSync("encode signedTx", Cip30.bytesToHex(signed.toCbor))
        txHash      <- stage("submitTx", Cip30.submitTx(api, signedHex))
      yield (txHash, networkId)

    flow.onComplete {
      case Success((txHash, networkId)) =>
        state.set(TxState.Submitted(txHash, networkId))
        notifyServer(txHash, networkId)
      case Failure(t) =>
        state.set(TxState.Failed(errorMessage(t)))
    }

  /** Wrap a Future so any failure is annotated with the stage name. */
  private def stage[A](label: String, f: Future[A]): Future[A] =
    import scala.concurrent.ExecutionContext.Implicits.global
    f.recoverWith { case t =>
      Future.failed(new RuntimeException(s"$label: ${errorMessage(t)}", t))
    }

  /** Same idea for synchronous steps that may throw. */
  private def stageSync[A](label: String, body: => A): Future[A] =
    try Future.successful(body)
    catch case t: Throwable => Future.failed(new RuntimeException(s"$label: ${errorMessage(t)}", t))

  private def notifyServer(txHash: String, networkId: Int): Unit =
    import scala.concurrent.ExecutionContext.Implicits.global
    val payload = s"""{"txHash":"$txHash","networkId":$networkId}"""
    val xhr = new dom.XMLHttpRequest()
    xhr.open("POST", "/done", async = true)
    xhr.setRequestHeader("Content-Type", "application/json")
    try xhr.send(payload)
    catch case t: Throwable => dom.console.warn(s"[Cip30App] /done POST failed: ${t.getMessage}")

  // ---- helpers ----------------------------------------------------------

  private def extractCreatedDIDs(events: Seq[SignedPrismEvent]): Seq[DIDPrism] =
    import fmgp.did.method.prism.proto.didPrism
    events.flatMap { signed =>
      signed.event.flatMap(_.didPrism.toOption)
    }

  private def eventTypeLabel(signed: SignedPrismEvent): String =
    signed.event match
      case None    => "(no event)"
      case Some(e) =>
        e.event match
          case PrismEvent.Event.CreateDid(_)              => "CreateDid"
          case PrismEvent.Event.UpdateDid(_)              => "UpdateDid"
          case PrismEvent.Event.DeactivateDid(_)          => "DeactivateDid"
          case PrismEvent.Event.ProtocolVersionUpdate(_)  => "ProtocolVersionUpdate"
          case PrismEvent.Event.CreateStorageEntry(_)     => "CreateStorageEntry"
          case PrismEvent.Event.UpdateStorageEntry(_)     => "UpdateStorageEntry"
          case PrismEvent.Event.DeactivateStorageEntry(_) => "DeactivateStorageEntry"
          case PrismEvent.Event.Empty                     => "(empty)"
          case other                                      => other.getClass.getSimpleName

  private def walletDisplayName(id: String): String =
    Cip30.info(id).map(i => if i.name.nonEmpty then s"${i.name} ($id)" else id).getOrElse(id)

  private def networkLabel(networkId: Int): String = networkId match
    case 1 => "mainnet"
    case 0 => "testnet (preprod / preview)"
    case n => s"network id $n"

  private def explorerUrl(networkId: Int, txHash: String): String = networkId match
    case 1 => s"https://cardanoscan.io/transaction/$txHash?tab=metadata"
    case _ => s"https://preprod.cardanoscan.io/transaction/$txHash?tab=metadata"

  private def errorMessage(t: Throwable): String =
    Option(t.getMessage).getOrElse(t.getClass.getSimpleName)
