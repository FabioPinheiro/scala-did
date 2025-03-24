package fmgp.prism

import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio.*

/** Main PrismNode
  *
  * https://scalapb.github.io/docs/
  *
  * https://scalapb.github.io/zio-grpc/
  */
object PrismNode extends ServerMain {

  // Default port is 9000
  override def port: Int = 8980

  override def services = ServiceList.addZIO(ZIO.succeed(PrismNodeImpl(State.empty)))

}
