package fmgp.crypto

import zio._

import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.SignedMessage
import fmgp.crypto.UtilsJS._
import fmgp.crypto.error._

object PlatformSpecificOperations {

  // ###########
  // ### JWT ###
  // ###########

  def signJWT(key: PrivateKey, payload: Array[Byte], alg: JWAAlgorithm): IO[CryptoFailed, JWT] =
    key.signJWT(payload = payload, alg = alg)

  def verifyJWT(key: PublicKey, jwt: JWT): IO[CryptoFailed, Boolean] =
    key.verifyJWT(jwt)

  // #####################
  // ### SignedMessage ###
  // #####################

  def sign(
      key: PrivateKey,
      payload: Array[Byte],
      // alg: JWAAlgorithm
  ): IO[CryptoFailed, SignedMessage] =
    key.sign(payload)

  def verify(key: PublicKey, jwm: SignedMessage): IO[CryptoFailed, Boolean] =
    key.verify(jwm)
}
