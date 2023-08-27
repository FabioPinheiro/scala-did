package fmgp.crypto

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._

/** methods: sign verify anonEncrypt authEncrypt anonDecrypt authDecrypt */
trait CryptoOperations {

  // ############
  // ### Sign ###
  // ############

  def sign(
      key: PrivateKey,
      plaintext: PlaintextMessage
  ): IO[CryptoFailed, SignedMessage] = sign(key, plaintext.toJson.getBytes)

  def sign(
      key: PrivateKey,
      payload: Array[Byte]
  ): IO[CryptoFailed, SignedMessage]

  def verify(
      key: PublicKey,
      jwm: SignedMessage
  ): IO[CryptoFailed, Boolean]

  // ###############
  // ### Encrypt ###
  // ###############

  def encrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage] = anonEncrypt(recipientKidsKeys, data: Array[Byte])

  def encrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage] = authEncrypt(senderKidKey, recipientKidsKeys, data)

  /** anoncrypt - Guarantees confidentiality and integrity without revealing the identity of the sender.
    */
  def anonEncrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage]

  /** authcrypt - Guarantees confidentiality and integrity. Also proves the identity of the sender – but in a way that
    * only the recipient can verify. This is the default wrapping choice, and SHOULD be used unless a different goal is
    * clearly identified. By design, this combination and all other combinations that use encryption in their outermost
    * layer share an identical IANA media type, because only the recipient should care about the difference.
    */
  def authEncrypt(
      senderKidKey: (VerificationMethodReferenced, PrivateKey),
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PublicKey)],
      data: Array[Byte]
  ): IO[CryptoFailed, EncryptedMessage]

  // ###############
  // ### Decrypt ###
  // ###############

  def decrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Array[Byte]] = anonDecrypt(recipientKidsKeys, msg)

  def decrypt(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Array[Byte]] = authDecrypt(senderKey, recipientKidsKeys, msg)

  def anonDecryptMessage(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Message] = anonDecrypt(recipientKidsKeys, msg)
    .flatMap(data =>
      ZIO.fromEither {
        String(data)
          .fromJson[Message]
          .left
          .map(info => FailToParse(s"Decoding into a Message: $info"))
      }
    )

  def authDecryptMessage(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Message] =
    authDecrypt(senderKey, recipientKidsKeys, msg)
      .flatMap(data =>
        ZIO.fromEither {
          String(data)
            .fromJson[Message]
            .left
            .map(info => FailToParse(s"Decoding into a Message: $info"))
        }
      )

  // ## Decrypt - RAW ##

  def anonDecrypt(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Array[Byte]]

  def authDecrypt(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Array[Byte]]

}
