package fmgp.did.method.prism.cli

import zio.*
import zio.http.*
import zio.json.*

import java.nio.file.{Files, Paths}

import fmgp.util.bytes2Hex
import fmgp.did.method.prism.proto.didPrism
import proto.prism.SignedPrismEvent

/** ZIO-http server that drives the CIP-30 browser-wallet submission flow.
  *
  * The CIP-30 webapp is bundled by sbt and embedded in this jar as a classpath
  * resource at `cip30/bundle.js`. To rebuild from source:
  *
  *   sbt cardanoPrismCip30Webapp/cip30Bundle
  *
  * For development without rebuilding the whole jar, set `CIP30_WEBAPP_BUNDLE`
  * to a filesystem path and the server will load that file instead of the
  * embedded resource.
  *
  * The page reads the events from the injected `window.PRISM_CIP30_EVENTS`
  * global. When the wallet reports a tx hash, the page POSTs `/done` and the
  * server completes the `Promise[TxResult]` so the CLI can exit.
  */
object Cip30SubmitterServer {

  final case class TxResult(txHash: String, networkId: Int)
  object TxResult {
    given codec: JsonCodec[TxResult] = DeriveJsonCodec.gen[TxResult]
  }

  private val BundleResource = "cip30/bundle.js"
  private val MapResource    = "cip30/bundle.js.map"

  def run(
      events: Seq[SignedPrismEvent],
      port: Int,
  ): ZIO[Any, Throwable, TxResult] =
    for {
      bundleBytes <- loadBundle
      mapBytes    <- loadMap
      _           <- ZIO.log(s"CIP-30 webapp bundle loaded (${bundleBytes.length} bytes)")
      done        <- Promise.make[Throwable, TxResult]
      indexHtml = renderIndexHtml(events)
      routes    = buildRoutes(bundleBytes, mapBytes, indexHtml, done)
      _ <- ZIO.log(s"Open http://localhost:$port in your browser to submit.")
      _ <- printDIDsToConsole(events)
      _ <-
        Server
          .serve(routes)
          .provide(Server.defaultWithPort(port))
          .raceFirst(done.await)
      result <- done.await
    } yield result

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
    for {
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
          ZIO.attemptBlocking {
            Option(getClass.getClassLoader.getResourceAsStream(resourcePath))
              .map { in =>
                try in.readAllBytes()
                finally in.close()
              }
          }.flatMap {
            case Some(b) => ZIO.some(b)
            case None if required =>
              ZIO.fail(new RuntimeException(
                s"""CIP-30 webapp bundle not found on classpath ($resourcePath).
                   |Rebuild it with:
                   |  sbt cardanoPrismCip30Webapp/cip30Bundle
                   |Or set CIP30_WEBAPP_BUNDLE to a filesystem path.""".stripMargin
              ))
            case None => ZIO.none
          }
    } yield bytes

  private def buildRoutes(
      bundleBytes: Array[Byte],
      mapBytes: Option[Array[Byte]],
      indexHtml: String,
      done: Promise[Throwable, TxResult],
  ): Routes[Any, Response] = {
    val indexResponse =
      Response(
        status = Status.Ok,
        headers = Headers(Header.ContentType(MediaType.text.html)),
        body = Body.fromString(indexHtml),
      )
    val bundleResponse =
      Response(
        status = Status.Ok,
        headers = Headers(Header.ContentType(MediaType.text.javascript)),
        body = Body.fromArray(bundleBytes),
      )
    val mapResponse = mapBytes.map { bytes =>
      Response(
        status = Status.Ok,
        headers = Headers(Header.ContentType(MediaType.application.json)),
        body = Body.fromArray(bytes),
      )
    }.getOrElse(Response.status(Status.NotFound))

    Routes(
      Method.GET / Root            -> handler(indexResponse),
      Method.GET / "index.html"    -> handler(indexResponse),
      Method.GET / "bundle.js"     -> handler(bundleResponse),
      Method.GET / "bundle.js.map" -> handler(mapResponse),
      Method.POST / "done" -> handler { (req: Request) =>
        for {
          body <- req.body.asString
          parsed = body.fromJson[TxResult]
          resp <- parsed match
            case Right(result) =>
              done.succeed(result).as(Response.text("ok"))
            case Left(err) =>
              ZIO.succeed(
                Response.text(s"bad request: $err").status(Status.BadRequest)
              )
        } yield resp
      },
    ).handleErrorRequestCauseZIO((_, cause) =>
      ZIO.succeed(
        Response
          .text(s"ERROR: ${cause.squash.getMessage}")
          .status(Status.InternalServerError)
      )
    )
  }

  private def renderIndexHtml(events: Seq[SignedPrismEvent]): String = {
    val eventsJsArray = events
      .map(e => "\"" + bytes2Hex(e.toByteArray) + "\"")
      .mkString("[", ",", "]")
    s"""<!doctype html>
       |<html lang="en">
       |<head>
       |  <meta charset="utf-8" />
       |  <title>cardano-prism CIP-30 submitter</title>
       |  <style>
       |    body { font-family: ui-sans-serif, system-ui, sans-serif; max-width: 760px; margin: 2rem auto; padding: 0 1rem; color: #222; }
       |    h1 { font-size: 1.5rem; }
       |    h2 { font-size: 1.1rem; margin-top: 2rem; }
       |    .section { margin: 1.2rem 0; }
       |    .events code.hex { display: block; word-break: break-all; font-size: 0.8rem; background: #f4f4f4; padding: 0.5rem; border-radius: 4px; }
       |    .picker { display: flex; gap: 1rem; align-items: center; margin: 0.6rem 0; }
       |    .status { padding: 0.6rem 1rem; border-radius: 4px; background: #eef; }
       |    .status.success { background: #efe; }
       |    .status.error { background: #fee; }
       |    button { font-size: 1rem; padding: 0.4rem 0.9rem; cursor: pointer; }
       |    button[disabled] { opacity: 0.6; cursor: not-allowed; }
       |  </style>
       |  <script>window.PRISM_CIP30_EVENTS = $eventsJsArray;</script>
       |</head>
       |<body>
       |  <div id="app-container">Loading webapp...</div>
       |  <script type="module" src="/bundle.js"></script>
       |</body>
       |</html>
       |""".stripMargin
  }

  private def printDIDsToConsole(events: Seq[SignedPrismEvent]): UIO[Unit] = {
    val dids = events.flatMap(_.event.flatMap(_.didPrism.toOption))
    ZIO.foreachDiscard(dids)(d => Console.printLine(s"DID to be created: ${d.string}").orDie)
  }

}
