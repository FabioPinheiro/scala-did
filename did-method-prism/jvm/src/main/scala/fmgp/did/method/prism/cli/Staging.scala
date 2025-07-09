package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import zio.cli.*
import java.nio.file.Path
import fmgp.did.method.prism.cardano.CardanoWalletConfig

object Staging {

  def stagingPath: Options[Path] = Options
    .file("staging-path")
    .??("Staging file (to build complex operations)")

  def overrideFlag = Options
    .boolean("staging-override")
    .alias("s")
    .??("Override the staging file")

  def options = (stagingPath.optional ++ overrideFlag.optional)
    .map((mStagingPath, mOverrideFlag) =>
      val autoload = { // AUTO LOAD
        val sourcePath = mStagingPath match
          case None       => Path.of("staging.json").toFile()
          case Some(path) => path.toFile()
        if (sourcePath.exists()) {
          val source = scala.io.Source.fromFile(sourcePath)
          val data = source.getLines.reduceLeft(_ + _)
          data.fromJson[StagingState]
        } else Left(s"Staging file '${sourcePath.toString}' does not exist")
      }
      Setup(
        stagingPath = mStagingPath.getOrElse(Path.of("staging.json")),
        updateStateFile = mOverrideFlag.orElse(autoload.map(_.updateStateFileByDefault).toOption).getOrElse(false),
        staging = autoload
      )
    )

  def createFlag = Options
    .boolean("create")
    .??("Create new staging file if not existe")

  val command =
    Command("staging", Staging.options ++ createFlag)
      .map { (config, flag) => Subcommand.Staging(config, flag) }

  def program(cmd: Subcommand.Staging) = cmd match
    case Subcommand.Staging(config, createFlag) =>
      for {
        _ <- ZIO.log(s"Command Staging: $cmd")
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
