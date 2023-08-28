package fmgp.crypto

import typings.jose.mod.importJWK
import typings.jose.mod.jwtDecrypt
import typings.jose.mod.jwtVerify
import typings.jose.mod.GeneralSign
import typings.jose.typesMod.JWK
import typings.jose.typesMod.KeyLike
import typings.jose.typesMod.CompactJWSHeaderParameters

import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.util.Base64

import zio._
import zio.json._

import scala.scalajs.js
import scala.scalajs.js.JavaScriptException
import scala.scalajs.js.JSConverters._
import scala.util.chaining._
import concurrent.ExecutionContext.Implicits.global

object UtilsJS {
  // extension (ec: ECKey) {

  //   // https://datatracker.ietf.org/doc/html/draft-ietf-jose-json-web-key
  //   def toJWKECKey = {
  //     val key = JWK()
  //     key.setKty(ec.kty.toString)
  //     key.setX(ec.x)
  //     key.setY(ec.y)
  //     key.setCrv(ec.getCurve.toString)
  //     // TODO REMOVE
  //     // ec.getCurve match { // See table in https://github.com/panva/jose/issues/210
  //     //   case Curve.`P-256`   => key.setAlg("ES256") // JWKECKey.apply(ECCurveJS.`P-256`, ec.x, ec.y)
  //     //   case Curve.`P-384`   => key.setAlg("ES384") // JWKECKey.apply(ECCurveJS.`P-384`, ec.x, ec.y)
  //     //   case Curve.`P-521`   => key.setAlg("ES512") // JWKECKey.apply(ECCurveJS.`P-521`, ec.x, ec.y)
  //     //   case Curve.secp256k1 => key.setAlg("ES256K") // JWKECKey.apply(ECCurveJS.secp256k1, ec.x, ec.y)
  //     // }
  //     key.setAlg(ec.alg)
  //     ec.kid.foreach(id => key.setKid(id))

  //     ec match {
  //       case _: PublicKey  => // ok
  //       case k: PrivateKey => key.setD(k.d)
  //     }
  //     key
  //   }
  // }

  // extension (okp: OKPKey) {
  //   def toJWKOKPKey = {
  //     val key = JWK()
  //     key.setKty(okp.kty.toString)
  //     key.setX(okp.x)
  //     key.setCrv(okp.getCurve.toString)
  //     // TODO REMOVE
  //     // okp.getCurve match {
  //     //   case Curve.Ed25519 => key.setAlg("EdDSA") // JWKOKPKey.apply(OKPCurveJS.Ed25519, okp.x)
  //     //   case Curve.X25519  => key.setAlg("EdDSA") // FIXME CHECK // JWKOKPKey.apply(OKPCurveJS.X25519, okp.x)
  //     // }
  //     key.setAlg(okp.alg)
  //     okp.kid.foreach(id => key.setKid(id))
  //     okp match {
  //       case _: PublicKey  => // ok
  //       case k: PrivateKey => key.setD(k.d)
  //     }
  //     key
  //   }
  // }

  extension (key: OKP_EC_Key) {

    private def toJWK: JWK = {
      val keyJWK = JWK()
      keyJWK.setKty(key.kty.toString)
      keyJWK.setX(key.x)
      key match {
        case ec: ECKey   => keyJWK.setY(ec.y)
        case okp: OKPKey => // ok
      }

      keyJWK.setCrv(key.crv.toString)
      keyJWK.setAlg(key.alg.toString)
      key.kid.foreach(id => keyJWK.setKid(id))

      key match {
        case _: PublicKey  => // ok
        case k: PrivateKey => keyJWK.setD(k.d)
      }
      keyJWK
    }

    // TODO make private
    def toKeyLike: IO[UnknownError.type, (KeyLike, String)] = {
      val aux = key.toJWK
      ZIO
        .fromPromiseJS(importJWK(aux))
        .map(k => (k.asInstanceOf[KeyLike], aux.alg.get))
        .orElseFail(UnknownError)
    }

    def verify(jwm: SignedMessage): IO[CryptoFailed, Boolean] =
      key.toKeyLike.flatMap(thisKey =>
        ZIO
          .fromPromiseJS(jwtVerify(jwm.base64, thisKey._1)) // .toFuture
          .map(_ => true)
          .catchAll { case JavaScriptException(ex: scala.scalajs.js.TypeError) => ZIO.succeed(false) }
      )
  }

  extension (key: PrivateKey) {
    def sign(payload: Array[Byte]): IO[CryptoFailed, SignedMessage] = {
      val data = js.typedarray.Uint8Array.from(payload.toSeq.map(_.toShort).toJSIterable)

      key.toKeyLike
        .flatMap { (thisKey, alg) =>
          ZIO
            .fromPromiseJS(
              GeneralSign(data) // We can also use CompactSign
                .tap(
                  _.addSignature(thisKey.asInstanceOf[KeyLike])
                    .setProtectedHeader(CompactJWSHeaderParameters(alg))
                )
                .sign()
            )
            // .toFuture
            .map(generalJWS =>
              // TODO REMOVE old .split('.') match { case Array(protectedValue, payload, signature) =>
              SignedMessage(
                payload = Payload.fromBase64url(generalJWS.payload),
                generalJWS.signatures.toSeq
                  .map(v =>
                    JWMSignatureObj(
                      `protected` = Base64(v.`protected`.get).unsafeAsObj[SignProtectedHeader],
                      signature = SignatureJWM(v.signature)
                    )
                  )
              )
            )
            .orElseFail(UnknownError)

        }

    }
  }
}
