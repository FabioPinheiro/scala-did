package fmgp.prism

import zio._
import scalapb.zio_grpc.{ServerMain, ServiceList}

/** Main PrismNode
  *
  * https://scalapb.github.io/docs/
  *
  * https://scalapb.github.io/zio-grpc/
  */
object PrismNode extends ServerMain {

  // Default port is 9000
  override def port: Int = 8980

  override def services = ServiceList.addZIO(ZIO.succeed(PrismNodeImpl()))

}
