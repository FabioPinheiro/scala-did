package fmgp.crypto

import zio._

import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.OctetKeyPair

import fmgp.did.comm.PlaintextMessage
import fmgp.did.comm.SignedMessage
import fmgp.crypto.UtilsJVM._
import fmgp.crypto.error._

object PlatformSpecificOperations {

  def sign(key: PrivateKey, payload: Array[Byte]): IO[CurveError, SignedMessage] =
    key match {
      case okp: OKPPrivateKey if okp.crv == Curve.Ed25519 =>
        ZIO.succeed(okp.toJWK.signWithEd25519(payload, key.jwaAlgorithmtoSign))
      case okp: OKPPrivateKey =>
        ZIO.fail(UnsupportedCurve(obtained = okp.crv, supported = Set(Curve.Ed25519)))
      case ec: ECPrivateKey =>
        ZIO.succeed(ec.toJWK.sign(payload, key.jwaAlgorithmtoSign))
    }

  def verify(key: PublicKey, jwm: SignedMessage): UIO[Boolean] =
    ZIO.succeed(key.match {
      case okp: OKPPublicKey => okp.toJWK.verify(jwm, key.jwaAlgorithmtoSign)
      case ec: ECPublicKey   => ec.toJWK.verify(jwm, key.jwaAlgorithmtoSign)
    })

}
