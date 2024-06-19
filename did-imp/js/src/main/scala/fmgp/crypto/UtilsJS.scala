package fmgp.crypto

import fmgp.typings.jose.mod.importJWK
// import fmgp.typings.jose.mod.jwtDecrypt
import fmgp.typings.jose.mod.jwtVerify
import fmgp.typings.jose.mod.generalVerify
import fmgp.typings.jose.mod.SignJWT
import fmgp.typings.jose.mod.GeneralSign
import fmgp.typings.jose.typesMod.JWK
import fmgp.typings.jose.typesMod.KeyLike
import fmgp.typings.jose.typesMod.CompactJWSHeaderParameters
import fmgp.typings.jose.typesMod.JWTHeaderParameters
import fmgp.typings.jose.typesMod.JWTPayload
import fmgp.typings.jose.typesMod.GeneralJWSInput
import fmgp.typings.jose.mod.errors.JWSSignatureVerificationFailed

import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.util.Base64

import zio._
import zio.json._

import scala.scalajs.js
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
      key.maybeKid.foreach(id => keyJWK.setKid(id)) // TODO make it Type Safe

      key match {
        case _: PublicKey  => // ok
        case k: PrivateKey => keyJWK.setD(k.d)
      }
      keyJWK
    }

    // TODO make private
    def toKeyLike: IO[CryptoFailed, (KeyLike, String, String)] = {
      key match
        case keyWithKid: WithKid =>
          val aux = key.toJWK
          ZIO
            .fromPromiseJS(importJWK(aux))
            .map(k => (k.asInstanceOf[KeyLike], aux.alg.get, keyWithKid.kid))
            .orElseFail(UnknownError)
        case keyWithoutKid =>
          ZIO.fail(FailToExtractKid(s"Fail to extract kid from ${key.toJson}"))
    }

    def verify(jwm: SignedMessage): IO[CryptoFailed, Boolean] =
      for {
        jwmAux <- ZIO
          .from(js.JSON.parse(jwm.toJson).asInstanceOf[GeneralJWSInput])
          .mapError(_ match {
            // case ex @ js.JavaScriptException(error: js.Error) =>
            case js.JavaScriptException(error: js.SyntaxError) =>
              CryptoFailToParse(s"JWT payload MUST be JSON: ${error.message}")
            case ex: Throwable => SomeThrowable(ex.getMessage())
          })
        ret <- key.toKeyLike.flatMap(thisKey =>
          ZIO
            .fromPromiseJS { generalVerify(jwmAux, thisKey._1) }
            .map(_ => true)
            .catchAll { case js.JavaScriptException(ex: scala.scalajs.js.TypeError) =>
              ZIO.succeed(false)
            // case js.JavaScriptException(ex: JWSSignatureVerificationFailed) => ZIO.succeed(false)
            }
        )
      } yield ret

    def verifyJWT(jwt: JWT): IO[CryptoFailed, Boolean] =
      for {
        ret <- key.toKeyLike.flatMap(thisKey =>
          ZIO
            .fromPromiseJS(
              jwtVerify(
                jwt.base64JWTFormat,
                thisKey._1,
                // options: JWTVerifyOptions
              )
            )
            .map(_ => true)
            .catchAll {
              case js.JavaScriptException(ex: scala.scalajs.js.TypeError)     => ZIO.succeed(false)
              case js.JavaScriptException(ex: JWSSignatureVerificationFailed) => ZIO.succeed(false)
            }
        )
      } yield ret
  }

  extension (key: PrivateKey) {
    def sign(payload: Array[Byte]): IO[CryptoFailed, SignedMessage] =
      for {
        // _ <- key match {
        //   case ECPublicKey(_, _, _, _, _) | ECPrivateKey(_, _, _, _, _, _) =>
        //     alg match
        //       case JWAAlgorithm.ES256K => ZIO.fail(WrongAlgorithmForKey)
        //       case JWAAlgorithm.ES256  => ZIO.fail(WrongAlgorithmForKey)
        //       case JWAAlgorithm.ES384  => ZIO.fail(WrongAlgorithmForKey)
        //       case JWAAlgorithm.ES512  => ZIO.fail(WrongAlgorithmForKey)
        //       case JWAAlgorithm.EdDSA  => ZIO.unit
        //   case OKPPublicKey(_, _, _, _) | OKPPrivateKey(_, _, _, _, _) =>
        //     alg match
        //       case JWAAlgorithm.ES256K => ZIO.fail(CryptoNotImplementedError)
        //       case JWAAlgorithm.ES256  => ZIO.unit
        //       case JWAAlgorithm.ES384  => ZIO.fail(CryptoNotImplementedError)
        //       case JWAAlgorithm.ES512  => ZIO.fail(CryptoNotImplementedError)
        //       case JWAAlgorithm.EdDSA  => ZIO.fail(WrongAlgorithmForKey)
        // }
        data <- ZIO.succeed(
          js.typedarray.Uint8Array.from(payload.toSeq.map(_.toShort).toJSIterable)
        )
        ret <- key.toKeyLike
          .flatMap { (thisKey, alg, kid) =>
            ZIO
              .fromPromiseJS(
                GeneralSign(data) // We can also use CompactSign
                  .tap(
                    _.addSignature(thisKey.asInstanceOf[KeyLike])
                      .setProtectedHeader(CompactJWSHeaderParameters(alg).set("kid", kid))
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
      } yield ret

    /** Based on https://github.com/panva/jose/blob/main/docs/classes/jwt_sign.SignJWT.md
      */
    def signJWT(payload: Array[Byte] /*, alg: JWAAlgorithm*/ ): IO[CryptoFailed, JWT] = {
      for {
        jwtPayload <- ZIO
          .from(js.JSON.parse(String(payload)).asInstanceOf[JWTPayload])
          .mapError(_ match {
            // case ex @ js.JavaScriptException(error: js.Error) =>
            case js.JavaScriptException(error: js.SyntaxError) =>
              CryptoFailToParse(s"JWT payload MUST be JSON: ${error.message}")
            case ex: Throwable => SomeThrowable(ex.getMessage())
          })
        ret <- key.toKeyLike
          .flatMap { (thisKey, alg, kid) =>
            ZIO
              .fromPromiseJS(
                SignJWT(jwtPayload)
                  .setProtectedHeader(JWTHeaderParameters(alg).set("kid", kid))
                  .sign(thisKey.asInstanceOf[KeyLike])
              )
              .map { jwtStr => JWT.unsafeFromEncodedJWT(jwtStr) }
              .orElseFail(UnknownError)
          }
      } yield ret

    }
  }
}
