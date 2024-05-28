package fmgp.crypto

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import scala.util.chaining._

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
