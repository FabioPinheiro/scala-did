package fmgp.crypto

import zio._
import zio.json._
import scala.util.Try

import com.nimbusds.jose.jwk

import fmgp.crypto.error._

object KeyGenerator {
  def newX25519: Either[FailToGenerateKey, OKPPrivateKey] = Try {
    new jwk.gen.OctetKeyPairGenerator(jwk.Curve.X25519).generate.toJSONString()
  }.toEither.left
    .map { case e => FailToGenerateKey(SomeThrowable(e)) }
    .flatMap {
      _.fromJson[OKPPrivateKey].left
        .map(strError => FailToGenerateKey(FailToParse(strError)))
    }

  def newEd25519: Either[FailToGenerateKey, OKPPrivateKey] = Try {
    new jwk.gen.OctetKeyPairGenerator(jwk.Curve.Ed25519).generate.toJSONString()
  }.toEither.left
    .map { case e => FailToGenerateKey(SomeThrowable(e)) }
    .flatMap {
      _.fromJson[OKPPrivateKey].left
        .map(strError => FailToGenerateKey(FailToParse(strError)))
    }

  def makeX25519: IO[FailToGenerateKey, OKPPrivateKey] = ZIO.fromEither(newX25519)
  def makeEd25519: IO[FailToGenerateKey, OKPPrivateKey] = ZIO.fromEither(newEd25519)

  def unsafeX25519: OKPPrivateKey = {
    val tmp = new jwk.gen.OctetKeyPairGenerator(jwk.Curve.X25519).generate()
    OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.X25519,
      d = tmp.getD().toString(),
      x = tmp.getX().toString(),
      kid = None,
    )
  }

  def unsafeEd25519: OKPPrivateKey = {
    val tmp = new jwk.gen.OctetKeyPairGenerator(jwk.Curve.Ed25519).generate()
    OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = tmp.getD().toString(),
      x = tmp.getX().toString(),
      kid = None,
    )
  }

}
