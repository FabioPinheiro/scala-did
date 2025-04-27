package fmgp.did.method.prism

import zio._

object DIDResolverProxySuiteUtils {
  val layer: ULayer[DIDResolverProxy] =
    DIDResolverProxy.layer
}
