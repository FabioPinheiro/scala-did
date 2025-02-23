package fmgp.prism

import java.nio.file.StandardOpenOption._

import zio._
import zio.stream._
import zio.json._
import zio.http._

import fmgp.blockfrost._

/** @param apiKey
  *   blockfrost API key
  */
case class IndexerConfig(apiKey: Option[String], workdir: String, network: String) {
  def rawMetadataPath = s"$workdir/cardano-21325"
  def eventsPath = s"$workdir/prism-events"
  // def statePath = s"$workdir/prism-state.json"

  def opidPath(did: String) = s"$workdir/opid/$did"
  def opsPath(did: String) = s"$workdir/ops/$did"
  def ssiPath(did: String) = s"$workdir/ssi/$did"
  def diddocPath(did: String) = s"$workdir/diddoc/$did"
}

object Indexer extends ZIOAppDefault {

  val PRISM_LABEL_CIP_10 = "21325"
  val PAGE_SIZE = 100

  /** Transactions per chunk file
    *
    * Maybe 1000 is too small
    *
    * (100 megabytes) / ((16 quilobytes) + (96 bytes)) = 6212?
    */
  val FILE_CHUNK_SIZE = 1000
  val FILE_CHUNK_NAME = "chunk"

  def metadataFromJsonAPI(apiKey: String, network: String, pageJump: Int = 0) = ZStream
    .paginateChunkZIO(pageJump) { pageNumber =>
      for {
        _ <- ZIO.log(s"pageNumber=$pageNumber; (entries ${pageNumber * PAGE_SIZE}-${(pageNumber + 1) * PAGE_SIZE})")
        response <- Client
          .batched(
            Request
              .get(path =
                API.metadataContentJson(
                  network = network,
                  label = PRISM_LABEL_CIP_10,
                  page = pageNumber + 1,
                  count = PAGE_SIZE
                )
              )
              .addHeaders(Headers(Header.Custom("project_id", apiKey)))
          )
        responseStr <- response.body.asString
        page <- responseStr.fromJson[Seq[MetadataContentJson]] match
          case Left(error) => ZIO.logError(responseStr) *> ZIO.fail(new RuntimeException("fail to parse"))
          case Right(value) =>
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

  def metadataFromCBORAPI(apiKey: String, network: String, pageJump: Int = 0) = ZStream
    .paginateChunkZIO(pageJump) { pageNumber =>
      for {
        _ <- ZIO.log(s"pageNumber=$pageNumber; (entries ${pageNumber * PAGE_SIZE}-${(pageNumber + 1) * PAGE_SIZE})")
        response <- Client
          .batched(
            Request
              .get(path =
                API.metadataContentCBOR(
                  network = network,
                  label = PRISM_LABEL_CIP_10,
                  page = pageNumber + 1,
                  count = PAGE_SIZE
                )
              )
              .addHeaders(Headers(Header.Custom("project_id", apiKey)))
          )
        responseStr <- response.body.asString
        page <- responseStr.fromJson[Seq[MetadataContentCBOR]] match
          case Left(error) => ZIO.logError(responseStr) *> ZIO.fail(new RuntimeException(s"fail to parse: $error"))
          case Right(value) =>
            ZIO.succeed(
              value.zipWithIndex.map((metadataContent, index) =>
                CardanoMetadataCBOR(pageNumber * PAGE_SIZE + index, metadataContent.tx_hash, metadataContent.metadata)
              )
            )
      } yield (Chunk.fromIterable(page), if (page.size == PAGE_SIZE) Some(pageNumber + 1) else None)
    }
    .provideSomeLayer(Client.default ++ Scope.default)

  def pipeline = ZPipeline.map[CardanoMetadata, Seq[MaybeOperation[OP]]](cardanoMetadata =>
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

  /** pipeline to update the State */
  def pipelineState = ZPipeline.mapZIO[Ref[State], Nothing, MaybeOperation[OP], MaybeOperation[OP]] { maybeOperation =>
    maybeOperation match
      case InvalidPrismObject(tx, b, reason)             => ZIO.succeed(maybeOperation)
      case InvalidSignedPrismOperation(tx, b, o, reason) => ZIO.succeed(maybeOperation)
      case op: MySignedPrismOperation[OP] =>
        for {
          refState <- ZIO.service[Ref[State]]
          _ <- refState.update(_.addOp(op))
        } yield (maybeOperation)
  }

  def sinkLog: ZSink[Any, java.io.IOException, MaybeOperation[OP], Nothing, Unit] = ZSink.foreach {
    case InvalidPrismObject(tx, b, reason)             => Console.printLineError(s"$b - tx:$tx - $reason")
    case InvalidSignedPrismOperation(tx, b, o, reason) => Console.printLineError(s"$b - tx:$tx op:$o - $reason")
    case op @ MySignedPrismOperation(tx, b, o, signedWith, signature, operation, pb) =>
      val str = operation match
        case CreateDidOP(publicKeys, services, context) => s"CreateDidOP: did:prism:${op.opHash}"
        case UpdateDidOP(previousOperationHash, id, actions) =>
          s"UpdateDidOP: previousOperationHash=$previousOperationHash}"
        case _ => operation.getClass.getName
      Console.printLine(s"$b - tx:$tx op:$o - $str")
  }

  def findChunkFiles(rawMetadataPath: String) =
    new java.io.File(rawMetadataPath)
      .listFiles()
      .filter(_.getName.startsWith(FILE_CHUNK_NAME))
      .sorted

  def sinkRawTransactions(filePath: String): ZSink[Any, Throwable, CardanoMetadataCBOR, Nothing, Unit] =
    ZSink
      .fromFileName(name = filePath, options = Set(WRITE, APPEND, CREATE, SYNC))
      .contramapChunks[CardanoMetadataCBOR](_.flatMap { cm => (cm.toJson + "\n").getBytes })
      .ignoreLeftover // TODO review
      .mapZIO(bytes => ZIO.log(s"ZSink PRISM Events into $filePath (write $bytes bytes)"))

  override val run = {
    for {
      _ <- Console.printLine(
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
      stateRef <- Ref.make(State.empty)
      indexerConfigZLayer <- getArgs
        .map(_.toSeq)
        .flatMap {
          case Seq(dataPath: String) =>
            ZIO.succeed(IndexerConfig(apiKey = None, network = Network.Mainnet, workdir = dataPath))
          case Seq(dataPath, "mainnet", apikey) =>
            ZIO.succeed(IndexerConfig(apiKey = Some(apikey), network = Network.Mainnet, workdir = dataPath))
          case Seq(dataPath, "preprod", apikey) =>
            ZIO.succeed(IndexerConfig(apiKey = Some(apikey), network = Network.Preprod, workdir = dataPath))
          case next => ZIO.fail(RuntimeException("Indexer <dataPath> [mainnet|preprod <dataPath>]"))
        }
        .map(ZLayer.succeed _)
      indexerConfig <- ZIO.service[IndexerConfig].provideLayer(indexerConfigZLayer)

      _ <- ZIO.log(s"Check the LastTransactionIndexStored")
      lastTransactionIndexStored <- findChunkFiles(rawMetadataPath = indexerConfig.rawMetadataPath).lastOption match
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

      _ <- indexerConfig.apiKey match
        case None => ZIO.logWarning("Token for blockfrost is missing") *> ZIO.log("Indexing from ofline file")
        case Some(apiKey) =>
          metadataFromCBORAPI(
            apiKey = apiKey,
            network = indexerConfig.network,
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

      streamAllChunkFiles = ZStream.fromIterable {
        findChunkFiles(rawMetadataPath = indexerConfig.rawMetadataPath).map { fileName =>
          ZStream
            .fromFile(fileName)
            .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
            .map { _.fromJson[CardanoMetadataCBOR].getOrElse(???) }
            .via(pipeline)
            .flatMap(e => ZStream.fromIterable(e))
            .tapSink {
              ZSink
                .fromFileName(
                  name = s"${indexerConfig.eventsPath}/${fileName.getName}",
                  options = Set(WRITE, TRUNCATE_EXISTING, CREATE)
                )
                .contramapChunks[MaybeOperation[OP]](_.flatMap { case op => s"${op.toJson}\n".getBytes })
            }
        }
      }.flatten
      streamMetadata = streamAllChunkFiles

      _ <- streamMetadata
        .via(pipelineState)
        .run(ZSink.count)
        .provideEnvironment(ZEnvironment(stateRef))
      _ <- ZIO.log(s"Finish Indexing")
      // ###############################################
      state <- stateRef.get
      _ <- ZIO.log(s"State have ${state.ssi2opId.size} SSI")
      _ <- ZStream
        .fromIterable(state.ssi2opId)
        .mapZIO { case (did, opidSeq) =>
          for {
            _ <- ZIO.logDebug(s"DID: $did")
            ops = state.allOpForDID(did)
            _ <- ZStream.fromIterable(ops).run {
              ZSink
                .fromFileName(name = indexerConfig.opsPath(did), options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[MySignedPrismOperation[OP]](_.flatMap { spo => s"${spo.toJson}\n".getBytes })
            }
            ssi = SSI.make(did, ops)
            _ <- ZStream.from(ssi).run {
              ZSink
                .fromFileName(name = indexerConfig.ssiPath(did), options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[SSI](_.flatMap { case ssi => s"${ssi.toJsonPretty}\n".getBytes })
            }
            _ <- ZStream.from(ssi).run {
              ZSink
                .fromFileName(name = indexerConfig.diddocPath(did), options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[SSI](_.flatMap { case ssi => s"${ssi.didDocument.toJsonPretty}\n".getBytes })
            }
          } yield ()
        }
        .run(ZSink.count)

    } yield ()
  }
}
