package fmgp.crypto

import zio.*
import zio.json.*
import scala.util.Try

import com.nimbusds.jose.jwk

import fmgp.crypto.error.*

object KeyGenerator {
  def makeX25519: IO[FailToGenerateKey, OKPPrivateKey] =
    ZIO
      .attempt(new jwk.gen.OctetKeyPairGenerator(jwk.Curve.X25519).generate.toJSONString())
      .mapError(ex => FailToGenerateKey(SomeThrowable(ex)))
      .flatMap(_.fromJson[OKPPrivateKey] match
        case Left(strError) => ZIO.fail(FailToGenerateKey(FailToParse(strError)))
        case Right(value)   => ZIO.succeed(value))

  def makeEd25519: IO[FailToGenerateKey, OKPPrivateKey] =
    ZIO
      .attempt(new jwk.gen.OctetKeyPairGenerator(jwk.Curve.Ed25519).generate.toJSONString())
      .mapError(ex => FailToGenerateKey(SomeThrowable(ex)))
      .flatMap(_.fromJson[OKPPrivateKey] match
        case Left(strError) => ZIO.fail(FailToGenerateKey(FailToParse(strError)))
        case Right(value)   => ZIO.succeed(value))

  @deprecated("Use makeX25519 instead", "0.1.0-M25")
  def newX25519: Either[FailToGenerateKey, OKPPrivateKey] = Try {
    new jwk.gen.OctetKeyPairGenerator(jwk.Curve.X25519).generate.toJSONString()
  }.toEither.left
    .map { case e => FailToGenerateKey(SomeThrowable(e)) }
    .flatMap {
      _.fromJson[OKPPrivateKey].left
        .map(strError => FailToGenerateKey(FailToParse(strError)))
    }

  @deprecated("Use makeEd25519 instead", "0.1.0-M25")
  def newEd25519: Either[FailToGenerateKey, OKPPrivateKey] = Try {
    new jwk.gen.OctetKeyPairGenerator(jwk.Curve.Ed25519).generate.toJSONString()
  }.toEither.left
    .map { case e => FailToGenerateKey(SomeThrowable(e)) }
    .flatMap {
      _.fromJson[OKPPrivateKey].left
        .map(strError => FailToGenerateKey(FailToParse(strError)))
    }

  def unsafeX25519: OKPPrivateKey = {
    val tmp = new jwk.gen.OctetKeyPairGenerator(jwk.Curve.X25519).generate()
    OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.X25519,
      d = tmp.getD().toString(),
      x = tmp.getX().toString(),
    )
  }

  def unsafeEd25519: OKPPrivateKey = {
    val tmp = new jwk.gen.OctetKeyPairGenerator(jwk.Curve.Ed25519).generate()
    OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = tmp.getD().toString(),
      x = tmp.getX().toString(),
    )
  }

}
