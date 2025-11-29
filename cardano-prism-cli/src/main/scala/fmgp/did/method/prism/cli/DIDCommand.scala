package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import zio.http.*
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cli.*
import fmgp.did.method.prism.cardano.DIDExtra
import fmgp.did.method.prism.proto.MaybeEvent
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import fmgp.did.uniresolver.Uniresolver
import fmgp.did.Resolver

object DIDCommand {

  val createCommand = Command(
    "create",
    ConfigCommand.optionsDefualt
      ++ (Options.text("master").withDefault("master").??("this is the label/name of the master key to be used")
        ++ Options.text("master-raw").optional)
      ++ (Options.text("vdr").withDefault("vdr").optional.??("this is the label/name of the vdr key to be used")
        ++ Options.text("vdr-raw").optional)
  ).map { case (setup, (master, masterRaw), (vdr, vdrRaw)) =>
    CMD.DIDCreate(
      /* setup =       */ setup,
      /* masterLabel = */ master,
      /* masterRaw =   */ masterRaw,
      /* vdrLabel =    */ vdr,
      /* vdrRaw =.     */ vdrRaw
    )
  }
  val updateCommand = Command("update", didPrismArg).map(CMD.DIDUpdate.apply)
  val deactivateCommand = Command("deactivate", didPrismArg).map(CMD.DIDDeactivate.apply)
  val resolveCommand = {
    val c1 = Command("resolve-prism", networkFlag, didPrismArg)
      .withHelp(HelpDoc.p("Resolve DID PRISM (from external storage)"))
      .map { case (network, did) => CMD.DIDResolve(did, network) }
    val c2 = Command("resolve-prism", networkFlag, didPrismArg ++ indexerWorkDirAgr)
      .withHelp(HelpDoc.p("Resolve DID PRISM (from indexer FS storage)"))
      .map { case (network, (did, workdir)) => CMD.DIDResolveFromFS(did, workdir, network) }

    val resolverEndpoint =
      Args
        .text("endpoint")
        .??("Endpoint of external resolver")
        .mapOrFail(e => Right(e))

    val c3 = Command("resolve", didPrismArg ++ resolverEndpoint)
      .withHelp(HelpDoc.p("Resolve DID PRISM (from endpoint)"))
      .map { case (did, endpointValue) =>
        val tmp = URL.decode(endpointValue).getOrElse(???) // FIXME
        CMD.DIDResolveFromEndpoint(did, tmp)
      }

    val c4 = Command(
      "resolve-universal",
      Options
        .text("endpoint")
        .optional
        // .withDefault("https://dev.uniresolver.io/")
        .??("Endpoint to get the DID Document"),
      didArg
    )
      .withHelp(HelpDoc.p("Resolve DIDs (using the 'https://dev.uniresolver.io/')"))
      .map { case (endpointValue, did) => CMD.DIDResolveFromUniresolver(did, endpointValue) }

    c1 | c2 | c3 | c4
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
          case None        => ZIO.none
          case Some(label) =>
            for {
              alternative <- stateLen(_.secp256k1PrivateKey.get(label).map(_.key))
              key = vdrRaw.map(rawHex => Utils.secp256k1FromRaw(rawHex)).orElse(alternative)
            } yield key.map(k => (label, k))
        (didPrism, signedPrismEvent) = DIDExtra.createDID(
          masterKeys = master.toSeq,
          vdrKeys = mVDR.toSeq,
        )
        _ <- ZIO.log(s"SSI: ${didPrism.string}")
        _ <- ZIO.log(s"Event: ${bytes2Hex(signedPrismEvent.toByteArray)}")
        maybeEvent = MaybeEvent.fromProto(signedPrismEvent, "tx-create", 0, 0)
        _ <- ZIO.log(s"MaybeEvent: ${maybeEvent.toJsonPretty}")
        _ <- ZIO.log(s"Simulate the PrismState")
        prismState <- PrismStateInMemory.empty
        _ <- prismState.addMaybeEvent(maybeEvent)
        ssi <- prismState.getSSI(didPrism)
        _ <- ZIO.log(s"PrismState: ${ssi.toJsonPretty}")
        _ <- Console.printLine(bytes2Hex(signedPrismEvent.toByteArray))
      } yield ()).provideLayer(setup.layer)
    case CMD.DIDUpdate(did)           => PrismCli.notCurrentlyImplemented(cmd)
    case CMD.DIDDeactivate(did)       => PrismCli.notCurrentlyImplemented(cmd)
    case CMD.DIDResolve(did, network) =>
      val aux = (Client.default ++ Scope.default >>> HttpUtils.layer) >>>
        DIDPrismResolver.layerDIDPrismResolver(
          // s"https://raw.githubusercontent.com/blockfrost/prism-vdr/refs/heads/main/${network.name}/diddoc"
          s"https://raw.githubusercontent.com/FabioPinheiro/prism-vdr/refs/heads/main/${network.name}/diddoc"
        )
      val program = for {
        _ <- ZIO.log(s"Resolve ${did.string} on $network network")
        resolver <- ZIO.service[DIDPrismResolver]
        diddoc <- resolver.didDocument(did.asFROMTO).orDieWith(error => new RuntimeException(error.toString()))
        _ <- Console.printLine(diddoc.toJsonPretty) // .mapError(error => SomeThrowable(error))
      } yield ()
      program.provideSomeLayer(aux)
    case CMD.DIDResolveFromFS(did, workdir, network)       => PrismCli.notCurrentlyImplemented(cmd)
    case CMD.DIDResolveFromEndpoint(did, resolverEndpoint) =>
      val aux = (Client.default ++ Scope.default >>> HttpUtils.layer) >>>
        DIDResolverProxy.layer(resolverEndpoint.toString)
      val program = for {
        _ <- ZIO.log(s"Resolve ${did.string} on ${resolverEndpoint.toString}")
        resolver <- ZIO.service[Resolver]
        diddoc <- resolver.didDocument(did.asFROMTO).orDieWith(error => new RuntimeException(error.toString()))
        _ <- Console.printLine(diddoc.toJsonPretty) // .mapError(error => SomeThrowable(error))
      } yield ()
      program.provideSomeLayer(aux)
    case CMD.DIDResolveFromUniresolver(did, baseEndpoint) =>
      val aux = (Client.default ++ Scope.default) >>>
        baseEndpoint.map(url => Uniresolver.layer(url)).getOrElse(Uniresolver.layer())
      val program = for {
        _ <- ZIO.log(s"Universal-Resolve ${did.string} on ${baseEndpoint.toString}")
        resolver <- ZIO.service[Resolver]
        diddoc <- resolver.didDocument(did.asFROMTO).orDieWith(error => new RuntimeException(error.toString()))
        _ <- Console.printLine(diddoc.toJsonPretty) // .mapError(error => SomeThrowable(error))
      } yield ()
      program.provideSomeLayer(aux)

}
