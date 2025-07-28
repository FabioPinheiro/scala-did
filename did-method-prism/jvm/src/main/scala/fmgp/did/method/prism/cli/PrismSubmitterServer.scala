package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.http.*
import zio.json.*
import zio.http.endpoint.Endpoint
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.did.method.prism.DIDPrism
import fmgp.util.safeValueOf
import fmgp.util.hex2bytes
import fmgp.util.Base64
import proto.prism.SignedPrismOperation
import proto.prism.PrismOperation
import fmgp.did.method.prism.proto.tryParseFrom
import zio.http.codec.HttpCodec

object PrismSubmitterServer {
  val logoCardanoPrism =
    """ ██████╗ █████╗ ██████╗ ██████╗  █████╗ ███╗   ██╗ ██████╗       ██████╗ ██████╗ ██╗███████╗███╗   ███╗
      |██╔════╝██╔══██╗██╔══██╗██╔══██╗██╔══██╗████╗  ██║██╔═══██╗      ██╔══██╗██╔══██╗██║██╔════╝████╗ ████║
      |██║     ███████║██████╔╝██║  ██║███████║██╔██╗ ██║██║   ██║█████╗██████╔╝██████╔╝██║███████╗██╔████╔██║
      |██║     ██╔══██║██╔══██╗██║  ██║██╔══██║██║╚██╗██║██║   ██║╚════╝██╔═══╝ ██╔══██╗██║╚════██║██║╚██╔╝██║
      |╚██████╗██║  ██║██║  ██║██████╔╝██║  ██║██║ ╚████║╚██████╔╝      ██║     ██║  ██║██║███████║██║ ╚═╝ ██║
      | ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝ ╚═════╝       ╚═╝     ╚═╝  ╚═╝╚═╝╚══════╝╚═╝     ╚═╝
      |                                                                                                       """.stripMargin // https://patorjk.com/software/taag/#p=display&f=ANSI%20Shadow&t=cardano-prism
  val logoDidSubmitter =
    """██████╗ ██╗██████╗       ███████╗██╗   ██╗██████╗ ███╗   ███╗██╗████████╗████████╗███████╗██████╗      
      |██╔══██╗██║██╔══██╗      ██╔════╝██║   ██║██╔══██╗████╗ ████║██║╚══██╔══╝╚══██╔══╝██╔════╝██╔══██╗     
      |██║  ██║██║██║  ██║█████╗███████╗██║   ██║██████╔╝██╔████╔██║██║   ██║      ██║   █████╗  ██████╔╝     
      |██║  ██║██║██║  ██║╚════╝╚════██║██║   ██║██╔══██╗██║╚██╔╝██║██║   ██║      ██║   ██╔══╝  ██╔══██╗     
      |██████╔╝██║██████╔╝      ███████║╚██████╔╝██████╔╝██║ ╚═╝ ██║██║   ██║      ██║   ███████╗██║  ██║     
      |╚═════╝ ╚═╝╚═════╝       ╚══════╝ ╚═════╝ ╚═════╝ ╚═╝     ╚═╝╚═╝   ╚═╝      ╚═╝   ╚══════╝╚═╝  ╚═╝     
      |                                                                                                       """.stripMargin // https://patorjk.com/software/taag/#p=display&f=ANSI%20Shadow&t=DID-submitter

  val prismSubmitterLogo =
    """██████╗ ██████╗ ██╗███████╗███╗   ███╗      ███████╗██╗   ██╗██████╗ ███╗   ███╗██╗████████╗████████╗███████╗██████╗ 
      |██╔══██╗██╔══██╗██║██╔════╝████╗ ████║      ██╔════╝██║   ██║██╔══██╗████╗ ████║██║╚══██╔══╝╚══██╔══╝██╔════╝██╔══██╗
      |██████╔╝██████╔╝██║███████╗██╔████╔██║█████╗███████╗██║   ██║██████╔╝██╔████╔██║██║   ██║      ██║   █████╗  ██████╔╝
      |██╔═══╝ ██╔══██╗██║╚════██║██║╚██╔╝██║╚════╝╚════██║██║   ██║██╔══██╗██║╚██╔╝██║██║   ██║      ██║   ██╔══╝  ██╔══██╗
      |██║     ██║  ██║██║███████║██║ ╚═╝ ██║      ███████║╚██████╔╝██████╔╝██║ ╚═╝ ██║██║   ██║      ██║   ███████╗██║  ██║
      |╚═╝     ╚═╝  ╚═╝╚═╝╚══════╝╚═╝     ╚═╝      ╚══════╝ ╚═════╝ ╚═════╝ ╚═╝     ╚═╝╚═╝   ╚═╝      ╚═╝   ╚══════╝╚═╝  ╚═╝
      |                                                                                                                     """.stripMargin

  val intro =
    s"""$logoCardanoPrism
       |$logoDidSubmitter
       |DID Prism Submitter is a http service create to provide a faster developer of PoCs.
       |This service is not intended to be used in production!
       |It only provides a easy and fast way to submit to Cardano Blockchain the necessary material to create PRISM DIDs.
       |
       |Vist: https://github.com/FabioPinheiro/scala-did""".stripMargin
  // import zio.http.codec.PathCodec.path
  val postEndpoint =
    Endpoint(Method.POST / string("network")) /// string("did")) //
      .query(HttpCodec.query[Chunk[String]]("event") ++ HttpCodec.query[Boolean]("dryrun"))
      .out[String]
      .outError[String](Status.BadRequest)

//curl -X POST http://localhost:8088/mainnet/\?dryrun\=false\&event=0a066d617374657212463044022048133b4c909a59f164c99eb996c8c4ddc36b1592d2f53e39bafcd2254190d10d0220136154cc9445c3d8da7fa5bacc8363888c919c3712832d6414294ae80a0375061a400a3e0a3c123a0a066d617374657210014a2e0a09736563703235366b311221039c14bb32096d246b54a05a232f3e686a1fa164bea43680b4a6bba84392cc9025 -v

  val postEndpointRoute = postEndpoint
    .implement((networkInput, /*didInput,*/ events, dryrun) =>
//      BlockfrostSubmitEvents(setup, network, events)
      for {
        _ <- ZIO.log(s"dryrun = $dryrun, networkInput = $networkInput") // ; didInput = $didInput")
        network <- safeValueOf(
          CardanoNetwork.valueOf(networkInput.take(1).toUpperCase + networkInput.drop(1))
        ) match
          case Left(someNetwork) => ZIO.fail(s"Invalid network '$someNetwork'")
          case Right(network)    => ZIO.succeed(network)
        // didPrism <- DIDPrism.fromString(didInput) match
        //   case Left(error) => ZIO.fail(s"Invalid DID PRISM '$error'")
        //   case Right(did)  => ZIO.succeed(did)
        events <- ZIO.foreach(events.zipWithIndex) { (proto, index) =>
          for {
            // protoBase64 <- Base64.safeBase64url(proto) match
            //   case Left(error)   => ZIO.fail(s"Invalid Base64url (event index=$index) '$error'")
            //   case Right(base64) => ZIO.succeed(base64)
            // _ <- ZIO.log(protoBase64.decodeToHex)
            // prismEvent <- PrismOperation.tryParseFrom(protoBase64.decode) match
            event <- SignedPrismOperation.tryParseFrom(hex2bytes(proto)) match
              case Left(error)  => ZIO.fail(s"Invalid SignedPrismOperation (event index=$index):'$error'")
              case Right(value) => ZIO.succeed(value)
          } yield event
        }
        setup <- ZIO.service[Ref[Setup]].flatMap(_.get)
        _ = ZIO.when(dryrun)( // FIXME
          ZIO.logError("dryrun not implemented anymore") *> ZIO.fail(new RuntimeException("dryrun not implemented "))
        )
        cmd = CMD.BlockfrostSubmitEvents(setup, network = network, events = events)
        _ <- ZIO.log(s"Network: $network; dry-run:$dryrun; Events#: ${events.size}")
        blockfrostCommandOut <- {
          if (dryrun) {
            BlockfrostCommand.submitProgram(cmd).map(_.hex).flatMapError(ex => ZIO.succeed(ex.getMessage()).debug)
          } else ZIO.succeed("This was a dry-run")
        }

      } yield blockfrostCommandOut
    )

  val routes =
    Routes(
      Method.GET / Root -> handler(Response.text("Prism Submitter service")),
      postEndpointRoute,
    ).handleErrorRequestCauseZIO((request, cause) =>
      ZIO.succeed(Response.text(s"ERROR: ${cause.toString()}").status(Status.InternalServerError))
    )

  def run: ZIO[Ref[Setup], Throwable, Unit] =
    for {
      _ <- Console.printLine(intro)
      _ <- ZIO.log(s"DID Prism Submitter. See https://github.com/FabioPinheiro/scala-did")
      port <- System
        .env("PORT")
        .flatMap {
          case None        => System.property("port")
          case Some(value) => ZIO.succeed(Some(value))
        }
        .map(_.flatMap(_.toIntOption).getOrElse(8088))
      _ <- ZIO.log(s"Starting server on port: $port")
      zServerLayer = Server.defaultWithPort(port)
      _ <- Server.serve(routes).provideSomeLayer(zServerLayer)
    } yield ()

}
