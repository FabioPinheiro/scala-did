package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import zio.cli.*
import java.nio.file.Path
import fmgp.did.method.prism.cardano.CardanoWalletConfig

object ConfigCommand {

  // def stagingPath: Options[Path] = Options.none
  //   .file("staging-path")
  //   .??("Staging file (to build complex operations)")

  def overrideFlag = Options
    .boolean("s")
    .??("Override the staging file")

  def options = overrideFlag // (stagingPath.optional ++ overrideFlag.optional)
    .map(
      mOverrideFlag => // (mStagingPath, mOverrideFlag) =>
        val mStagingPath: Option[Path] = None // FIXME REMOVE
        val autoload = { // AUTO LOAD
          Path.of(Setup.defaultCOnfigPath).toFile()
          val sourcePath = mStagingPath match
            case None       => Path.of(Setup.defaultCOnfigPath).toFile()
            case Some(path) => path.toFile()
          if (sourcePath.exists()) {
            val source = scala.io.Source.fromFile(sourcePath)
            val data = source.getLines.reduceLeft(_ + _)
            data.fromJson[StagingState]
          } else Left(s"Staging file '${sourcePath.toString}' does not exist")
        }
        Setup(
          stagingPath = mStagingPath.getOrElse(Path.of(Setup.defaultCOnfigPath)),
          // updateStateFile = mOverrideFlag.orElse(autoload.map(_.updateStateFileByDefault).toOption).getOrElse(false),
          updateStateFile = mOverrideFlag | (autoload.map(_.updateStateFileByDefault).toOption).getOrElse(false),
          staging = autoload
        )
    )

  def createFlag = Options
    .boolean("create")
    .??("Create new staging file if not existe")

  val command =
    Command("staging", options ++ createFlag)
      .map { (config, flag) => CMD.ConfigCMD(config, flag) }

  def program(cmd: CMD.ConfigCMD) = cmd match
    case CMD.ConfigCMD(config, createFlag) =>
      for {
        _ <- ZIO.log(s"Command config: $cmd")
        mStaging <-
          if (!createFlag) config.staging match
            case Left(value)  => ZIO.logWarning(value)
            case Right(value) => Console.printLine(value.toJsonPretty)
          else {
            val tmp = StagingState(test = "new")
            ZIO
              .writeFile(config.stagingPath, tmp.toJsonPretty)
              .map(_ => Right(tmp))
              .catchAll(ex => ZIO.succeed(Left(ex.getMessage())))
              *> Console.printLine(tmp.toJsonPretty)
          }
      } yield ()
}
