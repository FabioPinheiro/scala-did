package fmgp.did.method.prism.vdr

import zio.*
import zio.json.*
import zio.stream.*
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.proto.*

/** IndexerUtils
  *
  * @example
  *   {{{
  *     IndexerUtils.pipelineTransformCardanoMetadata2Events >>> IndexerUtils.pipelinePrismState >>> IndexerUtils.countEvents
  *   }}}
  */
object IndexerUtils {

  def pipelineTransformCardanoMetadata2SeqEvents: ZPipeline[Any, Nothing, CardanoMetadata, Seq[MaybeEvent[OP]]] =
    ZPipeline.map[CardanoMetadata, Seq[MaybeEvent[OP]]](cardanoMetadata =>
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
          MaybeEvent.fromProto(
            prismObject = cardanoPrismEntry.content,
            tx = cardanoPrismEntry.tx,
            blockIndex = cardanoPrismEntry.index,
          )
    )

  def pipelineTransformCardanoMetadata2Events: ZPipeline[Any, Nothing, CardanoMetadata, MaybeEvent[OP]] =
    pipelineTransformCardanoMetadata2SeqEvents.flattenIterables

  /** pipeline to load/initiate the PrismState from stream of all events */
  def pipelinePrismState = ZPipeline.mapZIO[PrismState, Nothing, MaybeEvent[OP], MaybeEvent[OP]] { maybeEvent =>
    maybeEvent match
      case InvalidPrismObject(tx, b, reason)         => ZIO.succeed(maybeEvent)
      case InvalidSignedPrismEvent(tx, b, o, reason) => ZIO.succeed(maybeEvent)
      case op: MySignedPrismEvent[OP]                =>
        for {
          state <- ZIO.service[PrismState]
          _ <- state.addEvent(op).orDie // TODO die
        } yield (maybeEvent)
  }

  case class EventCounter(
      invalidPrismObject: Int = 0,
      invalidSignedPrismEvent: Int = 0,
      signedPrismEvent: Int = 0
  )
  def countEvents(implicit trace: Trace): ZSink[Any, Nothing, MaybeEvent[OP], Nothing, EventCounter] =
    ZSink.foldLeft(EventCounter())((ec, event) => {
      event match
        case InvalidPrismObject(tx, b, reason) =>
          println(reason)
          ec.copy(invalidPrismObject = ec.invalidPrismObject + 1)
        case InvalidSignedPrismEvent(tx, b, o, reason) =>
          ec.copy(invalidSignedPrismEvent = ec.invalidSignedPrismEvent + 1)
        case MySignedPrismEvent(tx, b, o, signedWith, signature, protobuf) =>
          ec.copy(signedPrismEvent = ec.signedPrismEvent + 1)
    })

  def loadPrismStateFromChunkFiles: ZIO[IndexerConfig & PrismState, Throwable, PrismState] = for {
    indexerConfig <- ZIO.service[IndexerConfig]
    chunkFilesAfter <- fmgp.did.method.prism.vdr.Indexer
      .findChunkFiles(rawMetadataPath = indexerConfig.rawMetadataPath)
    _ <- ZIO.log(s"Read chunkFiles (${chunkFilesAfter.length} files)")
    streamAllMaybeEventFromChunkFiles = ZStream.fromIterable {
      chunkFilesAfter.map { fileName =>
        ZStream
          .fromFile(fileName)
          .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          .map { _.fromJson[CardanoMetadataCBOR].getOrElse(???) }
          .via(IndexerUtils.pipelineTransformCardanoMetadata2Events)
      }
    }.flatten
    _ <- ZIO.log(s"Init PrismState")
    state <- ZIO.service[PrismState]
    countEvents <- streamAllMaybeEventFromChunkFiles
      .via(IndexerUtils.pipelinePrismState)
      .run(IndexerUtils.countEvents) // (ZSink.count)
      .provideEnvironment(ZEnvironment(state: PrismState))
    _ <- ZIO.log(s"Finish Init PrismState: $countEvents")
    ssiCount <- state.ssiCount
    vdrCount <- state.vdrCount
    _ <- ZIO.log(s"PrismState was $ssiCount SSI and $vdrCount VDR")
  } yield state

}
