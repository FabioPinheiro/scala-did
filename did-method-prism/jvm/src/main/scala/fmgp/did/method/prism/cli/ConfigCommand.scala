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
    .map { mOverrideFlag => optionsWith(mOverrideFlag) } // (mStagingPath, mOverrideFlag) =>
  def optionsDefualt = Options.none // (stagingPath.optional ++ overrideFlag.optional)
    .map { e => optionsWith(false) } // (mStagingPath, mOverrideFlag) =>

  def optionsWith(mOverrideFlag: Boolean) = {
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
  }

  def createFlag = Options
    .boolean("create")
    .??("Create new staging file if does not exist")

  val command =
    Command("config-file", optionsDefualt ++ createFlag)
      .map { (config, flag) => CMD.ConfigCMD(config, flag) }

  def program(cmd: CMD.ConfigCMD) = cmd match
    case CMD.ConfigCMD(config, createFlag) =>
      for {
        _ <- ZIO.log(s"Command config: $cmd")
        mStaging <- (createFlag, config.staging) match
          case Tuple2(true, Right(value)) =>
            val msg = s"Cannot create file ${config.stagingPath} becuase it already exists"
            for {
              _ <- ZIO.log(msg)
              _ <- ZIO.log(value.toJson)
              _ <- Console.printLine(msg)
              _ <- Console.printLine(value.toJsonPretty)
            } yield ()
          case Tuple2(true, Left(noFileReason)) =>
            val tmp = StagingState(test = "new file")
            for {
              _ <- ZIO.log(s"Creating file ${config.stagingPath} with ${tmp.toJson}")
              output <- ZIO
                .writeFile(config.stagingPath, tmp.toJsonPretty)
                .map(_ => Right(tmp))
                .catchAll(ex => ZIO.succeed(Left(ex.getMessage())))
              _ <- output match
                case Left(error)  => ZIO.logError(s"FAIL with $error")
                case Right(state) => Console.printLine(tmp.toJsonPretty)
            } yield ()
          case Tuple2(false, Right(state)) =>
            Console.printLine(s"Config file contains the following settings: ${state.toJsonPretty}")
          case Tuple2(false, Left(noFileReason)) =>
            Console.printLine(s"Config file not found: $noFileReason")

      } yield ()
}
