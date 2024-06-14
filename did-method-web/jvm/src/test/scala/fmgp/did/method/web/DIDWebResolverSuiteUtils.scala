package fmgp.did.method.web

import zio._
import zio.http._
import fmgp.did.Resolver

object DIDWebResolverSuiteUtils {
  // FIXME https://github.com/zio/zio-http/issues/2280
  val resolverLayer: ZLayer[Any, Nothing, Resolver] =
    (Client.default ++ Scope.default >>> DIDWebResolver.layer).orDie
}
