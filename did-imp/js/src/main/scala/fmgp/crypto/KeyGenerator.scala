package fmgp.crypto

import zio._
import zio.json._
import scala.scalajs.js

import fmgp.typings.jose.mod.generateKeyPair
import fmgp.typings.jose.mod.exportJWK
import fmgp.typings.jose.generateKeyPairMod.GenerateKeyPairResult
import fmgp.typings.jose.generateKeyPairMod.GenerateKeyPairOptions

import fmgp.crypto.error._

object KeyGenerator {

  /** @see
    *   https://crypto.stackexchange.com/questions/98561/ed25519-to-x25519-transportation
    */
  def makeX25519: IO[FailToGenerateKey, OKPPrivateKey] = makeKeyOKP("ECDH-ES+A256KW", "X25519")
  // ZIO.fail(FailToGenerateKey(CryptoNotImplementedError))

  def makeEd25519: IO[FailToGenerateKey, OKPPrivateKey] = makeKeyOKP("EdDSA", "Ed25519")

  // let keyPair = await window.crypto.subtle.generateKey({name: "ECDSA",namedCurve: "P-384"},true,["sign", "verify"]);
  // import org.scalajs.dom.*
  // import org.scalajs.dom.KeyAlgorithm
  // import org.scalajs.dom.KeyUsage
  // def native: IO[FailToGenerateKey, OKPPrivateKey] = {
  //   for {
  //     keyPair <-
  //       ZIO
  //         .fromPromiseJS(
  //           crypto.subtle
  //             .generateKey(
  //               // EcKeyGenParams https://developer.mozilla.org/en-US/docs/Web/API/EcKeyGenParams
  //               js.Dictionary("name" -> "ECDSA", "namedCurve" -> "P-256").asInstanceOf[KeyAlgorithm],
  //               true,
  //               js.Array(KeyUsage.sign, KeyUsage.verify)
  //             )
  //         )
  //         .map(_.asInstanceOf[CryptoKeyPair])
  //         .catchAll { case ex =>
  //           println("makeX25519 error " + ex)
  //           ZIO.fail(FailToGenerateKey(SomeThrowable(ex)))
  //         // Like scala.scalajs.js.JavaScriptException: JOSENotSupported: Invalid or unsupported JWK "alg" (Algorithm) Parameter value
  //         }
  //     jwk <- ZIO
  //       .fromPromiseJS(
  //         crypto.subtle
  //           .exportKey(
  //             format = KeyFormat.jwk,
  //             key = keyPair.privateKey
  //           )
  //       )
  //       .catchAll { case ex =>
  //         println("exportKey " + ex)
  //         ZIO.fail(FailToGenerateKey(SomeThrowable(ex)))
  //       // Like scala.scalajs.js.JavaScriptException: JOSENotSupported: Invalid or unsupported JWK "alg" (Algorithm) Parameter value
  //       }
  //     _ <- Console
  //       .printLine(JSON.stringify(jwk))
  //       .catchAll { case ex =>
  //         ZIO.fail(FailToGenerateKey(SomeThrowable(ex)))
  //       }
  //     ret: OKPPrivateKey <- ZIO.fail(FailToGenerateKey(CryptoNotImplementedError))
  //   } yield (ret)
  // }

  // ES256 (P-256) //ES256K (secp256k1)
  def makeKeyEC(alg: String): IO[FailToGenerateKey, ECPrivateKey] =
    for {
      keyPair <-
        ZIO
          .fromPromiseJS(generateKeyPair(alg))
          .map(_.asInstanceOf[GenerateKeyPairResult[fmgp.typings.jose.typesMod.KeyLike]])
          // Can have error like Like scala.scalajs.js.JavaScriptException: JOSENotSupported: Invalid or unsupported JWK "alg" (Algorithm) Parameter value
          .catchAll { case ex => ZIO.fail(FailToGenerateKey(SomeThrowable(ex))) }
      jwk <- ZIO
        .fromPromiseJS(exportJWK(keyPair.privateKey))
        .catchAll { case ex => ZIO.fail(FailToGenerateKey(SomeThrowable(ex))) }
      str = js.JSON.stringify(jwk)
      ret <- str.fromJson[ECPrivateKey] match
        case Left(fail)   => ZIO.fail(FailToGenerateKey(CryptoFailToParse(fail)))
        case Right(value) => ZIO.succeed(value)
    } yield (ret)

  // EdDSA Ed25519 //ECDH-ES+A256KW X25519
  def makeKeyOKP(alg: String, crv: String): IO[FailToGenerateKey, OKPPrivateKey] =
    for {
      keyPair <-
        ZIO
          .fromPromiseJS(generateKeyPair(alg = alg, options = GenerateKeyPairOptions().setCrv(crv)))
          .map(_.asInstanceOf[GenerateKeyPairResult[fmgp.typings.jose.typesMod.KeyLike]])
          // Can have error like Like scala.scalajs.js.JavaScriptException: JOSENotSupported: Invalid or unsupported JWK "alg" (Algorithm) Parameter value
          .catchAll { case ex => ZIO.fail(FailToGenerateKey(SomeThrowable(ex))) }
      jwk <- ZIO
        .fromPromiseJS(exportJWK(keyPair.privateKey))
        .catchAll { case ex => ZIO.fail(FailToGenerateKey(SomeThrowable(ex))) }
      str = js.JSON.stringify(jwk)
      ret <- str.fromJson[OKPPrivateKey] match
        case Left(fail)   => ZIO.fail(FailToGenerateKey(CryptoFailToParse(fail)))
        case Right(value) => ZIO.succeed(value)
    } yield (ret)
}
