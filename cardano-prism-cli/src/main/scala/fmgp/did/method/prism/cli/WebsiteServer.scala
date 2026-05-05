package fmgp.did.method.prism.cli

import zio.*
import zio.http.*
import zio.json.*

import java.nio.file.{Files, Paths}

import fmgp.util.bytes2Hex
import fmgp.did.method.prism.proto.didPrism
import proto.prism.SignedPrismEvent

/** ZIO-http server that serves the cardano-prism CIP-30 webapp.
  *
  * The webapp bundle is built by sbt (`sbt cardanoPrismCip30Webapp/cip30Bundle`) and embedded in this jar as
  * `cip30/bundle.js`. Set `CIP30_WEBAPP_BUNDLE` to a filesystem path to override the embedded copy during development.
  *
  * Two modes:
  *   - [[runUntilDone]]: one-shot. Injects events as `window.PRISM_CIP30_EVENTS`, registers a `/done` handler, and
  *     completes when the page POSTs the tx hash. Used by `submit-cip30`.
  *   - [[runForever]]: long-running. No events injected, no `/done` handler. Used by `open` and `simulate`. Runs until
  *     interrupted (Ctrl-C).
  */
object WebsiteServer:

  enum Landing:
    case Home, Submit, Simulate

  final case class TxResult(txHash: String, networkId: Int)
  object TxResult:
    given codec: JsonCodec[TxResult] = DeriveJsonCodec.gen[TxResult]

  private val BundleResource = "cip30/bundle.js"
  private val MapResource = "cip30/bundle.js.map"

  // ---- public entry points ----------------------------------------------

  /** One-shot. Completes when the browser page POSTs `/done`. */
  def runUntilDone(events: Seq[SignedPrismEvent], port: Int): ZIO[Any, Throwable, TxResult] =
    for
      bundleBytes <- loadBundle
      mapBytes <- loadMap
      _ <- ZIO.log(s"CIP-30 webapp bundle loaded (${bundleBytes.length} bytes)")
      done <- Promise.make[Throwable, TxResult]
      indexHtml = renderIndexHtml(events, Landing.Submit)
      routes = buildRoutes(bundleBytes, mapBytes, indexHtml, Some(done))
      url = s"http://localhost:$port"
      _ <- ZIO.log(s"Open $url in your browser to submit.")
      _ <- printDIDsToConsole(events)
      _ <- BrowserOpener.openUrl(url).forkDaemon
      _ <-
        Server
          .serve(routes)
          .provide(Server.defaultWithPort(port))
          .raceFirst(done.await)
      result <- done.await
    yield result

  /** Long-running. Serves the SPA at the given landing page until interrupted. */
  def runForever(landing: Landing, port: Int): ZIO[Any, Throwable, Nothing] =
    for
      bundleBytes <- loadBundle
      mapBytes <- loadMap
      _ <- ZIO.log(s"webapp bundle loaded (${bundleBytes.length} bytes)")
      indexHtml = renderIndexHtml(Seq.empty, landing)
      routes = buildRoutes(bundleBytes, mapBytes, indexHtml, None)
      url = s"http://localhost:$port${landingFragment(landing)}"
      _ <- ZIO.log(s"Serving $url — Ctrl-C to stop.")
      _ <- BrowserOpener.openUrl(url).forkDaemon
      result <- Server.serve(routes).provide(Server.defaultWithPort(port))
    yield result

  // ---- bundle loading ---------------------------------------------------

  private def loadBundle: Task[Array[Byte]] =
    loadResourceOrFile(BundleResource, required = true).map(_.get)

  private def loadMap: Task[Option[Array[Byte]]] =
    loadResourceOrFile(MapResource, required = false)

  /** Resolution order:
    *   1. `CIP30_WEBAPP_BUNDLE` env var (only honoured for the main bundle).
    *   2. Classpath resource at the given path.
    */
  private def loadResourceOrFile(
      resourcePath: String,
      required: Boolean,
  ): Task[Option[Array[Byte]]] =
    for
      override_ <-
        if resourcePath == BundleResource then System.env("CIP30_WEBAPP_BUNDLE")
        else ZIO.none
      bytes <- override_ match
        case Some(p) =>
          val path = Paths.get(p).toAbsolutePath
          ZIO
            .attempt(Files.readAllBytes(path))
            .map(Some(_))
            .tap(_ => ZIO.log(s"loaded bundle from filesystem: $path"))
        case None =>
          ZIO
            .attemptBlocking {
              Option(getClass.getClassLoader.getResourceAsStream(resourcePath))
                .map { in =>
                  try in.readAllBytes()
                  finally in.close()
                }
            }
            .flatMap {
              case Some(b)          => ZIO.some(b)
              case None if required =>
                ZIO.fail(
                  new RuntimeException(
                    s"""CIP-30 webapp bundle not found on classpath ($resourcePath).
                   |Rebuild it with:
                   |  sbt cardanoPrismCip30Webapp/cip30Bundle
                   |Or set CIP30_WEBAPP_BUNDLE to a filesystem path.""".stripMargin
                  )
                )
              case None => ZIO.none
            }
    yield bytes

  // ---- HTTP routing -----------------------------------------------------

  private def buildRoutes(
      bundleBytes: Array[Byte],
      mapBytes: Option[Array[Byte]],
      indexHtml: String,
      done: Option[Promise[Throwable, TxResult]],
  ): Routes[Any, Response] =
    val indexResponse = Response(
      status = Status.Ok,
      headers = Headers(Header.ContentType(MediaType.text.html)),
      body = Body.fromString(indexHtml),
    )
    val bundleResponse = Response(
      status = Status.Ok,
      headers = Headers(Header.ContentType(MediaType.text.javascript)),
      body = Body.fromArray(bundleBytes),
    )
    val mapResponse = mapBytes
      .map { bytes =>
        Response(
          status = Status.Ok,
          headers = Headers(Header.ContentType(MediaType.application.json)),
          body = Body.fromArray(bytes),
        )
      }
      .getOrElse(Response.status(Status.NotFound))

    val staticRoutes = Routes(
      Method.GET / Root -> handler(indexResponse),
      Method.GET / "index.html" -> handler(indexResponse),
      Method.GET / "bundle.js" -> handler(bundleResponse),
      Method.GET / "bundle.js.map" -> handler(mapResponse),
    )

    val doneRoute = done.map { d =>
      Routes(
        Method.POST / "done" -> handler { (req: Request) =>
          for
            body <- req.body.asString
            parsed = body.fromJson[TxResult]
            resp <- parsed match
              case Right(result) => d.succeed(result).as(Response.text("ok"))
              case Left(err)     => ZIO.succeed(Response.text(s"bad request: $err").status(Status.BadRequest))
          yield resp
        }
      )
    }

    val all = doneRoute.fold(staticRoutes)(staticRoutes ++ _)

    all.handleErrorRequestCauseZIO((_, cause) =>
      ZIO.succeed(
        Response
          .text(s"ERROR: ${cause.squash.getMessage}")
          .status(Status.InternalServerError)
      )
    )

  // ---- index.html -------------------------------------------------------

  private def landingFragment(landing: Landing): String = landing match
    case Landing.Home     => "#/"
    case Landing.Submit   => "#/submit"
    case Landing.Simulate => "#/simulate"

  private def renderIndexHtml(events: Seq[SignedPrismEvent], landing: Landing): String =
    val eventsScript =
      if events.isEmpty then ""
      else
        val arr = events.map(e => "\"" + bytes2Hex(e.toByteArray) + "\"").mkString("[", ",", "]")
        s"""<script>window.PRISM_CIP30_EVENTS = $arr;</script>"""
    val fragment = landingFragment(landing)
    val landingScript = s"""<script>if (!location.hash) location.hash = "$fragment";</script>"""
    s"""<!doctype html>
       |<html lang="en">
       |<head>
       |  <meta charset="utf-8" />
       |  <title>cardano-prism playground</title>
       |  <style>
       |    body { font-family: ui-sans-serif, system-ui, sans-serif; max-width: 760px; margin: 2rem auto; padding: 0 1rem; color: #222; }
       |    h1 { font-size: 1.5rem; }
       |    h2 { font-size: 1.1rem; margin-top: 2rem; }
       |    h3 { font-size: 1rem; margin: 0 0 0.3rem 0; }
       |    .nav { margin: 0 0 1.2rem 0; padding: 0.4rem 0; border-bottom: 1px solid #ddd; }
       |    .nav a { text-decoration: none; color: #06c; margin: 0 0.2rem; }
       |    .nav a:hover { text-decoration: underline; }
       |    .section { margin: 1.2rem 0; }
       |    .events code.hex { display: block; word-break: break-all; font-size: 0.8rem; background: #f4f4f4; padding: 0.5rem; border-radius: 4px; }
       |    .picker { display: flex; gap: 1rem; align-items: center; margin: 0.6rem 0; }
       |    .status { padding: 0.6rem 1rem; border-radius: 4px; background: #eef; }
       |    .status.success { background: #efe; }
       |    .status.error { background: #fee; }
       |    button { font-size: 1rem; padding: 0.4rem 0.9rem; cursor: pointer; }
       |    button[disabled] { opacity: 0.6; cursor: not-allowed; }
       |    .cards { display: flex; gap: 1rem; flex-wrap: wrap; margin-top: 1rem; }
       |    .card { display: block; flex: 1 1 280px; padding: 1rem; border: 1px solid #ddd; border-radius: 6px; text-decoration: none; color: inherit; }
       |    .card:hover { border-color: #06c; }
       |    .hex-input { width: 100%; font-family: ui-monospace, monospace; font-size: 0.85rem; }
       |    pre.json { background: #f4f4f4; padding: 0.6rem; border-radius: 4px; overflow-x: auto; font-size: 0.8rem; }
       |    .error { color: #b00; }
       |  </style>
       |  $eventsScript
       |  $landingScript
       |</head>
       |<body>
       |  <div id="app-container">Loading webapp...</div>
       |  <script type="module" src="/bundle.js"></script>
       |</body>
       |</html>
       |""".stripMargin

  private def printDIDsToConsole(events: Seq[SignedPrismEvent]): UIO[Unit] =
    val dids = events.flatMap(_.event.flatMap(_.didPrism.toOption))
    ZIO.foreachDiscard(dids)(d => Console.printLine(s"DID to be created: ${d.string}").orDie)
