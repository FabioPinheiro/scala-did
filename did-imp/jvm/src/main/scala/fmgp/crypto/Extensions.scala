package fmgp.crypto

import scala.jdk.CollectionConverters.*

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.util.StandardCharset
import fmgp.crypto.UtilsJVM.*
import zio.json.*

extension (jwtUnsigned: JWTUnsigned) {

  def signWith(key: OKP_EC_Key): Either[String, JWT] = {
    val hack: ProtectedHeaderJWT = jwtUnsigned.protectedHeader
    for {
      header: JWSHeader <-
        try Right(JWSHeader.parse(jwtUnsigned.protectedHeader.content))
        catch {
          case ex: java.text.ParseException => Left(s"Fail to parse Header becuase: ${ex.getMessage()}")
        }
      jwtHeader <- jwtUnsigned.header
      signer: JWSSigner <- key match {
        case ecKey: ECKey =>
          jwtHeader.alg match
            case JWAAlgorithm.ES256K | JWAAlgorithm.ES256 | JWAAlgorithm.ES384 | JWAAlgorithm.ES512 =>
              Right(new ECDSASigner(ecKey.toJWK))
            case alg => Left(s"Unsupported combination: alg=${alg.symbol}, kty=${key.kty}")
        case okpKey: OKPKey =>
          jwtHeader.alg match
            case JWAAlgorithm.EdDSA => Right(new Ed25519Signer(okpKey.toJWK))
            case alg                => Left(s"Unsupported combination: alg=${alg.symbol}, kty=${key.kty}")
      }
      _ = signer.getJCAContext().setProvider(CryptoProvider.provider)
      signingInput: Array[Byte] = jwtUnsigned.base64JWTFormatWithNoSignature.getBytes(StandardCharset.UTF_8)
      signatureStr = signer.sign(header, signingInput).toString
      signature <- SignatureJWT.fromBase64url(signatureStr)
    } yield jwtUnsigned.toJWT(signature)
  }
}

extension (jwt: JWT) {
  def verifyWith(key: PublicKey): Boolean = key match
    case ecKey: ECPublicKey   => ecKeyVerifyJWT(ecKey, jwt)
    case okpKey: OKPPublicKey => okpKeyVerifyJWTWithEd25519(okpKey, jwt)
}
