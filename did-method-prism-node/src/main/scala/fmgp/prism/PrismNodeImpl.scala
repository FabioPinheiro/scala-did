package fmgp.prism

import fmgp.crypto.SHA256
import fmgp.prism.SSIExt.*
import fmgp.util.Base64
import proto.prism.PublicKey
import proto.prism.Service
import proto.prism.node.DIDData
import proto.prism.node.GetDidDocumentResponse
import proto.prism.node.HealthCheckResponse
import proto.prism.node.ZioPrismNodeApi
import zio.*
case class PrismNodeImpl(state: State) extends ZioPrismNodeApi.NodeService {
//   type Generic[-C, +E] = GNodeService[C, E]
  def healthCheck(
      request: proto.prism.node.HealthCheckRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.HealthCheckResponse] =
    println("healthCheck")
    ZIO.succeed(
      new HealthCheckResponse()
    )
  def getDidDocument(
      request: proto.prism.node.GetDidDocumentRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetDidDocumentResponse] = {
    println("getDidDocument")

    val didDataEffect = state.ssi2opId.get(request.did).map { operations =>
      val ops = operations.map(op => state.opHash2op.get(op.opHash)).flatten
      SSI.make(request.did, ops).didData
    }

    didDataEffect match {
      case Some(didData) =>
        // TODO: Create timestamp for last synced block
        val now = java.time.Instant.now
        val timestamp = com.google.protobuf.timestamp.Timestamp(now.getEpochSecond, now.getNano)

        ZIO.succeed(
          GetDidDocumentResponse(
            document = Some(didData),
            lastSyncedBlockTimestamp = Some(timestamp),
            lastUpdateOperation = com.google.protobuf.ByteString.EMPTY
          )
        )
      case None =>
        ZIO.fail(
          io.grpc.Status.NOT_FOUND
            .withDescription(s"Did document not found for did: ${request.did}")
            .asException()
        )
    }
  }

  def getNodeBuildInfo(
      request: proto.prism.node.GetNodeBuildInfoRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetNodeBuildInfoResponse] =
    println("getDidDocument")
    ???

  def getNodeNetworkProtocolInfo(
      request: proto.prism.node.GetNodeNetworkProtocolInfoRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetNodeNetworkProtocolInfoResponse] =
    println("getNodeNetworkProtocolInfo")
    ???

  def getOperationInfo(
      request: proto.prism.node.GetOperationInfoRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetOperationInfoResponse] = {
    println("getOperationInfo")

    // TODO make enum map from cardano java client
    ///    SUBMITTED,
    ///    FAILED,
    ///    PENDING,
    ///    TIMEOUT,
    ///    CONFIRMED
    // lastSyncedBlockTimestamp
    case class OperationInfo(
        txStatus: String,
        statusDetails: String,
        transactionId: Option[String]
    )

    val operationHashEffect = ZIO
      .attempt {
        SHA256.digestToHex(request.operationId.toByteArray)
      }
      .mapError(ex =>
        io.grpc.Status.INTERNAL
          .withDescription(s"Error computing operation hash: ${ex.getMessage}")
          .asException()
      )

    val operationEffect = operationHashEffect.map { opHash =>
      state.opHash2op.get(opHash).map { op =>
        assert(op.opHash == opHash, s"Operation hash mismatch: ${op.opHash} != $opHash")

        OperationInfo(
          txStatus = "CONFIRMED", // if we find the operation in the state, it is confirmed
          statusDetails = s"Operation found with hash: ${op.opHash}",
          transactionId = Some(op.tx)
        )
      }
    }

    operationEffect.map { maybeOpInfo =>
      // Create timestamp for last synced block TODO FIX ME
      val now = java.time.Instant.now
      val timestamp = com.google.protobuf.timestamp.Timestamp(now.getEpochSecond, now.getNano)

      val (status, details, txId) = maybeOpInfo
        .map { opInfo =>
          val status = opInfo.txStatus match {
            case "PENDING" =>
              proto.prism.node.OperationStatus.PENDING_SUBMISSION
            case "FAILED" =>
              proto.prism.node.OperationStatus.PENDING_SUBMISSION
            case "TIMEOUT" =>
              proto.prism.node.OperationStatus.PENDING_SUBMISSION
            case "SUBMITTED" =>
              proto.prism.node.OperationStatus.AWAIT_CONFIRMATION
            case "CONFIRMED" =>
              proto.prism.node.OperationStatus.CONFIRMED_AND_APPLIED
            case _ =>
              proto.prism.node.OperationStatus.UNKNOWN_OPERATION
          }

          (status, opInfo.statusDetails, opInfo.transactionId.getOrElse(""))
        }
        .getOrElse(
          (proto.prism.node.OperationStatus.UNKNOWN_OPERATION, "Operation not found", "")
        )
      proto.prism.node.GetOperationInfoResponse(
        operationStatus = status,
        transactionId = txId,
        lastSyncedBlockTimestamp = Some(timestamp),
        details = details
      )
    }
  }

  def getLastSyncedBlockTimestamp(
      request: proto.prism.node.GetLastSyncedBlockTimestampRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetLastSyncedBlockTimestampResponse] =
    println("getLastSyncedBlockTimestamp")
    // TODO: Create timestamp for last synced block TODO FIX ME
    val now = java.time.Instant.now
    val timestamp = com.google.protobuf.timestamp.Timestamp(now.getEpochSecond, now.getNano)
    ZIO.succeed(
      proto.prism.node.GetLastSyncedBlockTimestampResponse(
        lastSyncedBlockTimestamp = Some(timestamp)
      )
    )

  def scheduleOperations(
      request: proto.prism.node.ScheduleOperationsRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.ScheduleOperationsResponse] =
    println("scheduleOperations")
    ???

}
