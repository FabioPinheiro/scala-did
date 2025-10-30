package fmgp.did.method.prism.cli

import zio._
import zio.cli._
import zio.json._
import zio.http._

import fmgp.did.DIDSubject
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.CardanoNetwork

/** cardano-prism CLI
  *
  * {{{
  * sbt "didResolverPrismJVM/assembly"
  * java -jar did-method-prism/jvm/target/scala-3.3.7/cardano-prism.jar indexer ../prism-vdr/preprod
  * shasum -a 256 did-method-prism/jvm/target/scala-3.3.7/cardano-prism.jar
  * }}}
  *
  * cardanoPrismCliJVM/runMain fmgp.did.method.prism.cli.PrismCli
  */
object PrismCli extends ZIOCliDefault {

  val version = "0.4.0" // FIXME version MUST come from the build pipeline

  def notCurrentlyImplemented(cmd: CMD) = Console.printLine(
    s"Command `$cmd` support is not currently implemented. If you are interested in adding support, please open a pull request at https://github.com/FabioPinheiro/scala-did."
  )

  def cliApp = CliApp.make(
    name = "cardano-prism",
    version = version,
    summary = HelpDoc.Span.text("cli for the PRISM VDR protocol"),
    // command = finalCommand,
    command = Command("cardano-prism", Options.none, Args.none)
      .subcommands(
        IndexerCommand.command,
        Command("version").map(_ => CMD.Version()),
        ConfigCommand.command,
        MnemonicCommand.command,
        KeyCommand.command,
        DIDCommand.command,
        BlockfrostCommand.command,
        ServicesCommand.command,
        VDRCommand.command,
      ),
    config = CliConfig(
      caseSensitive = true,
      autoCorrectLimit = 2,
      finalCheckBuiltIn = false,
      showAllNames = true,
      showTypes = true
    )
  )(cmd => executeCMD(cmd))

  def executeCMD(command: CMD): ZIO[Any, Unit, Unit] = {
    command match {
      case CMD.Version()          => Console.printLine(version)
      case cmd: CMD.ConfigCMD     => ConfigCommand.program(cmd)
      case cmd: CMD.MnemonicCMD   => MnemonicCommand.program(cmd)
      case cmd: CMD.BlockfrostCMD => BlockfrostCommand.program(cmd)
      case cmd: CMD.KeyCMD        => KeyCommand.program(cmd)
      case cmd: CMD.DIDCMD        => DIDCommand.program(cmd)
      case cmd: CMD.VDRCMD        => VDRCommand.program(cmd)
      case cmd: CMD.CommCMD       => CommCommand.program(cmd)
      case cmd: CMD.Indexer       => IndexerCommand.program(cmd)
      case cmd: CMD.ServicesCMD   => ServicesCommand.program(cmd)
    }
  }.catchNonFatalOrDie { case error: java.io.IOException =>
    ZIO.succeed(error.printStackTrace()) *>
      ZIO.logError(error.getMessage()) *>
      exit(ExitCode.failure)
  }
}

object CliHelp extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("indexer", "--help")) }
object CliWizard extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("--wizard")) }
object CliVersion extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("version")) }
object CliCompletio extends ZIOAppDefault {
  override val run =
    PrismCli.cliApp.run(List("--shell-completion-index", "--shell-completion-script", "--shell-type", "bash"))
}
object CliResolveHelp extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("did", "resolve", "--help")) }

object CliIndexerRun extends ZIOAppDefault {
  override val run = PrismCli.cliApp.run(
    List("indexer", "--blockfrost-token", "preprod<token>", "../prism-vdr/preprod")
  )
}
