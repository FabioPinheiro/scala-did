package fmgp.did.method.prism.cli

import zio._
import zio.cli._
import zio.json._
import zio.http._

import fmgp.did.DIDSubject
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.CardanoNetwork

// didResolverPrismJVM/runMain fmgp.did.method.prism.cli.PrismCli

//TODO https://blog.derlin.ch/from-jar-to-brew

/** {{{
  * sbt "didResolverPrismJVM/assembly"
  * java -jar did-method-prism/jvm/target/scala-3.3.6/cardano-prism.jar indexer ../prism-vdr/preprod
  * }}}
  */
object PrismCli extends ZIOCliDefault {

  val version = "0.3.0" // FIXME version MUST come from the build pipeline

  def notCurrentlyImplemented(cmd: Subcommand) = Console.printLine(
    s"Command `$cmd` support is not currently implemented. If you are interested in adding support, please open a pull request at https://github.com/FabioPinheiro/scala-did."
  )

  def cliApp = CliApp.make(
    name = "cardano-prism",
    version = version,
    summary = HelpDoc.Span.text("cli for the PRISM VDR protocol"),
    // command = finalCommand,
    command = Command("cardano-prism", Options.none, Args.none)
      .subcommands(
        Command("version").map(_ => Subcommand.Version()),
        Staging.command,
        MnemonicCommand.mnemonicCommand,
        KeyCommand.command,
        IndexerCommand.command,
        DIDCommand.didCommand,
        testCommand
      ),
    config = CliConfig(
      caseSensitive = true,
      autoCorrectLimit = 2,
      finalCheckBuiltIn = false,
      showAllNames = true,
      showTypes = true
    )
  )(cmd => executeModel[Subcommand](cmd))

  def executeModel[Model](model: Model): ZIO[Any, Unit, Unit] = {
    model match {
      case Subcommand.Version()               => Console.printLine(version)
      case cmd: Subcommand.Staging            => Staging.program(cmd)
      case cmd: Subcommand.Test               => programTest(cmd)
      case cmd: Subcommand.MnemonicSubcommand => MnemonicCommand.program(cmd)
      case cmd: Subcommand.Mnemonic2Key       => KeyCommand.program(cmd)
      case cmd: Subcommand.DIDSubcommand      => DIDCommand.program(cmd)
      case cmd: Subcommand.Indexer            => IndexerCommand.program(cmd)
      // case input => Console.printLine(s"Args parsed: $input")
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
