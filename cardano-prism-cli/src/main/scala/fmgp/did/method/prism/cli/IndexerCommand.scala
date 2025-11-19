package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*

import fmgp.did.method.prism.vdr
import fmgp.did.method.prism.IndexerConfig
import fmgp.did.method.prism.PrismStateInMemory
import fmgp.did.method.prism.PrismStateMongoDB
import fmgp.did.method.prism.mongo.AsyncDriverResource

object IndexerCommand {

  def command: Command[CMD.Indexer] =
    Command("indexer", Options.none)
      .subcommands(
        indexerInMemory,
        indexerMongoDB,
      )

  def indexerInMemory =
    Command("in-memory", blockfrostConfig.optional, indexerWorkDirAgr)
      .map { case (mBlockfrostConfig, workdir) => CMD.IndexerInMemory(workdir, mBlockfrostConfig) }
  def indexerMongoDB =
    Command("mongodb", blockfrostConfig, indexerDBConnectionAgr)
      .map { case (blockfrostConfig, mongoDBConnection) =>
        CMD.IndexerMongoDB(mongoDBConnection, blockfrostConfig)
      }

  def program(cmd: CMD.Indexer): ZIO[Any, Throwable, Unit] = cmd match {
    case cmd @ CMD.IndexerInMemory(workdir, mBlockfrostConfig) =>
      for {
        _ <- vdr.Indexer.indexerLogo
        indexerConfig = IndexerConfig(
          mBlockfrostConfig = mBlockfrostConfig,
          workdir = workdir.toAbsolutePath().normalize.toString
        )
        _ <- ZIO.log(s"IndexerConfig: `${indexerConfig}`")
        indexerConfigZLayer = ZLayer.succeed(indexerConfig) // vdr.Indexer.makeIndexerConfigZLayerFromArgs
        prismStateZLayer = ZLayer(PrismStateInMemory.empty)
        _ <- vdr.Indexer.indexerJobFS.provideLayer(indexerConfigZLayer ++ prismStateZLayer)
      } yield ()
    case cmd @ CMD.IndexerMongoDB(mongoDBConnection, blockfrostConfig) =>
      for {
        _ <- ZIO.log(s"Indexer start: $mongoDBConnection")
        prismStateZLayer = AsyncDriverResource.layer >>> PrismStateMongoDB.makeLayer(mongoDBConnection)
        blockfrostConfigLayer = ZLayer.succeed(blockfrostConfig)
        eventCounter <- vdr.Indexer.indexerJobDB.provideLayer(prismStateZLayer ++ blockfrostConfigLayer)
        // _ <- ZIO.log(s"IndexerJobDB Update with $eventCounter")
      } yield ()
  }

}
