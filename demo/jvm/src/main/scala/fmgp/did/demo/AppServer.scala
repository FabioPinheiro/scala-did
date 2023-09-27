package fmgp.did.demo

import zio._
import zio.json._
import zio.stream._
import zio.http._
import zio.http.ZClient.ClientLive

import scala.io.Source

import laika.api._
import laika.format._
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.mediator._
import fmgp.did.comm.protocol._
import fmgp.did.method.DidPeerUniresolverDriver
import fmgp.did.method.peer.DidPeerResolver
import fmgp.crypto.Curve
import fmgp.crypto.KeyGenerator
import fmgp.util.MiddlewareUtils
import zio.http.endpoint.RoutesMiddleware
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
  * wscat -c ws://localhost:8080 --host "alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json'
  *
  * curl -X POST localhost:8080 -H "host: alice.did.fmgp.app" -H 'content-type: application/didcomm-encrypted+json' -d
  * '{}'
  *
  * curl
  * localhost:8080/resolver/did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.Vz6MksEtp5uusk11aUuwRHzdwfTxJBUaKaUVVXwFSVsmUkxKF.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTA5My8iLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19
  */
object AppServer extends ZIOAppDefault {

  val mdocMarkdown = Http.collectHttp[Request] { case req @ Method.GET -> Root / "mdoc" / path =>
    Http.fromResource(s"$path")
  }

  val mdocHTML = Http.collectHttp[Request] { case req @ Method.GET -> Root / "doc" / path =>
    val transformer = Transformer
      .from(Markdown)
      .to(HTML)
      .using(GitHubFlavor, SyntaxHighlighting)
      .build

    Http.fromResource(s"$path").mapZIO {
      _.body.asString.map { data =>
        val result = transformer.transform(data) match
          case Left(value)  => value.message
          case Right(value) => value
        Response.html(result)
      }
    }
  }

  val app: HttpApp[ // type HttpApp[-R, +Err] = Http[R, Err, Request, Response]
    Hub[String] & AgentByHost & Operations & MessageDispatcher & DidPeerResolver,
    Throwable
  ] = MediatorMultiAgent.didCommApp ++ Http
    .collectZIO[Request] {
      case req @ Method.GET -> Root / "headers" =>
        val data = req.headers.toSeq.map(e => (e.headerName, e.renderedValue))
        ZIO.succeed(Response.text("HEADERS:\n" + data.mkString("\n") + "\nRemoteAddress:" + req.remoteAddress)).debug
      case Method.GET -> Root / "hello"        => ZIO.succeed(Response.text("Hello World! DEMO DID APP")).debug
      case req @ Method.GET -> Root / "health" => ZIO.succeed(Response.ok)
      // http://localhost:8080/oob?_oob=eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNTk5ZjM2MzgtYjU2My00OTM3LTk0ODctZGZlNTUwOTlkOTAwIiwiZnJvbSI6ImRpZDpleGFtcGxlOnZlcmlmaWVyIiwiYm9keSI6eyJnb2FsX2NvZGUiOiJzdHJlYW1saW5lZC12cCIsImFjY2VwdCI6WyJkaWRjb21tL3YyIl19fQ
      case req @ Method.GET -> Root / "oob" =>
        for {
          _ <- ZIO.log("oob")
          ret <- ZIO.succeed(OutOfBand.oob(req.url.encode) match
            case Left(error)                          => Response.text(error).copy(status = Status.BadRequest)
            case Right(OutOfBandPlaintext(msg, data)) => Response.json(msg.toJsonPretty)
            case Right(OutOfBandSigned(msg, data))    => Response.json(msg.payload.content)
          )
        } yield (ret)
      case req @ Method.GET -> Root / "socket" =>
        for {
          _ <- ZIO.log("socket")
          agent <- AgentByHost.getAgentFor(req)
          sm <- agent.didSocketManager.get
          ret <- ZIO.succeed(Response.text(sm.toJsonPretty))
        } yield (ret)
      case req @ Method.POST -> Root / "socket" / id =>
        for {
          hub <- ZIO.service[Hub[String]]
          agent <- AgentByHost.getAgentFor(req)
          sm <- agent.didSocketManager.get
          ret <- sm.ids
            .get(FROMTO(id))
            .toSeq
            .flatMap { socketsID =>
              socketsID.flatMap(id => sm.sockets.get(id).map(e => (id, e))).toSeq
            } match {
            case Seq() =>
              req.body.asString.flatMap(e => hub.publish(s"socket missing for $id"))
                *> ZIO.succeed(Response.text(s"socket missing"))
            case seq =>
              ZIO.foreach(seq) { (socketID, channel) =>
                req.body.asString.flatMap(e => channel.socketOutHub.publish(e))
              } *> ZIO.succeed(Response.text(s"message sended"))
          }
        } yield (ret)
      case req @ Method.POST -> Root / "ops" =>
        req.body.asString
          .tap(e => ZIO.log("ops"))
          .tap(e => ZIO.logTrace(s"ops: $e"))
          .flatMap(e => OperationsServerRPC.ops(e))
          .map(e => Response.text(e))

      // ### MAKE KEYS ###
      case req @ Method.POST -> Root / "makeKey" =>
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
      case req @ Method.GET -> Root / "makeKey" / "X25519" =>
        KeyGenerator.makeX25519
          .mapError(e => DidException(e))
          .map(e => Response.text(e.toJson))
      case req @ Method.GET -> Root / "makeKey" / "Ed25519" =>
        KeyGenerator.makeEd25519
          .mapError(e => DidException(e))
          .map(e => Response.text(e.toJson))

      case Method.GET -> Root / "resolver" / did =>
        DIDSubject.either(did) match
          case Left(error)  => ZIO.succeed(Response.text(error.error).copy(status = Status.BadRequest)).debug
          case Right(value) => ZIO.succeed(Response.text("DID:" + value)).debug
      case req @ Method.GET -> Root => { // html.Html.fromDomElement()
        val data = Source.fromResource(s"index.html").mkString("")
        ZIO.log("index.html") *> ZIO.succeed(Response.html(data))
      }
      case req @ Method.GET -> Root / "index.html" => ZIO.succeed(Response.redirect(URL.root))
    } ++ Http.fromResource(s"favicon.ico").when {
    case Method.GET -> Root / "favicon.ico" => true
    case _                                  => false
  } ++ Http.fromResource(s"manifest.webmanifest").when {
    case Method.GET -> Root / "manifest.webmanifest" => true
    case _                                           => false
  } ++ Http.fromResource(s"sw.js").when {
    case Method.GET -> Root / "sw.js" => true
    case _                            => false
  } ++ {
    Http
      .fromResource(s"webapp.js") // ".gz" becuase of 0
      .map(_.setHeaders(Headers(Header.ContentType(MediaType.application.javascript)))) // Header.ContentEncoding.GZip
      .when {
        case Method.GET -> Root / "webapp.js" => true
        case _                                => false
      }
  } ++ Http
    .collectHandler[Request] { case Method.GET -> "" /: "assets" /: path =>
      RoutesMiddleware
      // TODO https://zio.dev/reference/stream/zpipeline/#:~:text=ZPipeline.gzip%20%E2%80%94%20The%20gzip%20pipeline%20compresses%20a%20stream%20of%20bytes%20as%20using%20gzip%20method%3A
      import zio.http.html._
      path.encode match {
        case "" | "/" =>
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
            .toHandler
        case other =>
          val fullPath = s"assets/$other"
          val classLoader = Thread.currentThread().getContextClassLoader()
          Option(classLoader.getResourceAsStream(fullPath)) match {
            case None => Handler.notFound
            case Some(_) =>
              import zio.http.{MediaType => Ztype}
              val headerContentType = fullPath match
                case s if s.endsWith(".html") => Header.ContentType(Ztype.text.html)
                case s if s.endsWith(".js")   => Header.ContentType(Ztype.text.javascript)
                case s if s.endsWith(".css")  => Header.ContentType(Ztype.text.css)
                case s                        => Header.ContentType(Ztype.text.plain)
              // TODO headerContentEncoding:
              Handler
                .fromStream(ZStream.fromResource(fullPath))
                .map(r => r.withHeader(headerContentType))
          }
      }
    }

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
    client = Scope.default >>> Client.default
    inboundHub <- Hub.bounded[String](5)
    myServer <- Server
      .serve(
        ((app ++ mdocMarkdown ++ mdocHTML ++ DidPeerUniresolverDriver.resolverPeer) @@ MiddlewareUtils.all)
          // TODO Test this .withDefaultErrorResponse
          .tapUnhandledZIO(ZIO.logWarning("Unhandled Endpoint"))
          .tapErrorCauseZIO(cause => ZIO.logErrorCause(cause)) // THIS is to log all the erros
          .withDefaultErrorResponse
      )
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
