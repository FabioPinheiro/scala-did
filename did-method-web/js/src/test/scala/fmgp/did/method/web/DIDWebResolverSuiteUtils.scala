package fmgp.did.method.web

import zio.*
import fmgp.did.Resolver

object DIDWebResolverSuiteUtils {
  val resolverLayer: ULayer[Resolver] =
    DIDWebResolver.layer
}
