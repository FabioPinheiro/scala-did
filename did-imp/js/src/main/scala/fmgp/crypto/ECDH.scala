package fmgp.crypto

import fmgp.did.VerificationMethodReferenced
import fmgp.did.VerificationMethodReferencedWithKey
import fmgp.did.comm._
import fmgp.crypto.error._

object ECDH {
  def anonEncryptEC(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: AnonHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    Left(CryptoNotImplementedError)

  def anonDecryptEC(
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    Left(CryptoNotImplementedError)

  def authEncryptEC(
      sender: ECKey,
      ecRecipientsKeys: Seq[VerificationMethodReferencedWithKey[ECPublicKey]],
      header: AuthHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    Left(CryptoNotImplementedError)

  def authDecryptEC(
      sender: ECKey,
      ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    Left(CryptoNotImplementedError)

  def anonEncryptOKP(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: AnonHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    Left(CryptoNotImplementedError)

  def anonDecryptOKP(
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    Left(CryptoNotImplementedError)

  def authEncryptOKP(
      sender: OKPKey,
      okpRecipientsKeys: Seq[VerificationMethodReferencedWithKey[OKPPublicKey]],
      header: AuthHeaderBuilder,
      clearText: Array[Byte],
  ): Either[CryptoFailed, EncryptedMessageGeneric] =
    Left(CryptoNotImplementedError)

  def authDecryptOKP(
      sender: OKPKey,
      okpRecipientsKeys: Seq[(VerificationMethodReferenced, OKPKey)],
      header: ProtectedHeaderBase64,
      recipients: Seq[JWERecipient],
      iv: IV,
      cipherText: CipherText,
      authTag: TAG,
  ): Either[CryptoFailed, Array[Byte]] =
    Left(CryptoNotImplementedError)
}
