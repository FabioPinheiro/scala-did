package fmgp.crypto

import java.util.Collections
import scala.jdk.CollectionConverters.*
import scala.util.Try

import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.impl.AAD
import com.nimbusds.jose.crypto.impl.AESKW
import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.crypto.impl.ECDHCryptoProvider
import com.nimbusds.jose.crypto.impl.ECDH1PU
import com.nimbusds.jose.crypto.impl.ECDH1PUCryptoProvider
import com.nimbusds.jose.crypto.impl.ContentCryptoProvider
import com.nimbusds.jose.jwk.{Curve as JWKCurve}
import com.nimbusds.jose.jwk.{ECKey as JWKECKey}
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jose.util.StandardCharset
import com.nimbusds.jose.JWECryptoParts
import javax.crypto.SecretKey

import zio.json.*
import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm.*
import fmgp.util.*
import fmgp.crypto.JWERecipient
import fmgp.crypto.error.*
import com.nimbusds.jose.crypto.impl.JWEHeaderValidation

/** Elliptic-curve Diffie–Hellman */
case class ECDH_AnonCryptoProvider(val curve: JWKCurve, val cek: SecretKey) extends ECDHCryptoProvider(curve, cek) {

  override def supportedEllipticCurves(): java.util.Set[JWKCurve] = Set(curve).asJava

  def encryptAUX(
      header: ProtectedHeader,
      sharedSecrets: Seq[(VerificationMethodReferenced, javax.crypto.SecretKey)],
      clearText: Array[Byte],
      aad: Array[Byte],
  ): EncryptedMessageGeneric = { // FIXME ERRORs
    import UtilsJVM.unsafe.given

    val alg = JWEHeaderValidation.getAlgorithmAndEnsureNotNull(header)
    val algMode: ECDH.AlgorithmMode = ECDH.resolveAlgorithmMode(alg)
    assert(algMode == ECDH.AlgorithmMode.KW)

    sharedSecrets match {
      case head :: tail =>
        val headParts: JWECryptoParts = encryptWithZ(header, head._2, clearText, aad)

        val recipients = tail.map { rs =>
          val sharedKey: SecretKey = ECDH.deriveSharedKey(header, rs._2, getConcatKDF)
          val encryptedKey = Base64.encode(AESKW.wrapCEK(cek, sharedKey, getJCAContext.getKeyEncryptionProvider))
          (rs._1, encryptedKey)
        }

        val auxRecipient = ((head._1, Base64.fromBase64url(headParts.getEncryptedKey.toString())) +: recipients)
          .map(e => Recipient(e._2, RecipientHeader(e._1)))

        val protectedHeader: ProtectedHeader =
          headParts.getHeader().toString.fromJson[ProtectedHeader].toOption.get // FIXME get

        EncryptedMessageGeneric(
          ciphertext = CipherText(headParts.getCipherText().toString),
          `protected` = Base64Obj(protectedHeader),
          recipients = auxRecipient.toSeq,
          tag = TAG(headParts.getAuthenticationTag().toString),
          iv = IV(headParts.getInitializationVector().toString),
        )
    }

  }

  def decryptAUX(
      header: ProtectedHeaderBase64,
      sharedSecrets: Seq[(VerificationMethodReferenced, SecretKey)],
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
      aad: Array[Byte],
  ): Either[CryptoFailed, Array[Byte]] = {

    val tmp = sharedSecrets.map { case (vmr, secretKey) =>
      recipients
        .find(recipient => recipient.vmr == vmr)
        .map(_.encryptedKey)
        .map(encryptedKey =>
          Try(
            decryptWithZ(header, aad, secretKey, encryptedKey, iv.base64, cipherText.base64, authTag.base64)
          ).toEither.left
            .map {
              case ex: com.nimbusds.jose.JOSEException if ex.getMessage == "MAC check failed" => MACCheckFailed
              case ex: com.nimbusds.jose.JOSEException                                        => SomeThrowable(ex)
            }
        )
    }.flatten

    CryptoErrorCollection.unfold(tmp).flatMap { result =>
      if (result.tail.forall(_.sameElements(result.head)))
        result.headOption match
          case Some(value) => Right(value)
          case None        => Left(ZeroResults)
      else Left(MultiDifferentResults)
    }
  }
}

/** Elliptic-curve Diffie–Hellman */
case class ECDH_AuthCryptoProvider(val curve: JWKCurve, val cek: SecretKey) extends ECDH1PUCryptoProvider(curve, cek) {

  override def supportedEllipticCurves(): java.util.Set[JWKCurve] = Set(curve).asJava

  def encryptAUX(
      header: ProtectedHeader,
      sharedSecrets: Seq[(VerificationMethodReferenced, javax.crypto.SecretKey)],
      clearText: Array[Byte],
      aad: Array[Byte],
  ): EncryptedMessageGeneric = { // FIXME ERRORs
    import UtilsJVM.unsafe.given

    val alg = JWEHeaderValidation.getAlgorithmAndEnsureNotNull(header)
    val algMode: ECDH.AlgorithmMode = ECDH1PU.resolveAlgorithmMode(alg)
    assert(algMode == ECDH.AlgorithmMode.KW)

    sharedSecrets match {
      case head :: tail =>
        val headParts: JWECryptoParts = encryptWithZ(header, head._2, clearText, aad)

        val recipients = tail.map { rs =>
          val sharedKey: SecretKey =
            ECDH1PU.deriveSharedKey(header, rs._2, headParts.getAuthenticationTag, getConcatKDF)
          val encryptedKey = Base64.encode(AESKW.wrapCEK(cek, sharedKey, getJCAContext.getKeyEncryptionProvider))
          (rs._1, encryptedKey)
        }

        val auxRecipient = ((head._1, Base64.fromBase64url(headParts.getEncryptedKey.toString())) +: recipients)
          .map(e => Recipient(e._2, RecipientHeader(e._1)))

        val protectedHeader: ProtectedHeader =
          headParts.getHeader().toString.fromJson[ProtectedHeader].toOption.get // FIXME get

        EncryptedMessageGeneric(
          ciphertext = CipherText(headParts.getCipherText().toString),
          `protected` = Base64Obj(protectedHeader),
          recipients = auxRecipient.toSeq,
          tag = TAG(headParts.getAuthenticationTag().toString),
          iv = IV(headParts.getInitializationVector().toString)
        )
    }
  }

  def decryptAUX(
      header: ProtectedHeaderBase64,
      sharedSecrets: Seq[(VerificationMethodReferenced, SecretKey)],
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
      aad: Array[Byte],
  ): Either[CryptoFailed, Array[Byte]] = {

    val tmp = sharedSecrets.map { case (vmr, secretKey) =>
      recipients
        .find(recipient => recipient.vmr == vmr)
        .map(_.encryptedKey)
        .map(encryptedKey =>
          Try(
            decryptWithZ(header, aad, secretKey, encryptedKey, iv.base64, cipherText.base64, authTag.base64)
          ).toEither.left
            .map {
              case ex: com.nimbusds.jose.JOSEException if ex.getMessage == "MAC check failed" => MACCheckFailed
              case ex: com.nimbusds.jose.JOSEException                                        => SomeThrowable(ex)
            }
        )
    }.flatten

    CryptoErrorCollection.unfold(tmp).flatMap { result =>
      if (result.tail.forall(_.sameElements(result.head)))
        result.headOption match
          case Some(value) => Right(value)
          case None        => Left(ZeroResults)
      else Left(MultiDifferentResults)
    }

  }

}
