package fmgp.did.method.prism.indexer

import zio.*
import zio.json.*
import zio.stream.*
import fmgp.did.method.prism._
import fmgp.did.method.prism.cardano._
import fmgp.did.method.prism.proto._

object IndexerUtils {

  def pipelineTransformCardanoMetadata2SeqEvents: ZPipeline[Any, Nothing, CardanoMetadata, Seq[MaybeOperation[OP]]] =
    ZPipeline.map[CardanoMetadata, Seq[MaybeOperation[OP]]](cardanoMetadata =>
      cardanoMetadata.toCardanoPrismEntry match
        case Left(error) =>
          Seq(
            InvalidPrismObject(
              tx = cardanoMetadata.tx,
              b = cardanoMetadata.b,
              reason = error,
            )
          )
        case Right(cardanoPrismEntry) =>
          MaybeOperation.fromProto(
            tx = cardanoPrismEntry.tx,
            blockIndex = cardanoPrismEntry.index,
            prismObject = cardanoPrismEntry.content
          )
    )

  /** pipeline to load/initiate the PrismState from stream of all events */
  def pipelinePrismState = ZPipeline.mapZIO[Ref[PrismState], Nothing, MaybeOperation[OP], MaybeOperation[OP]] {
    maybeOperation =>
      maybeOperation match
        case InvalidPrismObject(tx, b, reason)             => ZIO.succeed(maybeOperation)
        case InvalidSignedPrismOperation(tx, b, o, reason) => ZIO.succeed(maybeOperation)
        case op: MySignedPrismOperation[OP] =>
          for {
            refState <- ZIO.service[Ref[PrismState]]
            _ <- refState.update(_.addEvent(op))
          } yield (maybeOperation)
  }

  def loadPrismStateFromChunkFiles: ZIO[IndexerConfig, Throwable, Ref[PrismState]] = for {
    indexerConfig <- ZIO.service[IndexerConfig]
    chunkFilesAfter <- fmgp.did.method.prism.indexer.Indexer
      .findChunkFiles(rawMetadataPath = indexerConfig.rawMetadataPath)
    _ <- ZIO.log(s"Read chunkFiles")
    streamAllChunkFiles = ZStream.fromIterable {
      chunkFilesAfter.map { fileName =>
        ZStream
          .fromFile(fileName)
          .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          .map { _.fromJson[CardanoMetadataCBOR].getOrElse(???) }
          .via(IndexerUtils.pipelineTransformCardanoMetadata2SeqEvents)
          .flatMap(e => ZStream.fromIterable(e))
      }
    }.flatten
    streamMetadata = streamAllChunkFiles
    _ <- ZIO.log(s"Init PrismState")
    stateRef <- Ref.make(PrismState.empty)
    countEvents <- streamMetadata
      .via(IndexerUtils.pipelinePrismState)
      .run(ZSink.count)
      .provideEnvironment(ZEnvironment(stateRef))
    _ <- ZIO.log(s"Finish Init PrismState: $countEvents")
    state <- stateRef.get
    _ <- ZIO.log(s"PrismState was ${state.ssiCount} SSI")
  } yield stateRef

}
