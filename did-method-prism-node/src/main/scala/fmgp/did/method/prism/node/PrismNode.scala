package fmgp.did.method.prism.node

import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio.*
import zio.json._
import zio.stream._
import java.nio.file.StandardOpenOption.*
import fmgp.did.method.prism.vdr._

/** Main PrismNode
  *
  * https://scalapb.github.io/docs/
  *
  * https://scalapb.github.io/zio-grpc/
  *
  * didPrismNode/runMain fmgp.prism.PrismNode
  */
object PrismNode extends ServerMain {

  override def port: Int = 8980 // Default port is 9000

  override def services = ServiceList
    .addZIO(ZIO.log("Start PrismNode") *> PrismNodeImpl.make)
    .provide(
      // ZLayer.fromZIO(Ref.make(PrismState.empty))
      ZLayer.succeed(IndexerConfig(mBlockfrostConfig = None, workdir = "../../prism-vdr/mainnet")) >>>
        ZLayer.fromZIO(IndexerUtils.loadPrismStateFromChunkFiles)
    )

}
