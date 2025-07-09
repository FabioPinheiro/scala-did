package fmgp.did.method.prism.cli

import zio._
import zio.cli._
import zio.json._
import zio.http._
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cli.*

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

object DIDCommand {

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

  val didCommand: Command[Subcommand.DIDSubcommand] =
    Command("did", Options.none)
      // REPORT SCALA BUG in .map(Subcommand.DID(_)) when Subcommand was no argument
      .subcommands(createCommand, updateCommand, deactivateCommand, resolveCommand)

  def program(cmd: Subcommand.DIDSubcommand) = cmd match
    case Subcommand.DIDCreate()        => PrismCli.notCurrentlyImplemented(cmd)
    case Subcommand.DIDUpdate(did)     => PrismCli.notCurrentlyImplemented(cmd)
    case Subcommand.DIDDeactivate(did) => PrismCli.notCurrentlyImplemented(cmd)
    case Subcommand.DIDResolve(did, network) =>
      val aux = (Client.default ++ Scope.default >>> HttpUtils.layer) >>>
        DIDPrismResolver.layerDIDPrismResolver(
          s"https://raw.githubusercontent.com/blockfrost/prism-vdr/refs/heads/main/${network.name}/diddoc"
        )
      val program = for {
        _ <- ZIO.log(s"Resolve ${did.string}")
        resolver <- ZIO.service[DIDPrismResolver]
        diddoc <- resolver.didDocument(did.asFROMTO).orDieWith(error => new RuntimeException(error.toString()))
        _ <- Console.printLine(diddoc.toJsonPretty) // .mapError(error => SomeThrowable(error))
      } yield ()
      program
        .provideSomeLayer(aux)
    case Subcommand.DIDResolveFromFS(did, workdir, network) => PrismCli.notCurrentlyImplemented(cmd)

}
