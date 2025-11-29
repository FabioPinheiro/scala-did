package fmgp.crypto

import zio.*
import zio.json.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.crypto.error.*
import scala.util.chaining.*

trait JWTOperations {

  def signJWT(payload: JWTPayload): ZIO[Agent & Resolver, CryptoFailed, JWT]

  def verifyJWT(jwt: JWT): ZIO[Resolver, CryptoFailed, Boolean]

}

object JWTOperations {

  def signJWT(
      payload: JWTPayload
  ): ZIO[JWTOperations & Agent & Resolver, CryptoFailed, JWT] =
    ZIO.serviceWithZIO[JWTOperations](_.signJWT(payload))

  def verifyJWT(
      jwt: JWT
  ): ZIO[JWTOperations & Resolver, CryptoFailed, Boolean] =
    ZIO.serviceWithZIO[JWTOperations](_.verifyJWT(jwt))

}
