package fmgp.prism

import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio.*

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
    .addZIO(PrismNodeImpl.make)
    .provide(
      ZLayer.fromZIO(
        Ref.make(PrismState.empty)
      )
    )

}
