package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*

object VDRCommand {

  val command =
    Command("didcomm")
      .subcommands(
        Command("create-bytes", ConfigCommand.options).map(setup =>
          CMD.VDRCreateBytes(
            setup = setup,
            vdrLabel = ???,
            vdrRaw = ???,
            data = ???,
          )
        ),
        Command("update-bytes", ConfigCommand.options).map(setup => CMD.VDRUpdateBytes(???)),
        Command("deactivate", ConfigCommand.options).map(setup => CMD.VDRDeactivate(???)),
      )
  // .map { (config, flag) => CMD.ConfigCMD(config, flag) }

  def program(cmd: CMD.VDRCMD) = ???

}
