package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*

import fmgp.did.method.prism.vdr

object IndexerCommand {

  def command: Command[Subcommand.Indexer] =
    Command("indexer", blockfrostConfigOpt, indexerWorkDirAgr)
      .map { case (mBlockfrostConfig, workdir) => Subcommand.Indexer(workdir, mBlockfrostConfig) }

  def program(cmd: Subcommand.Indexer): ZIO[Any, Throwable, Unit] = cmd match {
    case cmd @ Subcommand.Indexer(workdir, mBlockfrostConfig) =>
      for {
        _ <- vdr.Indexer.indexerLogo
        indexerConfig = vdr.IndexerConfig(
          mBlockfrostConfig = mBlockfrostConfig,
          workdir = workdir.toAbsolutePath().normalize.toString
        )
        _ <- ZIO.log(s"IndexerConfig: `${indexerConfig}`")
        indexerConfigZLayer = ZLayer.succeed(indexerConfig) // vdr.Indexer.makeIndexerConfigZLayerFromArgs
        _ <- vdr.Indexer.indexerJob.provideLayer(indexerConfigZLayer)
      } yield ()
  }
}
