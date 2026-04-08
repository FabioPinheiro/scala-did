package fmgp.crypto

import zio.*

import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.SignedMessage
import fmgp.crypto.UtilsJS.*
import fmgp.crypto.error.*

object PlatformSpecificOperations {

  // #################
  // ### RAW Bytes ###
  // #################

  def signBytes(key: PrivateKey, payload: Array[Byte]): IO[CryptoFailed, Array[Byte]] =
    ???

  def verifyBytes(key: PublicKey, payload: Array[Byte], signature: Array[Byte]): IO[CryptoFailed, Boolean] =
    ???

  // ###########
  // ### JWT ###
  // ###########

  def signJWT(
      key: PrivateKey,
      payload: Array[Byte],
      // alg: JWAAlgorithm
  ): IO[CryptoFailed, JWT] =
    key.signJWT(payload = payload)

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
