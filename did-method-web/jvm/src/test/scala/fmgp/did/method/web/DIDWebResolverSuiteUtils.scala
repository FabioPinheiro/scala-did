package fmgp.did.method.web

import zio.*
import zio.http.*
import fmgp.did.Resolver

object DIDWebResolverSuiteUtils {
  // FIXME https://github.com/zio/zio-http/issues/2280
  val resolverLayer: ZLayer[Any, Nothing, Resolver] =
    (Client.default ++ Scope.default >>> DIDWebResolver.layer).orDie
}
