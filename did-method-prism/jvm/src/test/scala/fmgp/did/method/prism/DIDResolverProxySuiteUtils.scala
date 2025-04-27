package fmgp.did.method.prism

import zio._
import zio.http._

object DIDResolverProxySuiteUtils {
  val layer: ZLayer[Any, Nothing, DIDResolverProxy] =
    (Client.default ++ Scope.default >>> DIDResolverProxy.layer).orDie
}
