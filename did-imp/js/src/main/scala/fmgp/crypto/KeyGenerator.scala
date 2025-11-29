package fmgp.crypto

import zio.*
import zio.json.*
import scala.scalajs.js

import fmgp.crypto.error.*

object KeyGenerator extends KeyGeneratorWeb with KeyGeneratorJose {

  /** @see
    *   https://crypto.stackexchange.com/questions/98561/ed25519-to-x25519-transportation
    */
  def makeX25519: IO[FailToGenerateKey, OKPPrivateKey] = makeKeyOKP(Curve.X25519)
  def joseMakeX25519J: IO[FailToGenerateKey, OKPPrivateKey] = joseMakeKeyOKP("ECDH-ES+A256KW", "X25519")

  // ZIO.fail(FailToGenerateKey(CryptoNotImplementedError))

  def makeEd25519: IO[FailToGenerateKey, OKPPrivateKey] = makeKeyOKP(Curve.Ed25519)
  def joseMakeEd25519: IO[FailToGenerateKey, OKPPrivateKey] = joseMakeKeyOKP("EdDSA", "Ed25519")
}

/** https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto
  *
  * From version 113: this feature is behind the #enable-experimental-web-platform-features preference (needs to be set
  * to Enabled). To change preferences in Chrome, visit chrome://flags.
  *
  * #enable-experimental-web-platform-features
  */
trait KeyGeneratorWeb {
  import org.scalajs.dom.KeyUsage
  import org.scalajs.dom.KeyAlgorithm
  import org.scalajs.dom.KeyFormat
  import org.scalajs.dom.crypto.subtle.generateKey
  import org.scalajs.dom.crypto.subtle.exportKey

  // let keyPair = await window.crypto.subtle.generateKey({name: "ECDSA",namedCurve: "P-384"},true,["sign", "verify"]);

  def makeKeyEC(
      crv: Curve.`P-256`.type | Curve.`P-384`.type | Curve.`P-521`.type
  ): IO[FailToGenerateKey, ECPrivateKey] =
    for {
      keyPair <-
        ZIO
          .fromPromiseJS(
            generateKey(
              // EcKeyGenParams https://developer.mozilla.org/en-US/docs/Web/API/EcKeyGenParams
              js.Dictionary("name" -> "ECDSA", "namedCurve" -> crv.toString).asInstanceOf[KeyAlgorithm],
              true,
              js.Array(KeyUsage.sign, KeyUsage.verify)
            )
          )
          .map(_.asInstanceOf[org.scalajs.dom.CryptoKeyPair])
          // Can have error like Like scala.scalajs.js.JavaScriptException: JOSENotSupported: Invalid or unsupported JWK "alg" (Algorithm) Parameter value
          .catchAll { case ex => ZIO.fail(FailToGenerateKey(SomeThrowable(ex))) }
      jwk <- ZIO
        .fromPromiseJS(exportKey(format = KeyFormat.jwk, key = keyPair.privateKey))
        .catchAll { case ex => ZIO.fail(FailToGenerateKey(SomeThrowable(ex))) }
      str = js.JSON.stringify(jwk)
      ret <- str.fromJson[ECPrivateKey] match
        case Left(fail)   => ZIO.fail(FailToGenerateKey(CryptoFailToParse(fail)))
        case Right(value) => ZIO.succeed(value)
    } yield (ret)

  /** Notes:
    *
    * (node:20811) ExperimentalWarning: The X25519 Web Crypto API algorithm is an experimental feature and might change
    * at any time
    *
    * (node:20811) ExperimentalWarning: The Ed25519 Web Crypto API algorithm is an experimental feature and might change
    * at any time
    */
  def makeKeyOKP(
      crv: Curve.Ed25519.type | Curve.X25519.type
  ): IO[FailToGenerateKey, OKPPrivateKey] = {
    // EcKeyGenParams https://developer.mozilla.org/en-US/docs/Web/API/EcKeyGenParams
    def aux = crv match {
      case Curve.Ed25519 =>
        val keyAlgorithm = js.Dictionary("name" -> "Ed25519").asInstanceOf[KeyAlgorithm]
        generateKey(keyAlgorithm, true, js.Array(KeyUsage.sign, KeyUsage.verify))
      case Curve.X25519 =>
        val keyAlgorithm = js.Dictionary("name" -> "X25519").asInstanceOf[KeyAlgorithm]
        generateKey(keyAlgorithm, true, js.Array(KeyUsage.deriveKey))
    }
    for {
      keyPair <-
        ZIO
          .fromPromiseJS(aux)
          .map(_.asInstanceOf[org.scalajs.dom.CryptoKeyPair])
          // Can have error like Like scala.scalajs.js.JavaScriptException: JOSENotSupported: Invalid or unsupported JWK "alg" (Algorithm) Parameter value
          .catchAll { case ex => ZIO.fail(FailToGenerateKey(SomeThrowable(ex))) }
      jwk <- ZIO
        .fromPromiseJS(exportKey(format = KeyFormat.jwk, key = keyPair.privateKey))
        .catchAll { case ex => ZIO.fail(FailToGenerateKey(SomeThrowable(ex))) }
      str = js.JSON.stringify(jwk)
      ret <- str.fromJson[OKPPrivateKey] match
        case Left(fail)   => ZIO.fail(FailToGenerateKey(CryptoFailToParse(fail)))
        case Right(value) => ZIO.succeed(value)
    } yield (ret)
  }
}

trait KeyGeneratorJose {
  import fmgp.typings.jose.mod.generateKeyPair
  import fmgp.typings.jose.mod.exportJWK
  import fmgp.typings.jose.generateKeyPairMod.GenerateKeyPairResult
  import fmgp.typings.jose.generateKeyPairMod.GenerateKeyPairOptions

  // ES256 (P-256) //ES256K (secp256k1)
  def joseMakeKeyEC(alg: String): IO[FailToGenerateKey, ECPrivateKey] =
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
  def joseMakeKeyOKP(alg: String, crv: String): IO[FailToGenerateKey, OKPPrivateKey] =
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
