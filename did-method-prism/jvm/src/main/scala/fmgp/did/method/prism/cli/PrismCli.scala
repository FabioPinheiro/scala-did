package fmgp.did.method.prism.cli

import zio._
import zio.Console.printLine
import zio.cli.HelpDoc.Span.text
import zio.cli._

import java.nio.file.{Path => JPath}
import fmgp.did.method.prism.vdr
import fmgp.did.method.prism.vdr.BlockfrostConfig
import fmgp.did.method.prism.DIDPrism
import fmgp.did.DIDSubject

// didResolverPrismJVM/runMain fmgp.did.method.prism.cli.PrismCli

//TODO https://blog.derlin.ch/from-jar-to-brew

/** {{{
  * sbt "didResolverPrismJVM/assembly"
  * java -jar did-method-prism/jvm/target/scala-3.3.6/cardano-prism.jar indexer ../prism-vdr/preprod
  * }}}
  */
object PrismCli extends ZIOCliDefault {

  def notCurrentlyImplemented(cmd: Subcommand) = printLine(
    s"Command `$cmd` support is not currently implemented. If you are interested in adding support, please open a pull request athttps://github.com/FabioPinheiro/scala-did."
  )

  sealed trait Subcommand extends Product with Serializable
  object Subcommand {
    final case class Indexer(workdir: JPath, mBlockfrostConfig: Option[BlockfrostConfig]) extends Subcommand
    // final case class DID() extends Subcommand

    sealed trait DIDSubcommand extends Subcommand
    final case class DIDCreate() extends DIDSubcommand
    final case class DIDUpdate(did: DIDPrism) extends DIDSubcommand
    final case class DIDDeactivate(did: DIDPrism) extends DIDSubcommand
    final case class DIDResolve(did: DIDPrism) extends DIDSubcommand
    final case class DIDResolveFromFS(did: DIDPrism, workdir: JPath) extends DIDSubcommand

    // TODO VDR

    // TODO Event load PrismState
    // TODO Event validate Event
    // TODO Event validate Events
    // TODO Event validate Block

  }

  val indexerCommand: Command[Subcommand.Indexer] =
    Command("indexer", blockfrostTokenOpt, indexerWorkDirAgr)
      .map { case (token, directory) => Subcommand.Indexer(directory, token.map(BlockfrostConfig(_))) }

  val didCommand = { // : Command[Subcommand.DID.DIDSubcommand]

    val createCommand = Command("create").map(_ => Subcommand.DIDCreate())
    val updateCommand = Command("update", didArg).map(Subcommand.DIDUpdate.apply)
    val deactivateCommand = Command("deactivate", didArg).map(Subcommand.DIDDeactivate.apply)
    val resolveCommand = {
      val c1 = Command("resolve", didArg)
        .withHelp(HelpDoc.p("Resolve DID PRISM (from external storage)"))
        .map(Subcommand.DIDResolve(_))
      val c2 = Command("resolve", didArg ++ indexerWorkDirAgr)
        .withHelp(HelpDoc.p("Resolve DID PRISM (from indexer FS storage)"))
        .map(e => Subcommand.DIDResolveFromFS(e._1, e._2))

      c1 | c2 // Command.OrElse(c1, c2)
    }

    Command("did", Options.none)
      // REPORT SCALA BUG in .map(Subcommand.DID(_)) when Subcommand was no argument
      .subcommands(createCommand, updateCommand, deactivateCommand, resolveCommand)
  }

  val cliApp = CliApp.make(
    name = "cardano-prism",
    version = "0.1.0",
    summary = text("cli for the PRISM VDR protocol"),
    // command = finalCommand,
    command = Command("cardano-prism", Options.none, Args.none)
      .subcommands(
        indexerCommand,
        didCommand
      ),
    config = CliConfig(
      caseSensitive = true,
      autoCorrectLimit = 2,
      finalCheckBuiltIn = false,
      showAllNames = true,
      showTypes = true
    )
  ) {
    case cmd @ Subcommand.Indexer(workdir, mBlockfrostConfig) =>
      val program: ZIO[Any, Throwable, Unit] = for {
        _ <- vdr.Indexer.indexerLogo
        indexerConfig = vdr.IndexerConfig(
          mBlockfrostConfig = mBlockfrostConfig,
          workdir = workdir.toAbsolutePath().normalize.toString
        )
        _ <- ZIO.log(s"IndexerConfig: `${indexerConfig}`")
        indexerConfigZLayer = ZLayer.succeed(indexerConfig) // vdr.Indexer.makeIndexerConfigZLayerFromArgs
        _ <- vdr.Indexer.indexerJob.provideLayer(indexerConfigZLayer)
      } yield ()
      program.catchNonFatalOrDie(error =>
        ZIO.succeed(error.printStackTrace()) *>
          ZIO.logError(error.getMessage()) *>
          ZIO.fail(exit(ExitCode.failure))
      )
    case cmd @ Subcommand.DIDCreate()                                     => notCurrentlyImplemented(cmd)
    case cmd @ Subcommand.DIDUpdate(didPrism)                             => notCurrentlyImplemented(cmd)
    case cmd @ Subcommand.DIDDeactivate(didPrism)                         => notCurrentlyImplemented(cmd)
    case cmd @ Subcommand.DIDResolve(didPrism)                            => notCurrentlyImplemented(cmd)
    case cmd @ Subcommand.DIDResolveFromFS(did: DIDPrism, workdir: JPath) => notCurrentlyImplemented(cmd)
    // case Left(cmd)  => printLine(s"Executing L `$cmd`")
    // case Right(cmd) => printLine(s"Executing R `$cmd`")

    // case input => printLine(s"Args parsed: $input")
  }
}

object CliHelp extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("indexer", "--help")) }
object CliWizard extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("--wizard")) }
object CliCompletio extends ZIOAppDefault {
  override val run = PrismCli.cliApp.run(List("--shell-completion-index", "--shell-completion-script", "--shell-type"))
}
object CliResolveHelp extends ZIOAppDefault { override val run = PrismCli.cliApp.run(List("did", "resolve", "--help")) }

object CliIndexerRun extends ZIOAppDefault {
  override val run = PrismCli.cliApp.run(
    List("indexer", "--blockfrost-token", "preprod<token>", "../prism-vdr/preprod")
  )
}
