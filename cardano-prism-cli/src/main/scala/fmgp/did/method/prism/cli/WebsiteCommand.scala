package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*

/** Commands that drive a local one-shot website to interact with the user's
  * browser — wallet integration, presentation flows, etc. Distinct from
  * Blockfrost-driven submission because no Blockfrost token / local wallet is
  * needed; everything happens in the browser.
  */
object WebsiteCommand {

  val command: Command[CMD.WebsiteCMD] = Command("website")
    .withHelp("Local-website driven flows (CIP-30 browser wallet, …)")
    .subcommands(
      submitCip30Command,
    )

  def submitCip30Command = Command(
    "submit-cip30",
    Options.integer("port").withDefault(BigInt(8088)).??("HTTP port for the local browser UI"),
    BlockfrostCommand.eventArg,
  ).map { case (port, events) =>
    CMD.SubmitEventsCip30(events = events, port = port.toInt)
  }

  def program(cmd: CMD.WebsiteCMD): ZIO[Any, Throwable, Unit] = cmd match {
    case CMD.SubmitEventsCip30(events, port) =>
      for {
        result <- Cip30SubmitterServer.run(events, port)
        explorer = result.networkId match
          case 1 => s"https://cardanoscan.io/transaction/${result.txHash}?tab=metadata"
          case _ => s"https://preprod.cardanoscan.io/transaction/${result.txHash}?tab=metadata"
        _ <- Console.printLine(s"Trasation Hash: ${result.txHash}")
        _ <- Console.printLine(s"See $explorer")
      } yield ()
  }

}
