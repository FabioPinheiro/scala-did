package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*

import fmgp.did.method.prism.vdr
import fmgp.did.method.prism.IndexerConfig

object IndexerCommand {

  def command: Command[CMD.Indexer] =
    Command("indexer", blockfrostConfigOpt, indexerWorkDirAgr)
      .map { case (mBlockfrostConfig, workdir) => CMD.Indexer(workdir, mBlockfrostConfig) }

  def program(cmd: CMD.Indexer): ZIO[Any, Throwable, Unit] = cmd match {
    case cmd @ CMD.Indexer(workdir, mBlockfrostConfig) =>
      for {
        _ <- vdr.Indexer.indexerLogo
        indexerConfig = IndexerConfig(
          mBlockfrostConfig = mBlockfrostConfig,
          workdir = workdir.toAbsolutePath().normalize.toString
        )
        _ <- ZIO.log(s"IndexerConfig: `${indexerConfig}`")
        indexerConfigZLayer = ZLayer.succeed(indexerConfig) // vdr.Indexer.makeIndexerConfigZLayerFromArgs
        _ <- vdr.Indexer.indexerJob.provideLayer(indexerConfigZLayer)
      } yield ()
  }
}
