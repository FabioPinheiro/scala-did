package fmgp.did.method.prism.cli

import zio._
import zio.cli._
import zio.json._
import zio.http._
import java.nio.file.{Path => JPath}

import fmgp.did.DIDSubject
import fmgp.did.method.prism.*
import fmgp.did.method.prism.vdr
import fmgp.did.method.prism.cardano.CardanoNetwork

// didResolverPrismJVM/runMain fmgp.did.method.prism.cli.PrismCli

//TODO https://blog.derlin.ch/from-jar-to-brew

/** {{{
  * sbt "didResolverPrismJVM/assembly"
  * java -jar did-method-prism/jvm/target/scala-3.3.6/cardano-prism.jar indexer ../prism-vdr/preprod
  * }}}
  */
object PrismCli extends ZIOCliDefault {

  def notCurrentlyImplemented(cmd: Subcommand) = Console.printLine(
    s"Command `$cmd` support is not currently implemented. If you are interested in adding support, please open a pull request athttps://github.com/FabioPinheiro/scala-did."
  )

  val indexerCommand: Command[Subcommand.Indexer] =
    Command("indexer", blockfrostTokenOpt, indexerWorkDirAgr)
      .map { case (token, workdir) => Subcommand.Indexer(workdir, token.map(vdr.BlockfrostConfig(_))) }

  val didCommand = { // : Command[Subcommand.DID.DIDSubcommand]

    val createCommand = Command("create").map(_ => Subcommand.DIDCreate())
    val updateCommand = Command("update", didArg).map(Subcommand.DIDUpdate.apply)
    val deactivateCommand = Command("deactivate", didArg).map(Subcommand.DIDDeactivate.apply)
    val resolveCommand = {
      val c1 = Command("resolve", networkFlag, didArg)
        .withHelp(HelpDoc.p("Resolve DID PRISM (from external storage)"))
        .map { case (network, did) => Subcommand.DIDResolve(did, network) }
      val c2 = Command("resolve", networkFlag, didArg ++ indexerWorkDirAgr)
        .withHelp(HelpDoc.p("Resolve DID PRISM (from indexer FS storage)"))
        .map { case (network, (did, workdir)) => Subcommand.DIDResolveFromFS(did, workdir, network) }

      c1 | c2 // Command.OrElse(c1, c2)
    }

    Command("did", Options.none)
      // REPORT SCALA BUG in .map(Subcommand.DID(_)) when Subcommand was no argument
      .subcommands(createCommand, updateCommand, deactivateCommand, resolveCommand)
  }

  val cliApp = CliApp.make(
    name = "cardano-prism",
    version = "0.1.0",
    summary = HelpDoc.Span.text("cli for the PRISM VDR protocol"),
    // command = finalCommand,
    command = Command("cardano-prism", Options.none, Args.none)
      .subcommands(
        Command("version").map(_ => Subcommand.Version()),
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
    case Subcommand.Version() => Console.printLine(s"0.1.0")
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
    case cmd @ Subcommand.DIDCreate()             => notCurrentlyImplemented(cmd)
    case cmd @ Subcommand.DIDUpdate(didPrism)     => notCurrentlyImplemented(cmd)
    case cmd @ Subcommand.DIDDeactivate(didPrism) => notCurrentlyImplemented(cmd)
    case cmd @ Subcommand.DIDResolve(didPrism, network) =>
      val aux = (Client.default ++ Scope.default >>> HttpUtils.layer) >>>
        DIDPrismResolver.layerDIDPrismResolver(
          s"https://raw.githubusercontent.com/blockfrost/prism-vdr/refs/heads/main/${network.name}/diddoc"
        )
      val program = for {
        _ <- ZIO.log(s"Revolce ${didPrism.string}")
        resolver <- ZIO.service[DIDPrismResolver]
        diddoc <- resolver.didDocument(didPrism.asFROMTO).orDieWith(error => new RuntimeException(error.toString()))
        _ <- Console.printLine(diddoc.toJsonPretty) // .mapError(error => SomeThrowable(error))
      } yield ()
      program
        .provideSomeLayer(aux)
        .catchNonFatalOrDie(error =>
          ZIO.succeed(error.printStackTrace()) *>
            ZIO.logError(error.getMessage()) *>
            ZIO.fail(exit(ExitCode.failure))
        )
    case cmd @ Subcommand.DIDResolveFromFS(did: DIDPrism, workdir: JPath, network) => notCurrentlyImplemented(cmd)
    // case input => Console.printLine(s"Args parsed: $input")
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

object CliDIDResolve extends ZIOAppDefault {
  override val run = PrismCli.cliApp.run(
    List(
      "did",
      "resolve",
      "--network",
      "preprod",
      "did:prism:000010053b08fd04a93584176a82fed92e99a053614898dc8409ad3a9c43f435",
    )
  )
}
object CliIndexerRun extends ZIOAppDefault {
  override val run = PrismCli.cliApp.run(
    List("indexer", "--blockfrost-token", "preprod<token>", "../prism-vdr/preprod")
  )
}
