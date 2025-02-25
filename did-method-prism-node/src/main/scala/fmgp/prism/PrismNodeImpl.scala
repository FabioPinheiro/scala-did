package fmgp.prism

import zio._
import proto.prism.node.ZioPrismNodeApi
import proto.prism.node.HealthCheckResponse

case class PrismNodeImpl() extends ZioPrismNodeApi.NodeService {
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
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetDidDocumentResponse] =
    println("getDidDocument")
    ???

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
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetOperationInfoResponse] =
    println("getOperationInfo")
    ???

  def getLastSyncedBlockTimestamp(
      request: proto.prism.node.GetLastSyncedBlockTimestampRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetLastSyncedBlockTimestampResponse] =
    println("getLastSyncedBlockTimestamp")
    ???

  def scheduleOperations(
      request: proto.prism.node.ScheduleOperationsRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.ScheduleOperationsResponse] =
    println("scheduleOperations")
    ???

}
