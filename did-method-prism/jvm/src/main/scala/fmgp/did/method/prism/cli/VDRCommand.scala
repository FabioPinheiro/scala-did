package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import fmgp.util.hex2bytes
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.DIDPrism

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
          ConfigCommand.options
            ++ ownerOpt
            ++ Options.text("vdr").withDefault("vdr").??("Owner's VDR key name/label")
            ++ Options.text("vdr-raw").optional.??("Owner's VDR private key (in hex)"),
          Args.text("bytes (hex)").??("Bytes in hexdecimal to create a VDR entry")
        ).map { case ((setup, didOwner, label, keyRaw), data) =>
          CMD.VDRCreateBytes(
            setup = setup,
            didOwner = didOwner,
            vdrKeyLabel = label,
            vdrKeyRaw = keyRaw.map(hex2bytes(_)),
            data = hex2bytes(data),
          )
        },
        Command(
          "update-bytes",
          ConfigCommand.options ++
            Options.text("vdr").withDefault("vdr") ++ Options.text("vdr-raw").optional,
          Args.text("vdr-entry-ref").??("VDR entry Id") ++
            Args.text("bytes (hex)").??("Bytes in hexdecimal to repace the old data")
        ).map { case ((setup, label, keyRaw), (vdrEntryRef, data)) =>
          CMD.VDRUpdateBytes(
            setup = setup,
            vdrEntryRef = RefVDR(vdrEntryRef),
            vdrKeyLabel = label,
            vdrKeyRaw = keyRaw.map(hex2bytes(_)),
            data = hex2bytes(data)
          )
        },
        Command(
          "deactivate",
          ConfigCommand.options ++
            Options.text("vdr").withDefault("vdr") ++ Options.text("vdr-raw").optional,
          Args.text("vdr-entry-ref").??("VDR entry Id")
        ).map { case ((setup, label, keyRaw), vdrEntryRef) =>
          CMD.VDRDeactivateEntry(
            setup = setup,
            vdrEntryRef = RefVDR(vdrEntryRef),
            vdrKeyLabel = label,
            vdrKeyRaw = keyRaw.map(hex2bytes(_)),
          )
        },
        Command(
          "fetch",
          ConfigCommand.options,
          Args.text("vdr-entry-ref").??("VDR entry Id")
        ).map { case (setup, vdrEntryRef) =>
          CMD.VDRFetchEntry(
            setup = setup,
            vdrEntryRef = RefVDR(vdrEntryRef),
          )
        },
        Command(
          "proof",
          ConfigCommand.options,
          Args.text("vdr-entry-ref").??("VDR entry Id")
        ).map { case (setup, vdrEntryRef) =>
          CMD.VDRProofEntry(
            setup = setup,
            vdrEntryRef = RefVDR(vdrEntryRef),
          )
        },
      )

  def program(cmd: CMD.VDRCMD) = cmd match
    case CMD.VDRCreateBytes(setup, didOwner, vdrKeyLabel, vdrKeyRaw, data) =>
      for {
        _ <- ZIO.log(s"Create VDR entry bytes owner by ${didOwner.string}")
      } yield ()
    case CMD.VDRUpdateBytes(setup, vdrEntryRef, vdrKeyLabel, vdrKeyRaw, data) =>
      for {
        _ <- ZIO.log(s"Update VDR entry $vdrEntryRef")
      } yield ()
    case CMD.VDRDeactivateEntry(setup, vdrEntryRef, vdrKeyLabel, vdrKeyRaw) =>
      for {
        _ <- ZIO.log(s"Deactivate VDR entry $vdrEntryRef")
      } yield ()
    case CMD.VDRFetchEntry(setup, vdrEntryRef) =>
      for {
        _ <- ZIO.log(s"Fetch VDR entry $vdrEntryRef")
      } yield ()
    case CMD.VDRProofEntry(setup, vdrEntryRef) =>
      for {
        _ <- ZIO.log(s"Generate proof for VDR entry $vdrEntryRef")
      } yield ()

}
