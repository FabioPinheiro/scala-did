package fmgp.did.method.prism

import zio._
import zio.http._

object HttpUtilsSuiteAUX {
  val layer: ZLayer[Any, Nothing, HttpUtils] =
    (Client.default ++ Scope.default >>> HttpUtils.layer).orDie
}
