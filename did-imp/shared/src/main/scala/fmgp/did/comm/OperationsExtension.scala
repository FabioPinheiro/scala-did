package fmgp.did.comm

import zio.*
import fmgp.crypto.*

/** Hide implementation details to improve the API
  *
  * Note: methods names SHOULD be different from the methods extension in JWTOperations.type
  */
extension (c: Operations.type)
  def layerOperations: ULayer[Operations] =
    ZLayer.succeed(OperationsImp(CryptoOperationsImp))
