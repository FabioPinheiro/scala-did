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

object DIDCommand {

  val createCommand = Command(
    "create",
    ConfigCommand.options
      ++ (Options.text("master").withDefault("master") ++ Options.text("master-raw").optional)
      ++ (Options.text("vdr").withDefault("vdr").optional ++ Options.text("vdr-raw").optional)
  ).map { case (setup, (master, masterRaw), (vdr, vdrRaw)) =>
    CMD.DIDCreate(
      /* setup =       */ setup,
      /* masterLabel = */ master,
      /* masterRaw =   */ masterRaw,
      /* vdrLabel =    */ vdr,
      /* vdrRaw =.     */ vdrRaw
    )
  }
  val updateCommand = Command("update", didArg).map(CMD.DIDUpdate.apply)
  val deactivateCommand = Command("deactivate", didArg).map(CMD.DIDDeactivate.apply)
  val resolveCommand = {
    val c1 = Command("resolve", networkFlag, didArg)
      .withHelp(HelpDoc.p("Resolve DID PRISM (from external storage)"))
      .map { case (network, did) => CMD.DIDResolve(did, network) }
    val c2 = Command("resolve", networkFlag, didArg ++ indexerWorkDirAgr)
      .withHelp(HelpDoc.p("Resolve DID PRISM (from indexer FS storage)"))
      .map { case (network, (did, workdir)) => CMD.DIDResolveFromFS(did, workdir, network) }

    c1 | c2 // Command.OrElse(c1, c2)
  }

  val command: Command[CMD.DIDCMD] =
    Command("did", Options.none)
      // REPORT SCALA BUG in .map(CMD.DID(_)) when CMD was no argument
      .subcommands(createCommand, updateCommand, deactivateCommand, resolveCommand)

  def program(cmd: CMD.DIDCMD) = cmd match
    case CMD.DIDCreate(setup, masterLabel, masterRaw, vdrLabel, vdrRaw) =>
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
        _ <- ZIO.log(s"Simulate the PrismState")
        prismState = PrismStateInMemory.empty.addMaybeEvent(maybeOperation)
        ssi <- prismState.getSSI(didPrism)
        _ <- ZIO.log(s"PrismState: ${ssi.toJsonPretty}")
        _ <- Console.printLine(bytes2Hex(signedPrismOperation.toByteArray))
      } yield ()).provideLayer(setup.layer)
    case CMD.DIDUpdate(did)     => PrismCli.notCurrentlyImplemented(cmd)
    case CMD.DIDDeactivate(did) => PrismCli.notCurrentlyImplemented(cmd)
    case CMD.DIDResolve(did, network) =>
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
    case CMD.DIDResolveFromFS(did, workdir, network) => PrismCli.notCurrentlyImplemented(cmd)

}
