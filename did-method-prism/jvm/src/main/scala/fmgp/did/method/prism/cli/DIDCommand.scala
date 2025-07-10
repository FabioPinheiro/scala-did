package fmgp.did.method.prism.cli

import zio._
import zio.cli._
import zio.json._
import zio.http._
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cli.*
import fmgp.did.method.prism.cardano.DIDExtra
import fmgp.did.method.prism.proto.MaybeOperation
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex

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

  val createCommand = Command(
    "create",
    Staging.options
      ++ (Options.text("master").withDefault("master") ++ Options.text("master-raw").optional)
      ++ (Options.text("vdr").withDefault("vdr").optional ++ Options.text("vdr-raw").optional)
  ).map { case (setup, (master, masterRaw), (vdr, vdrRaw)) =>
    Subcommand.DIDCreate(
      /* setup =       */ setup,
      /* masterLabel = */ master,
      /* masterRaw =   */ masterRaw,
      /* vdrLabel =    */ vdr,
      /* vdrRaw =.     */ vdrRaw
    )
  }
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

  val command: Command[Subcommand.DIDSubcommand] =
    Command("did", Options.none)
      // REPORT SCALA BUG in .map(Subcommand.DID(_)) when Subcommand was no argument
      .subcommands(createCommand, updateCommand, deactivateCommand, resolveCommand)

  def program(cmd: Subcommand.DIDSubcommand) = cmd match
    case Subcommand.DIDCreate(setup, masterLabel, masterRaw, vdrLabel, vdrRaw) =>
      (for {
        _ <- ZIO.log(s"Command: $cmd")
        mkAlternative <- stateLen(_.secp256k1PrivateKey.get(masterLabel).map(_.key))
        master = masterRaw
          .map(rawHex => Utils.secp256k1FromRaw(rawHex))
          .orElse(mkAlternative)
          .map(k => (masterLabel, k))
        mVDR <- vdrLabel match
          case None => ZIO.none
          case Some(label) =>
            for {
              alternative <- stateLen(_.secp256k1PrivateKey.get(label).map(_.key))
              key = vdrRaw.map(rawHex => Utils.secp256k1FromRaw(rawHex)).orElse(alternative)
            } yield key.map(k => (label, k))
        (didPrism, signedPrismOperation) = DIDExtra.createDID(
          masterKeys = master.toSeq,
          vdrKeys = mVDR.toSeq,
        )
        _ <- ZIO.log(s"SSI: ${didPrism.string}")
        _ <- ZIO.log(s"Event: ${bytes2Hex(signedPrismOperation.toByteArray)}")
        maybeOperation = MaybeOperation.fromProto(signedPrismOperation, "tx-create", 0, 0)
        _ <- ZIO.log(s"MaybeOperation: ${maybeOperation.toJsonPretty}")
        _ <- Console.printLine(bytes2Hex(signedPrismOperation.toByteArray))
      } yield ()).provideLayer(setup.layer)
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
