package fmgp.did.method.prism

import zio._

object HttpUtilsSuiteAUX {
  val layer: ULayer[HttpUtils] =
    HttpUtils.layer
}
