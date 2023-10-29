package fmgp.did.demo

import zio._
import zio.json._
import zio.stream._
import zio.http._

import scala.io.Source

import laika.api._
import laika.format._
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.did.method.DidPeerUniresolverDriver
import fmgp.did.method.peer.DidPeerResolver
import fmgp.crypto.Curve
import fmgp.crypto.KeyGenerator
import fmgp.util.MiddlewareUtils
// import zio.http.endpoint.RoutesMiddleware
import java.io.File
import java.io.FileNotFoundException
import zio.http.Header.ContentType
import zio.http.MediaTypes

/** demoJVM/runMain fmgp.did.demo.AppServer
  *
  * curl localhost:8080/hello
  *
  * curl 'http://localhost:8080/db' -H "host: alice.did.fmgp.app"
  *
  * wscat -c ws://localhost:8080/ws
  *
  * curl -X POST localhost:8080 -H "host: alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json' -d
  * '{}'
  *
  * curl
  * localhost:8080/resolver/did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.Vz6MksEtp5uusk11aUuwRHzdwfTxJBUaKaUVVXwFSVsmUkxKF.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTA5My8iLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19
  */
object AppServer extends ZIOAppDefault {

  def mdocMarkdown = Routes(
    Method.GET / "mdoc" / string("path") -> handler { (path: String, req: Request) => path }
      .flatMap(path => Handler.fromResource(s"$path")),
  ).sandbox.toHttpApp

  def mdocHTML = Routes(
    Method.GET / "doc" / string("path") -> handler { (path: String, req: Request) => path }
      .flatMap { path =>
        val transformer = Transformer
          .from(Markdown)
          .to(HTML)
          .using(GitHubFlavor, SyntaxHighlighting)
          .build

        Handler.fromResource(s"$path").mapZIO {
          _.body.asString.map { data =>
            val result = transformer.transform(data) match
              case Left(value)  => value.message
              case Right(value) => value
            Response.html(result)
          }
        }
      }
  )

  def appTest = Routes(
    // Method.GET / "text" -> handler(ZIO.succeed(Response.text("Hello World!"))),
    Method.GET / "headers" -> handler { (req: Request) =>
      val data = req.headers.toSeq.map(e => (e.headerName, e.renderedValue))
      Response.text("HEADERS:\n" + data.mkString("\n") + "\nRemoteAddress:" + req.remoteAddress)
    },
    Method.GET / "hello" -> handler(Response.text("Hello World! DEMO DID APP")),
    Method.GET / "health" -> handler(Response.ok),
    // http://localhost:8080/oob?_oob=eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNTk5ZjM2MzgtYjU2My00OTM3LTk0ODctZGZlNTUwOTlkOTAwIiwiZnJvbSI6ImRpZDpleGFtcGxlOnZlcmlmaWVyIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJzdHJlYW1saW5lZC12cCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ
  ).toHttpApp @@ (Middleware.requestLogging(loggedRequestHeaders = Set(Header.Host, Header.Origin)) ++ Middleware.debug)

  def appOther = Routes(
    Method.GET / "oob" -> handler { (req: Request) =>
      for {
        _ <- ZIO.log("oob")
        ret <- ZIO.succeed(OutOfBand.oob(req.url.encode) match
          case Left(error)                          => Response.text(error).copy(status = Status.BadRequest)
          case Right(OutOfBandPlaintext(msg, data)) => Response.json(msg.toJsonPretty)
          case Right(OutOfBandSigned(msg, data))    => Response.json(msg.payload.content)
        )
      } yield (ret)
    },
    Method.POST / "ops" -> handler { (req: Request) =>
      req.body.asString
        .tap(e => ZIO.log("ops"))
        .tap(e => ZIO.logTrace(s"ops: $e"))
        .flatMap(e => OperationsServerRPC.ops(e))
        .map(e => Response.text(e))
    },

    // ### MAKE KEYS ###
    Method.POST / "makeKey" -> handler { (req: Request) =>
      req.body.asString
        .tap(e => ZIO.log("makeKey"))
        .tap(e => ZIO.logTrace(s"makeKey: $e"))
        .flatMap(e =>
          e.fromJson[Curve] match
            case Left(parseError)     => ZIO.fail(DidException(CryptoFailToParse(parseError)))
            case Right(Curve.X25519)  => KeyGenerator.makeX25519.mapError(e => DidException(e))
            case Right(Curve.Ed25519) => KeyGenerator.makeEd25519.mapError(e => DidException(e))
            case Right(curve) =>
              ZIO.fail(DidException(WrongCurve(obtained = curve, expected = Set(Curve.X25519, Curve.Ed25519))))
        )
        .map(e => Response.text(e.toJson))
    },
    Method.GET / "makeKey" / "X25519" -> handler { (req: Request) =>
      KeyGenerator.makeX25519
        .mapError(e => DidException(e))
        .map(e => Response.text(e.toJson))
    },
    Method.GET / "makeKey" / "Ed25519" -> handler { (req: Request) =>
      KeyGenerator.makeEd25519
        .mapError(e => DidException(e))
        .map(e => Response.text(e.toJson))
    },
    Method.GET / "resolver" / string("did") -> handler { (did: String, req: Request) =>
      DIDSubject.either(did) match
        case Left(error)  => ZIO.succeed(Response.text(error.error).copy(status = Status.BadRequest)).debug
        case Right(value) => ZIO.succeed(Response.text("DID:" + value)).debug
    },
  ).sandbox.toHttpApp

  def appWebsite = Routes(
    // Method.GET / trailing -> handler { // html.Html.fromDomElement()
    //   val data = Source.fromResource(s"index.html").mkString("")
    //   ZIO.log("index.html") *> ZIO.succeed(Response.html(data))
    // },
    Method.GET / trailing -> Handler
      .fromResource("index.html")
      .map(_.setHeaders(Headers(Header.ContentType(MediaType.text.html)))),
    Method.GET / "index.html" -> handler { Response.redirect(URL.root) },
    Method.GET / "favicon.ico" -> Handler.fromResource("favicon.ico"),
    Method.GET / "manifest.webmanifest" -> Handler.fromResource("manifest.webmanifest"),
    Method.GET / "sw.js" -> Handler
      .fromResource("sw.js")
      .map(_.setHeaders(Headers(Header.ContentType(MediaType.application.javascript)))),
    Method.GET / "webapp.js" -> Handler
      .fromResource("webapp.js")
      .map(_.setHeaders(Headers(Header.ContentType(MediaType.application.javascript)))),
    Method.GET / "assets" -> handler { (req: Request) =>
      import zio.http.template._
      Response
        .html(
          ul( // Custom UI to list all the files in the directory
            (li(a(href := "..", "..")) +: Source
              .fromResource("assets")
              .getLines()
              .map { file => li(a(href := file, file)): Html }
              .toSeq): _*
          )
        )
    },
    Method.GET / "assets" / string("path") -> handler { (path: String, req: Request) =>
      // RoutesMiddleware
      // TODO https://zio.dev/reference/stream/zpipeline/#:~:text=ZPipeline.gzip%20%E2%80%94%20The%20gzip%20pipeline%20compresses%20a%20stream%20of%20bytes%20as%20using%20gzip%20method%3A
      val fullPath = s"assets/$path"
      val classLoader = Thread.currentThread().getContextClassLoader()
      val headerContentType = fullPath match
        case s if s.endsWith(".html") => Header.ContentType(MediaType.text.html)
        case s if s.endsWith(".js")   => Header.ContentType(MediaType.text.javascript)
        case s if s.endsWith(".css")  => Header.ContentType(MediaType.text.css)
        case s                        => Header.ContentType(MediaType.text.plain)
      Handler.fromResource(fullPath).map(_.addHeader(headerContentType))
    }.flatten
  ).sandbox.toHttpApp

  val app: HttpApp[Hub[String] & AgentByHost & Operations & MessageDispatcher & DidPeerResolver] = (
    AgentWithSocketManager.didCommApp ++ appTest ++ mdocMarkdown ++ appOther ++ appWebsite ++ DidPeerUniresolverDriver.resolverPeer
  ) @@ (Middleware.cors) // ++ MiddlewareUtils.all)

  override val run = for {
    _ <- Console.printLine(
      """██████╗ ██╗██████╗     ██████╗ ███████╗███╗   ███╗ ██████╗
        |██╔══██╗██║██╔══██╗    ██╔══██╗██╔════╝████╗ ████║██╔═══██╗
        |██║  ██║██║██║  ██║    ██║  ██║█████╗  ██╔████╔██║██║   ██║
        |██║  ██║██║██║  ██║    ██║  ██║██╔══╝  ██║╚██╔╝██║██║   ██║
        |██████╔╝██║██████╔╝    ██████╔╝███████╗██║ ╚═╝ ██║╚██████╔╝
        |╚═════╝ ╚═╝╚═════╝     ╚═════╝ ╚══════╝╚═╝     ╚═╝ ╚═════╝
        |Yet another server simpler server to demo DID Comm v2.
        |Vist: https://github.com/FabioPinheiro/scala-did""".stripMargin
    )
    _ <- ZIO.log(s"DID DEMO APP. See https://github.com/FabioPinheiro/scala-did")
    myHub <- Hub.sliding[String](5)
    _ <- ZStream.fromHub(myHub).run(ZSink.foreach((str: String) => ZIO.logInfo("HUB: " + str))).fork
    pord <- System
      .property("PORD")
      .flatMap {
        case None        => System.property("pord")
        case Some(value) => ZIO.succeed(Some(value))
      }
      .map(_.flatMap(_.toBooleanOption).getOrElse(false))
    port <- System
      .property("PORT")
      .flatMap {
        case None        => System.property("port")
        case Some(value) => ZIO.succeed(Some(value))
      }
      .map(_.flatMap(_.toIntOption).getOrElse(8080))
    _ <- ZIO.log(s"Starting server on port: $port")
    client = Scope.default ++ Client.default
    inboundHub <- Hub.bounded[String](5)
    myServer <- Server
      .serve(app)
      .provideSomeLayer(DidPeerResolver.layerDidPeerResolver)
      .provideSomeLayer(AgentByHost.layer)
      .provideSomeLayer(Operations.layerDefault)
      .provideSomeLayer(client >>> MessageDispatcherJVM.layer)
      .provideSomeEnvironment { (env: ZEnvironment[Server]) => env.add(myHub) }
      .provide(Server.defaultWithPort(port))
      // .provide(
      //   Server.defaultWith(
      //     _.port(port)
      //       .responseCompression(
      //         Server.Config.ResponseCompressionConfig.default
      //           // Server.Config.ResponseCompressionConfig.config
      //           // Server.Config.ResponseCompressionConfig(0, IndexedSeq(Server.Config.CompressionOptions.gzip()))
      //       )
      //   )
      // )
      .debug
      .fork
    _ <- ZIO.log(s"Server Started")
    _ <- myServer.join *> ZIO.log(s"Server End")
  } yield ()

}
