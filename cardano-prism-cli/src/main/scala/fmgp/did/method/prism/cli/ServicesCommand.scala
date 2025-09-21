package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.http.*
import zio.json.*

object ServicesCommand {

  val command =
    Command("services")
      .subcommands(
        Command("prism-submitter", ConfigCommand.optionsDefualt).map(setup => CMD.SubmitDID(setup = setup)),
      )

  def program(cmd: CMD.ServicesCMD): ZIO[Any, Nothing, Unit] = cmd match {
    case CMD.SubmitDID(setup) =>
      (for {
        _ <- ZIO.log("Starting service DID PRISM Submitter")
        fiber <- PrismSubmitterServer.run.onInterrupt(e => ZIO.log("Graceful Shutdown")).fork
        _ <- Console.readLine("Prese enter to Shutdown").orDie <|> fiber.await
        _ <- Console.printLine("Start Shutdown").orDie
        _ <- fiber.interrupt
      } yield ()).provideLayer(setup.layer)
  }

}
