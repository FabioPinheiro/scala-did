package fmgp.did.method.prism.node

import zio.*
import proto.prism.ProtocolVersion
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
    didDataEffect = state.ssi2eventsId.get(did).map { events =>
      val ops = events.map(op => state.getEventsByHash(op.eventHash)).flatten
      SSI.make(did, ops).didData
    }
    ret = GetDidDocumentResponse(
      document = didDataEffect,
      lastSyncedBlockTimestamp = Some(state.lastSyncedBlockTimestamp),
      lastUpdateEvent = com.google.protobuf.ByteString.EMPTY // FIXME
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
  } yield GetNodeBuildInfoResponse(
    version = "0.1.0",
    scalaVersion = "scalaVersion", // TODO
    sbtVersion = "sbtVersion", // TODO
    // unknownFields: scalapb.UnknownFieldSet
  )

  def getNodeNetworkProtocolInfo(
      request: GetNodeNetworkProtocolInfoRequest
  ): IO[io.grpc.StatusException, GetNodeNetworkProtocolInfoResponse] = for {
    _ <- ZIO.log("getNodeNetworkProtocolInfo")
  } yield GetNodeNetworkProtocolInfoResponse(
    supportedNetworkProtocolVersion = Some(ProtocolVersion(majorVersion = 1, minorVersion = 0)),
    currentNetworkProtocolVersion = Some(ProtocolVersion(majorVersion = 1, minorVersion = 0)),
    // unknownFields: scalapb.UnknownFieldSet
  )

  case class EventInfo(
      txStatus: String,
      statusDetails: String,
      transactionId: Option[String]
  )

  def getEventInfo(
      request: GetEventInfoRequest
  ): IO[io.grpc.StatusException, GetEventInfoResponse] = for {
    _ <- ZIO.log("getEventInfo")
    state <- refState.get
    // TODO make enum map from cardano java client
    ///    SUBMITTED,
    ///    FAILED,
    ///    PENDING,
    ///    TIMEOUT,
    ///    CONFIRMED
    // lastSyncedBlockTimestamp

    eventHashEffect = ZIO
      .attempt {
        // SHA256.digestToHex(request.eventId.toByteArray) //???? what? is this
        EventHash(request.eventId.toByteArray)
      }
      .mapError(ex =>
        io.grpc.Status.INTERNAL
          .withDescription(s"Error computing event hash: ${ex.getMessage}")
          .asException()
      )

    eventEffect = eventHashEffect.map { eventHash =>
      state.getEventsByHash(eventHash).map { op =>
        assert(op.opHash == eventHash.hex, s"Event hash mismatch: ${op.opHash} != ${eventHash.hex}")

        EventInfo(
          txStatus = "CONFIRMED", // if we find the event in the state, it is confirmed
          statusDetails = s"Event found with hash: ${op.opHash}",
          transactionId = Some(op.tx)
        )
      }
    }

    ret <- eventEffect.map { maybeOpInfo =>
      // Create timestamp for last synced block TODO FIX ME
      val now = java.time.Instant.now
      val timestamp = com.google.protobuf.timestamp.Timestamp(now.getEpochSecond, now.getNano)

      val (status, details, txId) = maybeOpInfo
        .map { opInfo =>
          val status = opInfo.txStatus match {
            case "PENDING" =>
              EventStatus.PENDING_SUBMISSION
            case "FAILED" =>
              EventStatus.PENDING_SUBMISSION
            case "TIMEOUT" =>
              EventStatus.PENDING_SUBMISSION
            case "SUBMITTED" =>
              EventStatus.AWAIT_CONFIRMATION
            case "CONFIRMED" =>
              EventStatus.CONFIRMED_AND_APPLIED
            case _ =>
              EventStatus.UNKNOWN_EVENT
          }
          (status, opInfo.statusDetails, opInfo.transactionId.getOrElse(""))
        }
        .getOrElse((EventStatus.UNKNOWN_EVENT, "Event not found", ""))
      GetEventInfoResponse(
        eventStatus = status,
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

  def scheduleEvents(
      request: ScheduleEventsRequest
  ): IO[io.grpc.StatusException, ScheduleEventsResponse] = for {
    _ <- ZIO.log(s"scheduleEvents with ${request.signedEvents.size} signed events")
    _ <- ZIO.foreach(request.signedEvents.toSeq.zipWithIndex) { case (sp, index) =>
      ZIO.log(s"Event $index to '${sp.toProtoString}'")
    }
  } yield ScheduleEventsResponse(
    outputs = Seq.empty
  )
}
