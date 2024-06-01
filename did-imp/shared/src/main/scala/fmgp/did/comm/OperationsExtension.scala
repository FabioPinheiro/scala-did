package fmgp.did.comm

import zio._
import fmgp.crypto._

/** Hide implementation details to improve the API
  *
  * Note: methods names SHOULD be different from the methods extension in JWTOperations.type
  */
extension (c: Operations.type)
  def layerOperations: ULayer[Operations] =
    ZLayer.succeed(OperationsImp(CryptoOperationsImp))
