package fmgp.prism

import java.nio.file.StandardOpenOption.{WRITE, APPEND, CREATE, TRUNCATE_EXISTING}

import zio._
import zio.stream._
import zio.json._
import zio.http._

import fmgp.blockfrost.{API, MetadataContentJson}

object Indexer extends ZIOAppDefault {

  val PRISM_LABEL_CIP_10 = "21325"
  val apiKey = "FIXME"
  val PAGE_SIZE = 100
  val WORKDIR_PATH = s"../prism-vdr/mainnet" // "did-method-prism/db_prism"
  val METADATA_FILENAME = s"$WORKDIR_PATH/cardano-21325"
  val STATE_FILENAME = s"$WORKDIR_PATH/prism-state.json"

  def metadataFromAPI = ZStream
    .paginateChunkZIO(0) { pageNumber =>
      for {
        _ <- ZIO.log(s"pageNumber=$pageNumber; (entries ${pageNumber * PAGE_SIZE}-${(pageNumber + 1) * PAGE_SIZE})")
        response <- Client
          .batched(
            Request
              .get(path = API.metadataContentJson(PRISM_LABEL_CIP_10, page = pageNumber + 1, count = PAGE_SIZE))
              .addHeaders(Headers(Header.Custom("project_id", apiKey)))
          )
        responseStr <- response.body.asString
        page <- responseStr.fromJson[Seq[MetadataContentJson]] match
          case Left(error) => ZIO.logError(responseStr) *> ZIO.fail(new RuntimeException("fail to parse"))
          case Right(value) =>
            ZIO.succeed(
              value.zipWithIndex.map((metadataContent, index) =>
                CardanoMetadata(pageNumber * PAGE_SIZE + index, metadataContent.tx_hash, metadataContent.json_metadata)
              )
            )
      } yield (Chunk.fromIterable(page), if (page.size == PAGE_SIZE) Some(pageNumber + 1) else None)
    }
    .provideLayer(Client.default ++ Scope.default)

  def metadataFromFile(fileName: String = METADATA_FILENAME) =
    ZStream
      .fromFileName(name = fileName)
      .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
      .map { _.fromJson[CardanoMetadata].getOrElse(???) }

  def sinkMetadataIntoFile(fileName: String = METADATA_FILENAME) = ZSink
    .fromFileName(name = fileName, options = Set(WRITE, APPEND, CREATE))
    .contramapChunks[CardanoMetadata](_.flatMap { cm => (cm.toJson + "\n").getBytes })

  // def sinkStateIntoFile(fileName: String = STATE_FILENAME) = ZSink
  //   .fromFileName(name = fileName, options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
  //   .contramapChunks[State](_.flatMap { _.toJsonPretty.getBytes })

  def pipeline = ZPipeline.map[CardanoMetadata, Seq[MaybeOperation[OP]]](CardanoMetadata =>
    CardanoMetadata.toCardanoPrismEntry match
      case Left(error) =>
        Seq(
          InvalidPrismObject(
            tx = CardanoMetadata.tx,
            b = CardanoMetadata.b,
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

  override val run = {
    for {
      _ <- Console.printLine(
        """██████╗ ██████╗ ██╗███████╗███╗   ███╗    ██╗   ██╗██████╗ ██████╗ 
          |██╔══██╗██╔══██╗██║██╔════╝████╗ ████║    ██║   ██║██╔══██╗██╔══██╗
          |██████╔╝██████╔╝██║███████╗██╔████╔██║    ██║   ██║██████╔╝██║  ██║
          |██╔═══╝ ██╔══██╗██║╚════██║██║╚██╔╝██║    ╚██╗ ██╔╝██╔══██╗██║  ██║
          |██║     ██║  ██║██║███████║██║ ╚═╝ ██║     ╚████╔╝ ██║  ██║██████╔╝
          |╚═╝     ╚═╝  ╚═╝╚═╝╚══════╝╚═╝     ╚═╝      ╚═══╝  ╚═╝  ╚═╝╚═════╝ 
          |                                                                   
          |       ██╗███╗   ██╗██████╗ ███████╗██╗  ██╗███████╗██████╗        
          |       ██║████╗  ██║██╔══██╗██╔════╝╚██╗██╔╝██╔════╝██╔══██╗       
          |       ██║██╔██╗ ██║██║  ██║█████╗   ╚███╔╝ █████╗  ██████╔╝       
          |       ██║██║╚██╗██║██║  ██║██╔══╝   ██╔██╗ ██╔══╝  ██╔══██╗       
          |       ██║██║ ╚████║██████╔╝███████╗██╔╝ ██╗███████╗██║  ██║       
          |       ╚═╝╚═╝  ╚═══╝╚═════╝ ╚══════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝       
          |PRISM - Verifiable Data Registry (VRD) Indexer
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

      streamAPI = metadataFromAPI
      _ <- streamAPI.run(sinkMetadataIntoFile())
      streamMetadata = metadataFromFile()
      _ <- streamMetadata
        .via(pipeline)
        .flatMap(e => ZStream.fromIterable(e))
        .tapSink(
          ZSink
            .fromFileName(name = s"$WORKDIR_PATH/prism-operations", options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
            .contramapChunks[MaybeOperation[OP]](_.flatMap { case op => s"${op.toJson}\n".getBytes })
        )
        .via(pipelineState)
        .run(ZSink.count) // .run(sinkLog)
        .provideEnvironment(ZEnvironment(stateRef))
      _ <- ZIO.log(s"Finish Indexing")
      // ###############################################
      state <- stateRef.get
      _ <- ZIO.log(s"State have ${state.ssi2opId.size} SSI")
      _ <- ZStream
        .fromIterable(state.ssi2opId)
        .mapZIO { case (did, opidSeq) =>
          for {
            _ <- ZStream.fromIterable(opidSeq).run {
              ZSink
                .fromFileName(name = s"$WORKDIR_PATH/opid/$did", options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[OpId](_.flatMap { case opid => s"${opid.toJson}\n".getBytes })
            }
            ops = state.allOpForDID(did)
            _ <- ZStream.fromIterable(ops).run {
              ZSink
                .fromFileName(name = s"$WORKDIR_PATH/ops/$did", options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[MySignedPrismOperation[OP]](_.flatMap { spo => s"${spo.toJson}\n".getBytes })
            }
            ssi = SSI.make(did, ops)
            _ <- ZStream.from(ssi).run {
              ZSink
                .fromFileName(name = s"$WORKDIR_PATH/ssi/$did", options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[SSI](_.flatMap { case ssi => s"${ssi.toJsonPretty}\n".getBytes })
            }
            _ <- ZStream.from(ssi).run {
              ZSink
                .fromFileName(name = s"$WORKDIR_PATH/diddoc/$did", options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
                .contramapChunks[SSI](_.flatMap { case ssi => s"${ssi.didDocument.toJsonPretty}\n".getBytes })
            }
          } yield ()
        }
        .run(ZSink.count)

    } yield ()
  }
}
