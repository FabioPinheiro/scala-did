package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import zio.http.*

import fmgp.did.DIDSubject
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.PublicCardanoNetwork

/** cardano-prism CLI
  *
  * {{{
  * sbt "didResolverPrismJVM/assembly"
  * java -jar did-method-prism/jvm/target/scala-3.3.7/cardano-prism.jar indexer ../prism-vdr/preprod
  * shasum -a 256 did-method-prism/jvm/target/scala-3.3.7/cardano-prism.jar
  * }}}
  *
  * cardanoPrismCli/runMain fmgp.did.method.prism.cli.PrismCli
  *
  * cat ~/.cardano-prism-config.json
  */
object PrismCli extends ZIOCliDefault {

  val version = "0.5.0" // FIXME version MUST come from the build pipeline

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
        Command("version").map(_ => CMD.Version()),
        Command("pwd").map(_ => CMD.PWD()),
        ConfigCommand.command,
        IndexerCommand.command,
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
    import scala.sys.process.*
    command match {
      case CMD.Version()          => Console.printLine(version)
      case CMD.PWD()              => Console.printLine("pwd".!!)
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
  }.catchAll {
    case errorStr: String           => ZIO.logError(errorStr) *> Console.printError(errorStr).orDie
    case error: java.io.IOException =>
      ZIO.logError(error.getMessage()) *> ZIO.succeed(error.printStackTrace()) *> exit(ExitCode.failure)
    case error: Throwable =>
      ZIO.logError(error.getMessage()) *> ZIO.succeed(error.printStackTrace()) *> exit(ExitCode.failure)
  }
}

object CliHelp extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("indexer", "--help")) }
object CliWizard extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("--wizard")) }
object CliVersion extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("version")) }
object CliCompletio extends ZIOAppDefault {
  override val run =
    PrismCli.cliApp.run(List("--shell-completion-index", "--shell-completion-script", "--shell-type", "bash"))
}
// object CliResolveHelp extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("did", "resolve", "--help")) }

// object CliIndexerRun extends ZIOAppDefault {
//   override val run = PrismCli.cliApp.run(
//     List("indexer", "--blockfrost-token", "preprod<token>", "../prism-vdr/preprod")
//   )
// }
