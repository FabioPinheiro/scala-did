package fmgp.did.method.prism.node

import zio.*
import proto.prism.node.*
import fmgp.crypto.SHA256
import fmgp.util.Base64
import fmgp.did.DIDSubject
import fmgp.did.method.prism._
import fmgp.did.method.prism.cardano._

object PrismNodeImpl {
  def make = for {
    state <- ZIO.service[Ref[PrismState]]
    ssiCount <- state.get.map(_.ssiCount)
    _ <- ZIO.log(s"Init PrismNodeImpl Service with PrismState (with $ssiCount SSI)")
    walletConfig = CardanoWalletConfig()
    node = PrismNodeImpl(state, walletConfig)
  } yield node
}

case class PrismNodeImpl(refState: Ref[PrismState], walletConfig: CardanoWalletConfig)
    extends ZioPrismNodeApi.NodeService {

  def healthCheck(
      request: HealthCheckRequest
  ): IO[io.grpc.StatusException, HealthCheckResponse] = for {
    _ <- ZIO.log("healthCheck")
    ret = new HealthCheckResponse()
  } yield ret

  def getDidDocument(
      request: GetDidDocumentRequest
  ): IO[io.grpc.StatusException, GetDidDocumentResponse] = for {
    _ <- ZIO.log("getDidDocument")
    did <- ZIO
      .fromEither(DIDSubject.either(request.did))
      .mapError(exFailToParse => // StatusException
        io.grpc.Status.NOT_FOUND
          .withDescription(s"Fail to parse '${request.did}': ${exFailToParse.error}")
          .asException()
      )
    state <- refState.get
    didDataEffect = state.ssi2eventsId.get(did).map { operations =>
      val ops = operations.map(op => state.getEventsByHash(op.eventHash)).flatten
      SSI.make(did, ops).didData
    }
    ret = GetDidDocumentResponse(
      document = didDataEffect,
      lastSyncedBlockTimestamp = Some(state.lastSyncedBlockTimestamp),
      lastUpdateOperation = com.google.protobuf.ByteString.EMPTY // FIXME
    )

    // ZIO.fail(
    //   io.grpc.Status.NOT_FOUND
    //     .withDescription(s"Did document not found for did: ${request.did}")
    //     .asException()
    // )

  } yield ret

  def getNodeBuildInfo(
      request: GetNodeBuildInfoRequest
  ): IO[io.grpc.StatusException, GetNodeBuildInfoResponse] = for {
    _ <- ZIO.log("getDidDocument")
  } yield ???

  def getNodeNetworkProtocolInfo(
      request: GetNodeNetworkProtocolInfoRequest
  ): IO[io.grpc.StatusException, GetNodeNetworkProtocolInfoResponse] = for {
    _ <- ZIO.log("getNodeNetworkProtocolInfo")
  } yield ???

  case class OperationInfo(
      txStatus: String,
      statusDetails: String,
      transactionId: Option[String]
  )

  def getOperationInfo(
      request: GetOperationInfoRequest
  ): IO[io.grpc.StatusException, GetOperationInfoResponse] = for {
    _ <- ZIO.log("getOperationInfo")
    state <- refState.get
    // TODO make enum map from cardano java client
    ///    SUBMITTED,
    ///    FAILED,
    ///    PENDING,
    ///    TIMEOUT,
    ///    CONFIRMED
    // lastSyncedBlockTimestamp

    operationHashEffect = ZIO
      .attempt {
        SHA256.digestToHex(request.operationId.toByteArray)
      }
      .mapError(ex =>
        io.grpc.Status.INTERNAL
          .withDescription(s"Error computing operation hash: ${ex.getMessage}")
          .asException()
      )

    operationEffect = operationHashEffect.map { opHash =>
      state.getEventsByHash(opHash).map { op =>
        assert(op.opHash == opHash, s"Operation hash mismatch: ${op.opHash} != $opHash")

        OperationInfo(
          txStatus = "CONFIRMED", // if we find the operation in the state, it is confirmed
          statusDetails = s"Operation found with hash: ${op.opHash}",
          transactionId = Some(op.tx)
        )
      }
    }

    ret <- operationEffect.map { maybeOpInfo =>
      // Create timestamp for last synced block TODO FIX ME
      val now = java.time.Instant.now
      val timestamp = com.google.protobuf.timestamp.Timestamp(now.getEpochSecond, now.getNano)

      val (status, details, txId) = maybeOpInfo
        .map { opInfo =>
          val status = opInfo.txStatus match {
            case "PENDING" =>
              OperationStatus.PENDING_SUBMISSION
            case "FAILED" =>
              OperationStatus.PENDING_SUBMISSION
            case "TIMEOUT" =>
              OperationStatus.PENDING_SUBMISSION
            case "SUBMITTED" =>
              OperationStatus.AWAIT_CONFIRMATION
            case "CONFIRMED" =>
              OperationStatus.CONFIRMED_AND_APPLIED
            case _ =>
              OperationStatus.UNKNOWN_OPERATION
          }
          (status, opInfo.statusDetails, opInfo.transactionId.getOrElse(""))
        }
        .getOrElse((OperationStatus.UNKNOWN_OPERATION, "Operation not found", ""))
      GetOperationInfoResponse(
        operationStatus = status,
        transactionId = txId,
        lastSyncedBlockTimestamp = Some(timestamp),
        details = details
      )
    }
  } yield ret

  def getLastSyncedBlockTimestamp(
      request: GetLastSyncedBlockTimestampRequest
  ): IO[io.grpc.StatusException, GetLastSyncedBlockTimestampResponse] = for {
    _ <- ZIO.log("getLastSyncedBlockTimestamp")
    // TODO: Create timestamp for last synced block TODO FIX ME
    now = java.time.Instant.now
    timestamp = com.google.protobuf.timestamp.Timestamp(now.getEpochSecond, now.getNano)
    ret = GetLastSyncedBlockTimestampResponse(
      lastSyncedBlockTimestamp = Some(timestamp)
    )

  } yield ret

  def scheduleOperations(
      request: ScheduleOperationsRequest
  ): IO[io.grpc.StatusException, ScheduleOperationsResponse] = for {
    _ <- ZIO.log(s"scheduleOperations with ${request.signedOperations.size} signed operations")
    _ <- ZIO.foreach(request.signedOperations.toSeq.zipWithIndex) { case (sp, index) =>
      ZIO.log(s"Operation $index to '${sp.toProtoString}'")
    }
  } yield ScheduleOperationsResponse(
    outputs = Seq.empty
  )
}
