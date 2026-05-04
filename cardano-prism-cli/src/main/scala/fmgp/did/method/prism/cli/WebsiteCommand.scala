package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*

/** Commands that drive a local website to interact with the user's browser. */
object WebsiteCommand {

  private val portOption = Options
    .integer("port")
    .withDefault(BigInt(8088))
    .??("HTTP port for the local browser UI")

  def submitCip30Command: Command[CMD.SubmitEventsCip30] = Command(
    "submit-cip30",
    portOption,
    BlockfrostCommand.eventArg,
  ).map { case (port, events) =>
    CMD.SubmitEventsCip30(events = events, port = port.toInt)
  }

  def openCommand: Command[CMD.OpenWebsite] =
    Command("open", portOption)
      .withHelp("Open the cardano-prism playground (Home page) in the browser")
      .map(port => CMD.OpenWebsite(port = port.toInt))

  def simulateCommand: Command[CMD.SimulateOnWebsite] =
    Command("simulate", portOption)
      .withHelp("Open the cardano-prism playground on the Simulate page")
      .map(port => CMD.SimulateOnWebsite(port = port.toInt))

  val command: Command[CMD.WebsiteCMD] = Command("website")
    .withHelp("Local-website driven flows (Home, Simulate, CIP-30 submit, …)")
    .subcommands(
      submitCip30Command,
      openCommand,
      simulateCommand,
    )

  def program(cmd: CMD.WebsiteCMD): ZIO[Any, Throwable, Unit] = cmd match {
    case CMD.SubmitEventsCip30(events, port) =>
      for {
        result <- WebsiteServer.runUntilDone(events, port)
        explorer = result.networkId match
          case 1 => s"https://cardanoscan.io/transaction/${result.txHash}?tab=metadata"
          case _ => s"https://preprod.cardanoscan.io/transaction/${result.txHash}?tab=metadata"
        _ <- Console.printLine(s"Trasation Hash: ${result.txHash}")
        _ <- Console.printLine(s"See $explorer")
      } yield ()
    case CMD.OpenWebsite(port) =>
      WebsiteServer.runForever(WebsiteServer.Landing.Home, port).unit
    case CMD.SimulateOnWebsite(port) =>
      WebsiteServer.runForever(WebsiteServer.Landing.Simulate, port).unit
  }

}
