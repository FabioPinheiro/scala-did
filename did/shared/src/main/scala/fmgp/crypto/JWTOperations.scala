package fmgp.crypto

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._

trait JWTOperations {

  def signJWT(key: PrivateKey, payload: Array[Byte], alg: JWAAlgorithm): IO[CryptoFailed, JWT]

  def verifyJWT(key: PublicKey, jwt: JWT): IO[CryptoFailed, Boolean]

}
