package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import zio.cli.*

object CommCommand {

  val command =
    Command("didcomm", ConfigCommand.options)
      .subcommands(
        Command("login").map(e => CMD.CommLogin(???, ???))
      )
    // .map { (config, flag) => CMD.ConfigCMD(config, flag) }

  def program(cmd: CMD.CommCMD) = ???

}
