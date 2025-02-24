package fmgp.prism

import scalapb.zio_grpc.{ServerMain, ServiceList}

/** Main PrismNode
  *
  * https://scalapb.github.io/docs/
  *
  * https://scalapb.github.io/zio-grpc/
  */
object PrismNode extends ServerMain {
  def services = ServiceList.add(PrismNodeImpl)

  // Default port is 9000
  override def port: Int = 8980
}
