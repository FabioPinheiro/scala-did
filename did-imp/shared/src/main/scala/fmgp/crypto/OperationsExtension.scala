package fmgp.crypto

import zio._

/** Hide implementation details to improve the API
  *
  * Note: methods names SHOULD be different from the methods extension in Operations.type
  */
extension (c: JWTOperations.type)
  def layerJWTOperations: ULayer[JWTOperations] =
    ZLayer.succeed(JWTOperationsImp(CryptoOperationsImp))
