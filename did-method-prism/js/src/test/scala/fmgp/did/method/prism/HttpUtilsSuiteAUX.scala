package fmgp.did.method.prism

import zio.*

object HttpUtilsSuiteAUX {
  val layer: ULayer[HttpUtils] =
    HttpUtils.layer
}
