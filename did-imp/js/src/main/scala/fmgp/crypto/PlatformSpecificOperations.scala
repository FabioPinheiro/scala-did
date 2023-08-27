package fmgp.crypto

import zio._

import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.SignedMessage
import fmgp.crypto.UtilsJS._
import fmgp.crypto.error._

object PlatformSpecificOperations {
  def sign(key: PrivateKey, payload: Array[Byte]): IO[CryptoFailed, SignedMessage] =
    key.sign(payload)

  def verify(key: PublicKey, jwm: SignedMessage): IO[CryptoFailed, Boolean] =
    key.verify(jwm)
}
