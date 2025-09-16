package fmgp.did.method.prism.vdr

import fmgp.blockfrost.*
import zio.*
import zio.http.*
import zio.json.*
import zio.stream.*

import java.nio.file.StandardOpenOption.*
import scala.util._
import fmgp.did._
import fmgp.did.method.prism._
import fmgp.did.method.prism.cardano._
import fmgp.did.method.prism.proto._

/** Indexer
  *
  * {{{
  * didResolverPrismJVM/runMain fmgp.did.method.prism.indexer.Indexer ../prism-vdr/prismnet preprodTOKEN
  * }}}
  */
object Indexer extends ZIOAppDefault {

  val PAGE_SIZE = 100

  /** Transactions per chunk file
    *
    * Maybe 1000 is too small
    *
    * (100 megabytes) / ((16 quilobytes) + (96 bytes)) = 6212?
    */
  val FILE_CHUNK_SIZE = 1000
  val FILE_CHUNK_NAME = "chunk"

  def metadataFromJsonAPI(blockfrostConfig: BlockfrostConfig, pageJump: Int = 0) = ZStream
    .paginateChunkZIO(pageJump) { pageNumber =>
      for {
        _ <- ZIO.log(s"pageNumber=$pageNumber; (entries ${pageNumber * PAGE_SIZE}-${(pageNumber + 1) * PAGE_SIZE})")
        urlRequest = API.metadataContentJson(
          network = blockfrostConfig.network,
          label = PRISM_LABEL_CIP_10,
          page = pageNumber + 1,
          count = PAGE_SIZE
        )
        _ <- ZIO.log(s"Next request call: $urlRequest") // For Debug
        response <- Client.batched(
          Request.get(path = urlRequest).addHeaders(Headers(Header.Custom("project_id", blockfrostConfig.token)))
        )
        responseStr <- response.body.asString
        page <- responseStr.fromJson[Either[BlockfrostErrorResponse, Seq[MetadataContentJson]]](
          MetadataContentJson.decoderSeqOrError
        ) match
          case Left(error) => ZIO.logError(responseStr) *> ZIO.fail(new RuntimeException("fail to parse"))
          case Right(Left(value: BlockfrostErrorResponse)) =>
            ZIO.logError(s"Got a BlockfrostErrorResponse: '$value' from the call '$urlRequest'")
              *> ZIO.succeed(Seq.empty)
          case Right(Right(value: Seq[MetadataContentJson])) =>
            ZIO.succeed(
              value.zipWithIndex.map((metadataContent, index) =>
                CardanoMetadataJson(
                  pageNumber * PAGE_SIZE + index,
                  metadataContent.tx_hash,
                  metadataContent.json_metadata
                )
              )
            )
      } yield (Chunk.fromIterable(page), if (page.size == PAGE_SIZE) Some(pageNumber + 1) else None)
    }
    .provideSomeLayer(Client.default ++ Scope.default)

  def metadataFromCBORAPI(blockfrostConfig: BlockfrostConfig, pageJump: Int = 0) = ZStream
    .paginateChunkZIO(pageJump) { pageNumber =>
      for {
        _ <- ZIO.log(s"pageNumber=$pageNumber; (entries ${pageNumber * PAGE_SIZE}-${(pageNumber + 1) * PAGE_SIZE})")
        urlRequest = API.metadataContentCBOR(
          network = blockfrostConfig.network,
          label = PRISM_LABEL_CIP_10,
          page = pageNumber + 1,
          count = PAGE_SIZE
        )
        _ <- ZIO.log(s"Next request call: $urlRequest") // For Debug
        response <- Client.batched(
          Request.get(path = urlRequest).addHeaders(Headers(Header.Custom("project_id", blockfrostConfig.token)))
        )
        responseStr <- response.body.asString

        page <- responseStr.fromJson[Either[BlockfrostErrorResponse, Seq[MetadataContentCBOR]]](
          MetadataContentCBOR.decoderSeqOrError
        ) match
          case Left(error) => ZIO.logError(responseStr) *> ZIO.fail(new RuntimeException(s"fail to parse: $error"))
          case Right(Left(value: BlockfrostErrorResponse)) =>
            ZIO.logError(s"Got a BlockfrostErrorResponse: '$value' from the call '$urlRequest'")
              *> ZIO.succeed(Seq.empty)
          case Right(Right(value: Seq[MetadataContentCBOR])) =>
            ZIO.succeed(
              value.zipWithIndex.map((metadataContent, index) =>
                CardanoMetadataCBOR(pageNumber * PAGE_SIZE + index, metadataContent.tx_hash, metadataContent.metadata)
              )
            )
      } yield (Chunk.fromIterable(page), if (page.size == PAGE_SIZE) Some(pageNumber + 1) else None)
    }
    .provideSomeLayer(Client.default ++ Scope.default)

  def sinkLog: ZSink[Any, java.io.IOException, MaybeOperation[OP], Nothing, Unit] = ZSink.foreach {
    case InvalidPrismObject(tx, b, reason)             => Console.printLineError(s"$b - tx:$tx - $reason")
    case InvalidSignedPrismOperation(tx, b, o, reason) => Console.printLineError(s"$b - tx:$tx op:$o - $reason")
    case op @ MySignedPrismOperation(tx, b, o, signedWith, signature, pb) =>
      val str = op.operation match
        case CreateDidOP(publicKeys, services, context) => s"CreateDidOP: did:prism:${op.opHash}"
        case UpdateDidOP(previousOperationHash, id, actions) =>
          s"UpdateDidOP: previousOperationHash=$previousOperationHash}"
        case _ => op.operation.getClass.getName
      Console.printLine(s"$b - tx:$tx op:$o - $str")
  }

  def findChunkFiles(rawMetadataPath: String) =
    Try(
      Option(
        new java.io.File(rawMetadataPath).listFiles()
      ) match
        case None => // becuase of: Exception when loading chunks: java.lang.NullPointerException: Cannot invoke "Object.getClass()" because "$this" is null
          val msg = s"Unable to load folder with chunks from: $rawMetadataPath"
          ZIO.logError(msg) *> ZIO.fail(new RuntimeException(msg))
        case Some(files) =>
          ZIO.log(s"Chunks Files: ${files.mkString("; ")}") *>
            ZIO.succeed(files.filter(_.getName.startsWith(FILE_CHUNK_NAME)).sorted)
    ) match
      case Failure(exception) => ZIO.logError(s"Exception when loading chunks: $exception") *> ZIO.fail(exception)
      case Success(value)     => value

  def sinkRawTransactions(filePath: String): ZSink[Any, Throwable, CardanoMetadataCBOR, Nothing, Unit] =
    ZSink
      .fromFileName(name = filePath, options = Set(WRITE, APPEND, CREATE, SYNC))
      .contramapChunks[CardanoMetadataCBOR](_.flatMap { cm => (cm.toJson + "\n").getBytes })
      .ignoreLeftover // TODO review
      .mapZIO(bytes => ZIO.log(s"ZSink PRISM Events into $filePath (write $bytes bytes)"))

  def indexerLogo = Console.printLine(
    """██████╗ ██████╗ ██╗███████╗███╗   ███╗    ██╗   ██╗██████╗ ██████╗ 
      |██╔══██╗██╔══██╗██║██╔════╝████╗ ████║    ██║   ██║██╔══██╗██╔══██╗
      |██████╔╝██████╔╝██║███████╗██╔████╔██║    ██║   ██║██║  ██║██████╔╝
      |██╔═══╝ ██╔══██╗██║╚════██║██║╚██╔╝██║    ╚██╗ ██╔╝██║  ██║██╔══██╗
      |██║     ██║  ██║██║███████║██║ ╚═╝ ██║     ╚████╔╝ ██████╔╝██║  ██║
      |╚═╝     ╚═╝  ╚═╝╚═╝╚══════╝╚═╝     ╚═╝      ╚═══╝  ╚═════╝ ╚═╝  ╚═╝
      |                                                                   
      |       ██╗███╗   ██╗██████╗ ███████╗██╗  ██╗███████╗██████╗        
      |       ██║████╗  ██║██╔══██╗██╔════╝╚██╗██╔╝██╔════╝██╔══██╗       
      |       ██║██╔██╗ ██║██║  ██║█████╗   ╚███╔╝ █████╗  ██████╔╝       
      |       ██║██║╚██╗██║██║  ██║██╔══╝   ██╔██╗ ██╔══╝  ██╔══██╗       
      |       ██║██║ ╚████║██████╔╝███████╗██╔╝ ██╗███████╗██║  ██║       
      |       ╚═╝╚═╝  ╚═══╝╚═════╝ ╚══════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝       
      |PRISM - Verifiable Data Registry (VDR) Indexer
      |Vist: https://github.com/FabioPinheiro/scala-did
      |
      |DID PRISM resolves:
      |- https://statistics.blocktrust.dev/resolve
      |- https://neoprism.patlo.dev/explorer
      |
      |Tools: 
      |- https://protobuf-decoder.netlify.app/
      |""".stripMargin
  )

  def makeIndexerConfigZLayerFromArgs = getArgs
    .map(_.toSeq)
    .flatMap {
      case Seq(dataPath: String) =>
        ZIO.succeed(IndexerConfig(mBlockfrostConfig = None, workdir = dataPath))
      case Seq(dataPath, apikey) =>
        ZIO.succeed(IndexerConfig(mBlockfrostConfig = Some(BlockfrostConfig(apikey)), workdir = dataPath))
      case next =>
        ZIO.logWarning(s"Fail to parse indexerConfig from '${next.mkString(" ")}'") *>
          ZIO.fail(RuntimeException("Indexer <dataPath> [<TOKEN_mainnet|preprod|preview>]"))
    }
    .map(ZLayer.succeed _)

  def indexerJob = for {
    indexerConfig <- ZIO.service[IndexerConfig]
    _ <- ZIO.log(s"Check the LastTransactionIndexStored")
    chunkFilesBeforeStart <- findChunkFiles(rawMetadataPath = indexerConfig.rawMetadataPath)
    lastTransactionIndexStored <- chunkFilesBeforeStart.lastOption match
      case None => ZIO.succeed(None)
      case Some(lastChunkFilesPath) =>
        ZStream
          .fromFile(lastChunkFilesPath)
          .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          .takeRight(1) // last line
          .map { _.fromJson[CardanoMetadataCBOR].getOrElse(???) }
          .map(_.index)
          .runCollect
          .map(_.headOption)

    nextMetadateIndex = lastTransactionIndexStored.map(_ + 1).getOrElse(0)
    cardinalityOfEntries = nextMetadateIndex + 1
    nextPageIndex = cardinalityOfEntries / 100
    _ <- ZIO.log(s"API calls start: nextPageIndex=$nextPageIndex; nextMetadateIndex=$nextMetadateIndex")

    _ <- indexerConfig.mBlockfrostConfig match
      case None => ZIO.logWarning("Token for blockfrost is missing") *> ZIO.log("Indexing from ofline file")
      case Some(blockfrostConfig) =>
        metadataFromCBORAPI(
          blockfrostConfig = blockfrostConfig,
          pageJump = nextPageIndex,
        )
          .filter(_.index >= nextMetadateIndex) // .drop(nextItemIndex + 1)
          .groupByKey(_.index / FILE_CHUNK_SIZE) { (fileN, stream) =>
            stream.tapSink {
              val fileName = f"$FILE_CHUNK_NAME${fileN}%03.0f"
              sinkRawTransactions(indexerConfig.rawMetadataPath + s"/$fileName")
            }
          }
          .run(ZSink.drain)
          *> ZIO.log(s"End updating the raw metadata '${indexerConfig.rawMetadataPath}'")

    // Load PRISM State from chunk files
    refPrismState <- IndexerUtils.loadPrismStateFromChunkFiles // .provideLayer(indexerConfigZLayer)
    state <- refPrismState.get
    // ###############################################

    _ <- ZStream
      .fromIterable(state.ssi2eventsId)
      .mapZIO { case (did, opidSeq) =>
        for {
          _ <- ZIO.logDebug(s"DID: $did")
          events <- state.getEventsForSSI(did)
          _ <- ZStream.fromIterableZIO(state.getEventsForSSI(did)).run {
            ZSink
              .fromFileName(
                name = indexerConfig.ssiEventsPath(did),
                options = Set(WRITE, TRUNCATE_EXISTING, CREATE)
              )
              .contramapChunks[MySignedPrismOperation[OP]](_.flatMap { spo => s"${spo.toJson}\n".getBytes })
          }
          ssi = fmgp.did.method.prism.SSI.make(did, events)
          _ <- ZIO.when(ssi.latestHash.isDefined)(for {
            _ <- ZStream.from(ssi).run {
              ZSink
                .fromFileName(name = indexerConfig.ssiPath(did), options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[SSI](_.flatMap { case ssi => s"${ssi.toJsonPretty}\n".getBytes })
            }
            _ <- ZStream.from(ssi).run {
              ZSink
                .fromFileName(name = indexerConfig.diddocPath(did), options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[SSI](
                  _.flatMap(_.didDocument)
                    .flatMap { case doc => s"${doc.toJsonPretty}\n".getBytes }
                )
            }
          } yield ())
        } yield ()
      }
      .run(ZSink.count)

    _ <- ZStream
      .fromIterable(state.asInstanceOf[PrismStateInMemory].vdr2eventRef) // FIXME
      .mapZIO { case (ref, events) =>
        for {
          _ <- ZIO.logDebug(s"VDR: $ref")
          _ <- ZStream.fromIterableZIO(state.getEventsForVDR(ref)).run {
            ZSink
              .fromFileName(
                name = indexerConfig.vdrEventsPath(ref),
                options = Set(WRITE, TRUNCATE_EXISTING, CREATE)
              )
              .contramapChunks[MySignedPrismOperation[OP]](_.flatMap { spo => s"${spo.toJson}\n".getBytes })
          }
          vdr <- state.getVDR(ref)
          _ <- ZStream.from(vdr).run {
            ZSink
              .fromFileName(name = indexerConfig.vdrPath(ref), options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
              .contramapChunks[VDR](_.flatMap { case vdr => s"${vdr.toJsonPretty}\n".getBytes })
          }
        } yield ()
      }
      .run(ZSink.count)
  } yield ()

  override val run = {
    for {
      _ <- indexerLogo
      indexerConfigZLayer <- makeIndexerConfigZLayerFromArgs
      _ <- indexerJob.provideLayer(indexerConfigZLayer)
    } yield ()
  }

}
