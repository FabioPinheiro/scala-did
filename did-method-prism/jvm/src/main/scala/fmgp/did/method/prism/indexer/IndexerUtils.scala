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
            prismObject = cardanoPrismEntry.content,
            tx = cardanoPrismEntry.tx,
            blockIndex = cardanoPrismEntry.index,
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

  case class EventCounter(
      invalidPrismObject: Int = 0,
      invalidSignedPrismOperation: Int = 0,
      signedPrismOperation: Int = 0
  )
  def countEvents(implicit trace: Trace): ZSink[Any, Nothing, MaybeOperation[OP], Nothing, EventCounter] =
    ZSink.foldLeft(EventCounter())((ec, event) => {
      event match
        case InvalidPrismObject(tx, b, reason) =>
          println(reason)
          ec.copy(invalidPrismObject = ec.invalidPrismObject + 1)
        case InvalidSignedPrismOperation(tx, b, o, reason) =>
          ec.copy(invalidSignedPrismOperation = ec.invalidSignedPrismOperation + 1)
        case MySignedPrismOperation(tx, b, o, signedWith, signature, operation, protobuf) =>
          ec.copy(signedPrismOperation = ec.signedPrismOperation + 1)

    })

  def loadPrismStateFromChunkFiles: ZIO[IndexerConfig, Throwable, Ref[PrismState]] = for {
    indexerConfig <- ZIO.service[IndexerConfig]
    chunkFilesAfter <- fmgp.did.method.prism.indexer.Indexer
      .findChunkFiles(rawMetadataPath = indexerConfig.rawMetadataPath)
    _ <- ZIO.log(s"Read chunkFiles")
    streamAllMaybeOperationFromChunkFiles = ZStream.fromIterable {
      chunkFilesAfter.map { fileName =>
        ZStream
          .fromFile(fileName)
          .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          .map { _.fromJson[CardanoMetadataCBOR].getOrElse(???) }
          .via(IndexerUtils.pipelineTransformCardanoMetadata2SeqEvents)
          .flatMap(e => ZStream.fromIterable(e))
      }
    }.flatten
    _ <- ZIO.log(s"Init PrismState")
    stateRef <- Ref.make(PrismState.empty)
    countEvents <- streamAllMaybeOperationFromChunkFiles
      .via(IndexerUtils.pipelinePrismState)
      .run(countEvents) // (ZSink.count)
      .provideEnvironment(ZEnvironment(stateRef))
    _ <- ZIO.log(s"Finish Init PrismState: $countEvents")
    state <- stateRef.get
    _ <- ZIO.log(s"PrismState was ${state.ssiCount} SSI")
    _ <- ZIO.log(s"PrismState was ${state.asInstanceOf[PrismStateInMemory].vdr2eventRef.count(_ => true)} VDR")
    // _ <- ZIO.log(s"PrismState was ${state.asInstanceOf[PrismStateInMemory].toJsonPretty}")
  } yield stateRef

}
