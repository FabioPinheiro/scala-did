package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import fmgp.util.hex2bytes
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.vdr.GenericVDRDriver

object VDRCommand {

  private val ownerOpt = Options
    .text("owner")
    .??("Reference to the PRISM DID that will owner the data")
    .mapOrFail(did =>
      DIDPrism
        .fromString(did)
        .left
        .map(error =>
          ValidationError(
            ValidationErrorType.InvalidValue,
            HelpDoc.p(error)
          )
        )
    )

  val command =
    Command("vdr")
      .subcommands(
        Command(
          "create-bytes",
          ConfigCommand.optionsDefualt
            ++ networkOnlineFlag
            ++ indexerWorkDirOpt
            ++ ownerOpt
            ++ Options.text("vdr").withDefault("vdr").??("Owner's VDR key name/label")
            ++ Options.text("vdr-raw").optional.??("Owner's VDR private key (in hex)"),
          Args.text("bytes (hex)").??("Bytes in hexdecimal to create a VDR entry")
        ).map { case ((setup, network, workdir, didOwner, label, keyRaw), data) =>
          CMD.VDRCreateBytes(
            setup = setup,
            network = network,
            workdir = workdir,
            didOwner = didOwner,
            vdrKeyLabel = label,
            vdrKeyRaw = keyRaw.map(hex2bytes(_)),
            data = hex2bytes(data),
          )
        }.withHelp("Create a VDR Bytes entry"),
        Command(
          "create-ipfs",
          ConfigCommand.optionsDefualt
            ++ networkOnlineFlag
            ++ indexerWorkDirOpt
            ++ ownerOpt
            ++ Options.text("vdr").withDefault("vdr").??("Owner's VDR key name/label")
            ++ Options.text("vdr-raw").optional.??("Owner's VDR private key (in hex)"),
          Args.text("CID").??("IPFS's CID to create a VDR entry")
        ).map { case ((setup, network, workdir, didOwner, label, keyRaw), cid) =>
          CMD.VDRCreateIPFS(
            setup = setup,
            network = network,
            workdir = workdir,
            didOwner = didOwner,
            vdrKeyLabel = label,
            vdrKeyRaw = keyRaw.map(hex2bytes(_)),
            cid = cid,
          )
        }.withHelp("Create a VDR IPFS entry with a CID"),
        Command(
          "update-bytes",
          ConfigCommand.optionsDefualt
            ++ networkOnlineFlag
            ++ indexerWorkDirOpt
            ++ Options.text("vdr").withDefault("vdr")
            ++ Options.text("vdr-raw").optional,
          Args.text("vdr-entry-ref").??("VDR entry Id") ++
            Args.text("bytes (hex)").??("Bytes in hexdecimal to repace the old data")
        ).map { case ((setup, network, workdir, keyLabel, keyRaw), (vdrEntryRef, data)) =>
          CMD.VDRUpdateBytes(
            setup = setup,
            network = network,
            workdir = workdir,
            vdrEntryRef = RefVDR(vdrEntryRef),
            vdrKeyLabel = keyLabel,
            vdrKeyRaw = keyRaw.map(hex2bytes(_)),
            data = hex2bytes(data)
          )
        }.withHelp("Update the VDR Bytes entry with a new Bytes"),
        Command(
          "update-ipfs",
          ConfigCommand.optionsDefualt
            ++ networkOnlineFlag
            ++ indexerWorkDirOpt
            ++ Options.text("vdr").withDefault("vdr")
            ++ Options.text("vdr-raw").optional,
          Args.text("vdr-entry-ref").??("VDR entry Id") ++
            Args.text("CID").??("IPFS's CID to create a VDR entry")
        ).map { case ((setup, network, workdir, keyLabel, keyRaw), (vdrEntryRef, cid)) =>
          CMD.VDRUpdateIPFS(
            setup = setup,
            network = network,
            workdir = workdir,
            vdrEntryRef = RefVDR(vdrEntryRef),
            vdrKeyLabel = keyLabel,
            vdrKeyRaw = keyRaw.map(hex2bytes(_)),
            cid = cid
          )
        }.withHelp("Update the VDR IPFS entry with a new CID"),
        Command(
          "deactivate",
          ConfigCommand.optionsDefualt
            ++ networkOnlineFlag
            ++ indexerWorkDirOpt
            ++ Options.text("vdr").withDefault("vdr")
            ++ Options.text("vdr-raw").optional,
          Args.text("vdr-entry-ref").??("VDR entry Id")
        ).map { case ((setup, network, workdir, keyLabel, keyRaw), vdrEntryRef) =>
          CMD.VDRDeactivateEntry(
            setup = setup,
            network = network,
            workdir = workdir,
            vdrEntryRef = RefVDR(vdrEntryRef),
            vdrKeyLabel = keyLabel,
            vdrKeyRaw = keyRaw.map(hex2bytes(_)),
          )
        },
        Command(
          "fetch",
          ConfigCommand.optionsDefualt
            ++ networkOnlineFlag
            ++ indexerWorkDirOpt,
          Args.text("vdr-entry-ref").??("VDR entry Id")
        ).map { case ((setup, network, workdir), vdrEntryRef) =>
          CMD.VDRFetchEntry(
            setup = setup,
            network = network,
            workdir = workdir,
            vdrEntryRef = RefVDR(vdrEntryRef),
          )
        },
        Command(
          "proof",
          ConfigCommand.optionsDefualt
            ++ networkOnlineFlag
            ++ indexerWorkDirOpt,
          Args.text("vdr-entry-ref").??("VDR entry Id")
        ).map { case ((setup, network, workdir), vdrEntryRef) =>
          CMD.VDRProofEntry(
            setup = setup,
            network = network,
            workdir = workdir,
            vdrEntryRef = RefVDR(vdrEntryRef),
          )
        },
      )

  def program(cmd: CMD.VDRCMD) = cmd match
    case CMD.VDRCreateBytes(setup, network, workdir, didOwner, vdrKeyLabel, vdrKeyRaw, data) =>
      (for {
        _ <- ZIO.log(s"Create VDR entry bytes owner by ${didOwner.string}")
        bfConfig <- stateLen[BlockfrostConfig](_.blockfrost(network))
          .flatMap {
            case None      => ZIO.fail(PrismCliError(s"BlockfrostConfig is not set for '${network.name}' network"))
            case Some(aux) => ZIO.succeed(aux)
          }
        wallet <- stateLen(_.cardanoWallet)
          .flatMap {
            case None      => ZIO.fail(PrismCliError(s"CardanoWalletConfig is not set"))
            case Some(aux) => ZIO.succeed(aux)
          }
        vdrKey <- vdrKeyRaw match
          case Some(raw) => ZIO.succeed(Secp256k1PrivateKey(raw))
          case None      =>
            stateLen(_.ssiPrivateKeys.get(vdrKeyLabel))
              .flatMap {
                case Some(KeySecp256k1(derivationPath, key)) => ZIO.succeed(key)
                case Some(KeyEd25519(derivationPath, key))   =>
                  ZIO.fail(PrismCliError(s"Key '$vdrKeyLabel' found but is not of the type secp256k1"))
                case None => ZIO.fail(PrismCliError(s"No Key found with label '$vdrKeyLabel'"))
              }
        driver = GenericVDRDriver(
          bfConfig = bfConfig,
          wallet = wallet,
          workdir = workdir.toAbsolutePath().normalize().toString(),
          didPrism = didOwner,
          keyName = vdrKeyLabel,
          vdrKey = vdrKey,
          maybeMsgCIP20 = Some("cardano-prism"),
        )
        aux <- driver.createBytesEntry(data)
        (refVDR, txRef) = aux
        _ <- ZIO.log(s"VDR entry created '$refVDR'. In trasation $txRef")
        _ <- Console.printLine(refVDR)
      } yield ()).provideLayer(setup.layer)
    case CMD.VDRCreateIPFS(setup, network, workdir, didOwner, vdrKeyLabel, vdrKeyRaw, cid) =>
      (for {
        _ <- ZIO.log(s"Create VDR entry IPFS owner by ${didOwner.string} with cid '$cid'")
        _ <- ZIO.logError(s"TODO this is not implemente") // TODO
      } yield ()).provideLayer(setup.layer)
    case CMD.VDRUpdateBytes(setup, network, workdir, vdrEntryRef, vdrKeyLabel, vdrKeyRaw, data) =>
      (for {
        _ <- ZIO.log(s"Update VDR entry $vdrEntryRef")
        _ <- ZIO.logError(s"TODO this is not implemente") // TODO
      } yield ()).provideLayer(setup.layer)
    case CMD.VDRUpdateIPFS(setup, network, workdir, vdrEntryRef, vdrKeyLabel, vdrKeyRaw, cid) =>
      (for {
        _ <- ZIO.log(s"Update VDR entry $vdrEntryRef with cid '$cid'")
        _ <- ZIO.logError(s"TODO this is not implemente") // TODO
      } yield ()).provideLayer(setup.layer)
    case CMD.VDRDeactivateEntry(setup, network, workdir, vdrEntryRef, vdrKeyLabel, vdrKeyRaw) =>
      (for {
        _ <- ZIO.log(s"Deactivate VDR entry $vdrEntryRef")
        _ <- ZIO.logError(s"TODO this is not implemente") // TODO
      } yield ()).provideLayer(setup.layer)
    case CMD.VDRFetchEntry(setup, network, workdir, vdrEntryRef) =>
      (for {
        _ <- ZIO.log(s"Fetch VDR entry $vdrEntryRef")
        _ <- ZIO.logError(s"TODO this is not implemente") // TODO
      } yield ()).provideLayer(setup.layer)
    case CMD.VDRProofEntry(setup, network, workdir, vdrEntryRef) =>
      (for {
        _ <- ZIO.log(s"Generate proof for VDR entry $vdrEntryRef")
        _ <- ZIO.logError(s"TODO this is not implemente") // TODO
      } yield ()).provideLayer(setup.layer)

}
