package fmgp.crypto

import zio._

import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.OctetKeyPair

import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.SignedMessage
import fmgp.crypto.UtilsJVM._
import fmgp.crypto.error._

object PlatformSpecificOperations {

  // ###########
  // ### JWT ###
  // ###########

  def signJWT(
      key: PrivateKey,
      payload: Array[Byte],
      alg: JWAAlgorithm
  ): IO[CryptoFailed, JWT] = key match {
    case okp: OKPPrivateKey => ZIO.succeed(UtilsJVM.okpKeySignJWTWithEd25519(okpKey2JWK(okp), payload, alg))
    case ec: ECPrivateKey   => ZIO.succeed(UtilsJVM.ecKeySignJWT(ec.toJWK, payload, alg))
  }

  def verifyJWT(
      key: PublicKey,
      jwt: JWT
  ): IO[CryptoFailed, Boolean] = key match {
    case okp: OKPPublicKey => ZIO.succeed(UtilsJVM.okpKeyVerifyJWTWithEd25519(okpKey2JWK(okp), jwt))
    case ec: ECPublicKey   => ZIO.succeed(UtilsJVM.ecKeyVerifyJWT(ec.toJWK, jwt))
  }

  // #####################
  // ### SignedMessage ###
  // #####################

  def sign(
      key: PrivateKey,
      payload: Array[Byte],
      // alg: JWAAlgorithm
  ): IO[CurveError, SignedMessage] =
    key match {
      case okp @ OKPPrivateKey(kty, Curve.Ed25519, d, x, kid) =>
        ZIO.succeed(okpKeySignJWMWithEd25519(okpKey2JWK(okp), payload, key.jwaAlgorithmtoSign))
      case okp @ OKPPrivateKey(kty, crv, d, x, kid) =>
        ZIO.fail(UnsupportedCurve(obtained = crv, supported = Set(Curve.Ed25519)))
      case ec: ECPrivateKey =>
        ZIO.succeed(ecKeySignJWM(ec.toJWK, payload, key.jwaAlgorithmtoSign))
    }

  def verify(key: PublicKey, jwm: SignedMessage): IO[CurveError, Boolean] =
    key.match {
      case okp @ OKPPublicKey(kty, Curve.Ed25519, x, kid) =>
        ZIO.succeed(okpKeyVerifyJWMWithEd25519(okpKey2JWK(okp), jwm, key.jwaAlgorithmtoSign))
      case okp @ OKPPublicKey(kty, crv, x, kid) =>
        ZIO.fail(UnsupportedCurve(obtained = crv, supported = Set(Curve.Ed25519)))
      case ec: ECPublicKey =>
        ZIO.succeed(ecKeyVerifyJWM(ec.toJWK, jwm, key.jwaAlgorithmtoSign))
    }

}
