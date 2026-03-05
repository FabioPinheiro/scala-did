package fmgp.crypto

import scala.jdk.CollectionConverters.*

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.util.StandardCharset
import fmgp.crypto.UtilsJVM.{toJWK, asNimbusds, ecKeyVerifyJWT, okpKeyVerifyJWTWithEd25519}
import zio.json.*

extension (header: JWTHeader) {
  def makeJWSHeader: JWSHeader = {
    val h = new JWSHeader.Builder(header.alg.asNimbusds)
    header match
      case JWTHeader(alg, jku, jwk, kid, typ, cty, crit) =>
        jku.map(e => h.jwkURL(java.net.URI(e)))
        jwk.map(e => h.jwk(e.toJWK))
        kid.map(e => h.keyID(e))
        typ.map(e => h.`type`(JOSEObjectType(e)))
        cty.map(e => h.contentType(e))
        crit.map(e => h.criticalParams(e.asJava))
    h.build()
  }
}

extension (jwtUnsigned: JWTUnsigned) {

  def signWith(key: OKP_EC_Key): Either[String, JWT] = {
    val header: JWSHeader = jwtUnsigned.header.makeJWSHeader
    val signingInput: Array[Byte] =
      jwtUnsigned.base64JWTFormatWithNoSignature.getBytes(StandardCharset.UTF_8)

    val signer: JWSSigner = key match
      case ecKey: ECKey =>
        jwtUnsigned.header.alg match
          case JWAAlgorithm.ES256K | JWAAlgorithm.ES256 | JWAAlgorithm.ES384 | JWAAlgorithm.ES512 =>
            new ECDSASigner(ecKey.toJWK)
          case alg => return Left(s"Unsupported combination: alg=${alg.symbol}, kty=${key.kty}")
      case okpKey: OKPKey =>
        jwtUnsigned.header.alg match
          case JWAAlgorithm.EdDSA => new Ed25519Signer(okpKey.toJWK)
          case alg                => return Left(s"Unsupported combination: alg=${alg.symbol}, kty=${key.kty}")

    signer.getJCAContext().setProvider(CryptoProvider.provider)
    val signature = signer.sign(header, signingInput)
    Right(jwtUnsigned.toJWT(signature = SignatureJWT.fromBase64url(signature.toString)))
  }
}

extension (jwt: JWT) {
  def verifyWith(key: PublicKey): Boolean = key match
    case ecKey: ECPublicKey   => ecKeyVerifyJWT(ecKey, jwt)
    case okpKey: OKPPublicKey => okpKeyVerifyJWTWithEd25519(okpKey, jwt)
}
