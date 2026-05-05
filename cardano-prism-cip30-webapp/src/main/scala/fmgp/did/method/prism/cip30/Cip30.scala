package fmgp.did.method.prism.cip30

import scala.scalajs.js
import scala.concurrent.Future
import scala.scalajs.js.Thenable.Implicits.thenable2future

/** Thin js.Dynamic facade over `window.cardano` for CIP-30 wallet extensions (Nami, Eternl, Lace, ...). Each entry
  * exposes `enable()` returning the per-session API handle.
  */
object Cip30:

  final case class WalletInfo(
      id: String,
      name: String,
      icon: String,
      apiVersion: String,
      supportedExtensions: List[Int],
  )

  /** CIP-30 `DataSignature`: a CBOR-encoded `COSE_Sign1` plus the public key in `COSE_Key` form, both hex-encoded. See
    * CIP-8 for the COSE structure.
    */
  final case class DataSignature(signature: String, key: String)

  private def cardano: js.UndefOr[js.Dynamic] =
    val w = js.Dynamic.global.window
    if js.isUndefined(w) then js.undefined
    else
      val c = w.cardano
      if js.isUndefined(c) then js.undefined else c.asInstanceOf[js.UndefOr[js.Dynamic]]

  private def entry(id: String): Option[js.Dynamic] =
    cardano.toOption.flatMap { c =>
      val raw = c.selectDynamic(id)
      if js.isUndefined(raw) then None else Some(raw)
    }

  def available: List[String] =
    cardano.toOption match
      case None    => Nil
      case Some(c) =>
        val obj = c.asInstanceOf[js.Object]
        js.Object.keys(obj).toList.filter { k =>
          val v = c.selectDynamic(k)
          !js.isUndefined(v) && js.typeOf(v) == "object" && js.typeOf(v.enable) == "function"
        }

  def info(id: String): Option[WalletInfo] = entry(id).map { e =>
    def str(field: String): String =
      val v = e.selectDynamic(field)
      if js.isUndefined(v) then "" else v.toString
    WalletInfo(
      id = id,
      name = str("name"),
      icon = str("icon"),
      apiVersion = str("apiVersion"),
      supportedExtensions = readExtensions(e),
    )
  }

  private def readExtensions(e: js.Dynamic): List[Int] =
    val raw = e.selectDynamic("supportedExtensions")
    if js.isUndefined(raw) then Nil
    else
      val arr = raw.asInstanceOf[js.Array[js.Dynamic]]
      arr.toList.flatMap { ext =>
        val cip = ext.selectDynamic("cip")
        if js.isUndefined(cip) then None
        else
          val n = cip.asInstanceOf[Double]
          if n.isNaN then None else Some(n.toInt)
      }

  private val dAppName: String = "cardano-prism"

  private def withDappName[T](f: => js.Promise[js.Any]): Future[js.Any] =
    import scala.concurrent.ExecutionContext.Implicits.global
    val doc = js.Dynamic.global.document
    val previous = if js.isUndefined(doc.title) then "" else doc.title.toString
    doc.title = dAppName
    val fut: Future[js.Any] = f
    fut.andThen { case _ => doc.title = previous }

  private inline def log(args: Any*): Unit =
    js.Dynamic.global.console.log(args.map(_.asInstanceOf[js.Any])*)

  private inline def warn(args: Any*): Unit =
    js.Dynamic.global.console.warn(args.map(_.asInstanceOf[js.Any])*)

  private def traced[T](label: String, f: Future[T]): Future[T] =
    import scala.concurrent.ExecutionContext.Implicits.global
    log(s"[Cip30] $label ...")
    f.andThen {
      case scala.util.Success(v) => log(s"[Cip30] $label OK", v.asInstanceOf[js.Any])
      case scala.util.Failure(e) => warn(s"[Cip30] $label FAIL", e.asInstanceOf[js.Any])
    }

  def enable(id: String): Future[js.Dynamic] =
    import scala.concurrent.ExecutionContext.Implicits.global
    entry(id) match
      case None    => Future.failed(new RuntimeException(s"Wallet '$id' is not installed"))
      case Some(e) =>
        traced(
          s"enable($id)",
          withDappName(e.enable().asInstanceOf[js.Promise[js.Any]])
            .map(_.asInstanceOf[js.Dynamic]),
        )

  def getChangeAddress(api: js.Dynamic): Future[String] =
    import scala.concurrent.ExecutionContext.Implicits.global
    val p = api.getChangeAddress().asInstanceOf[js.Promise[js.Any]]
    val f: Future[js.Any] = p
    traced("getChangeAddress", f.map(_.toString))

  /** Network ID the wallet is configured for. `0 = testnet (preprod/preview)`, `1 = mainnet`. CIP-30 §3.
    */
  def getNetworkId(api: js.Dynamic): Future[Int] =
    import scala.concurrent.ExecutionContext.Implicits.global
    val raw = api.getNetworkId()
    log("[Cip30] getNetworkId raw =", raw.asInstanceOf[js.Any], "typeof:", js.typeOf(raw))
    val asPromise: js.Promise[js.Any] =
      if js.typeOf(raw) == "object" && !js.isUndefined(raw.asInstanceOf[js.Dynamic].`then`)
      then raw.asInstanceOf[js.Promise[js.Any]]
      else js.Promise.resolve[js.Any](raw.asInstanceOf[js.Any])
    val f: Future[js.Any] = asPromise
    traced("getNetworkId", f.map(_.asInstanceOf[Double].toInt))

  /** All UTxOs available to the wallet, as hex-CBOR `[input, output]` pairs. The wallet may return `null` (treated here
    * as an empty list) or an empty array if it has no funds. CIP-30 §3.
    */
  def getUtxos(api: js.Dynamic): Future[List[String]] =
    import scala.concurrent.ExecutionContext.Implicits.global
    val p = api.getUtxos().asInstanceOf[js.Promise[js.Any]]
    val f: Future[js.Any] = p
    traced(
      "getUtxos",
      f.map { raw =>
        val list =
          if raw == null || js.isUndefined(raw.asInstanceOf[js.Any]) then Nil
          else raw.asInstanceOf[js.Array[String]].toList
        log(s"[Cip30] getUtxos count=${list.size}")
        list
      },
    )

  /** Ask the wallet to sign a transaction. Returns the hex-CBOR `TransactionWitnessSet` (the wallet does not return a
    * full signed tx -- caller must merge witnesses with the body). `partialSign = false` is the conventional default.
    */
  def signTx(api: js.Dynamic, txCborHex: String, partialSign: Boolean = false): Future[String] =
    import scala.concurrent.ExecutionContext.Implicits.global
    log(s"[Cip30] signTx(${txCborHex.length / 2}B, partialSign=$partialSign)")
    traced(
      "signTx",
      withDappName(api.signTx(txCborHex, partialSign).asInstanceOf[js.Promise[js.Any]])
        .map(_.toString),
    )

  /** Submit a fully-witnessed tx through the wallet's backend. Returns the tx hash (hex). The wallet may show a final
    * confirmation popup, hence the `withDappName` wrap.
    */
  def submitTx(api: js.Dynamic, signedTxCborHex: String): Future[String] =
    import scala.concurrent.ExecutionContext.Implicits.global
    log(s"[Cip30] submitTx(${signedTxCborHex.length / 2}B)")
    traced(
      "submitTx",
      withDappName(api.submitTx(signedTxCborHex).asInstanceOf[js.Promise[js.Any]])
        .map(_.toString),
    )

  def bytesToHex(bytes: Array[Byte]): String =
    val sb = new StringBuilder(bytes.length * 2)
    var i = 0
    while i < bytes.length do
      val b = bytes(i) & 0xff
      sb.append(Character.forDigit(b >>> 4, 16))
      sb.append(Character.forDigit(b & 0x0f, 16))
      i += 1
    sb.toString

  def hexToBytes(hex: String): Array[Byte] =
    val clean = if hex.startsWith("0x") then hex.substring(2) else hex
    require(clean.length % 2 == 0, s"odd-length hex: ${clean.length} chars")
    val out = new Array[Byte](clean.length / 2)
    var i = 0
    while i < out.length do
      val hi = Character.digit(clean.charAt(i * 2), 16)
      val lo = Character.digit(clean.charAt(i * 2 + 1), 16)
      require(hi >= 0 && lo >= 0, s"invalid hex char near index ${i * 2}")
      out(i) = ((hi << 4) | lo).toByte
      i += 1
    out
