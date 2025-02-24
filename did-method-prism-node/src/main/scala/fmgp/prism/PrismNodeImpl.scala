package fmgp.prism

import proto.prism.node.ZioPrismNodeApi

object PrismNodeImpl extends ZioPrismNodeApi.NodeService {
//   type Generic[-C, +E] = GNodeService[C, E]
  def healthCheck(
      request: proto.prism.node.HealthCheckRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.HealthCheckResponse] = ???
  def getDidDocument(
      request: proto.prism.node.GetDidDocumentRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetDidDocumentResponse] = ???
  def getNodeBuildInfo(
      request: proto.prism.node.GetNodeBuildInfoRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetNodeBuildInfoResponse] = ???
  def getNodeNetworkProtocolInfo(
      request: proto.prism.node.GetNodeNetworkProtocolInfoRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetNodeNetworkProtocolInfoResponse] = ???
  def getOperationInfo(
      request: proto.prism.node.GetOperationInfoRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetOperationInfoResponse] = ???
  def getLastSyncedBlockTimestamp(
      request: proto.prism.node.GetLastSyncedBlockTimestampRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.GetLastSyncedBlockTimestampResponse] = ???
  def scheduleOperations(
      request: proto.prism.node.ScheduleOperationsRequest
  ): _root_.zio.IO[io.grpc.StatusException, proto.prism.node.ScheduleOperationsResponse] = ???
}
