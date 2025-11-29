package fmgp.did.method.prism

import zio.*
import zio.http.*

object HttpUtilsSuiteAUX {
  val layer: ZLayer[Any, Nothing, HttpUtils] =
    (Client.default ++ Scope.default >>> HttpUtils.layer).orDie
}
