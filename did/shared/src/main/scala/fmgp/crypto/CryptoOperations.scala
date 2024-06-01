package fmgp.crypto

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._

/** Raw crypto operations */
trait CryptoOperations extends CryptoDIDCommOperations with CryptoJWTOperations

/** Raw crypto operations for JWT */
trait CryptoJWTOperations {

  def signJWT(
      key: PrivateKey,
      payload: Array[Byte],
      // alg: JWAAlgorithm
  ): IO[CryptoFailed, JWT]

  def verifyJWT(
      key: PublicKey,
      jwt: JWT
  ): IO[CryptoFailed, Boolean]

}

/** Raw crypto operations for DID Comm
  *
  * methods: sign verify anonEncrypt authEncrypt anonDecrypt authDecrypt
  */
trait CryptoDIDCommOperations {

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
      msg: EncryptedMessage // TODO make more type safe
  ): IO[DidFail, Array[Byte]] = anonDecrypt(recipientKidsKeys, msg)

  def decrypt(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage // TODO make more type safe
  ): IO[DidFail, Array[Byte]] = authDecrypt(senderKey, recipientKidsKeys, msg)

  def anonDecryptMessage(
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Message] =
    msg.`protected`.obj.match
      case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
        ZIO.fail(AnonDecryptAuthMsgFailed)
      case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
        anonDecrypt(recipientKidsKeys, msg)
          .flatMap(data =>
            ZIO.fromEither {
              String(data)
                .fromJson[Message]
                .left
                .map(info => FailToParse(s"Decoding into a Message: $info"))
            }
          )
          .reject {
            // FIXME case out: SignedMessage    => CryptoNotImplementedError // TODO
            // FIXME case out: EncryptedMessage => CryptoNotImplementedError // TODO
            // this is tested by "decrypt encryptedMessage_EdDSA_ECDH1PU_X25519_A256CBCHS512__ECDHES_X25519_XC20P"
            case out: PlaintextMessage if {
                  val recipientsKID = msg.recipients.map(_.recipientKid.value)
                  val keysKID = recipientKidsKeys.map(_._1.value)
                  !(keysKID.forall(s => recipientsKID.contains(s))) // Only use keys that makes sense
                } =>
              DecryptionFailed("Only use keys that is expeted by the Message")
            case out: PlaintextMessage if {
                  val recipientDIDs = msg.recipients.map(_.recipientKid.did.did).toSet
                  val outToDIDs = out.to.getOrElse(Set.empty).map(_.toDID.did)
                  !(recipientDIDs == outToDIDs) // Outer and inner message MUST have the same recipients
                } =>
              DecryptionFailed("Outer and inner message MUST have the same recipients")
          }

  def authDecryptMessage(
      senderKey: PublicKey,
      recipientKidsKeys: Seq[(VerificationMethodReferenced, PrivateKey)],
      msg: EncryptedMessage
  ): IO[DidFail, Message] =
    msg.`protected`.obj.match
      case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
        ZIO.fail(AuthDecryptAnonMsgFailed)
      case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
        authDecrypt(senderKey, recipientKidsKeys, msg)
          .flatMap(data =>
            ZIO.fromEither {
              String(data)
                .fromJson[Message]
                .left
                .map(info => FailToParse(s"Decoding into a Message: $info"))
            }
          )
          .reject {
            // FIXME case out: SignedMessage => CryptoNotImplementedError // TODO
            // this is tested by "decrypt encryptedMessage_EdDSA_ECDH1PU_P521_A256CBCHS512"
            // FIXME case out: EncryptedMessage => CryptoNotImplementedError // TODO
            case out: PlaintextMessage if {
                  val recipientsKID = msg.recipients.map(_.recipientKid.value)
                  val keysKID = recipientKidsKeys.map(_._1.value)
                  !(keysKID.forall(s => recipientsKID.contains(s))) // Only use keys that makes sense
                } =>
              DecryptionFailed("Only use keys that is expeted by the Message")
            case out: PlaintextMessage if {
                  val recipientDIDs = msg.recipients.map(_.recipientKid.did.did).toSet
                  val outToDIDs = out.to.getOrElse(Set.empty).map(_.toDID.did)
                  !(recipientDIDs == outToDIDs) // Outer and inner message MUST have the same recipients
                } =>
              DecryptionFailed("Outer and inner message MUST have the same recipients")
            case out: PlaintextMessage if !(out.from.exists(_.toDID.string == skid.did.string)) =>
              DecryptionFailed("Outer Message skid Inner MUST correspond to the inner message FROM")
          }

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
