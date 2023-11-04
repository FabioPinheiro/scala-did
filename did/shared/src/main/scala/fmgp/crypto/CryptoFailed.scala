package fmgp.crypto

import zio.json._

import fmgp.did.DIDSubject
import fmgp.did.comm._

package error {

  case class DidException(fail: DidFail) extends Exception(fail.toString())

  @jsonDiscriminator("typeOfDidFail")
  sealed trait DidFail // extends Exception with Product with Serializable
  object DidFail {
    import CryptoFailed.given
    given decoder: JsonDecoder[DidFail] = DeriveJsonDecoder.gen[DidFail]
    given encoder: JsonEncoder[DidFail] = DeriveJsonEncoder.gen[DidFail]
  }

  case class FailToParse(error: String) extends DidFail

  case class DidMethodNotSupported(method: String) extends DidFail // rename
  case class DIDSubjectNotSupported(did: DIDSubject) extends DidFail

  case class NoAgent(info: String) extends DidFail

  // ####################
  // ### Error Crypto ###
  // ####################
  @jsonDiscriminator("typeOfCryptoFailed")
  sealed trait CryptoFailed extends DidFail

  object CryptoFailed {
    // import CurveError.given
    given decoder: JsonDecoder[CryptoFailed] = DeriveJsonDecoder.gen[CryptoFailed]
    given encoder: JsonEncoder[CryptoFailed] = DeriveJsonEncoder.gen[CryptoFailed]
  }

  case class FailToGenerateKey(origin: DidFail) extends CryptoFailed

  case object CryptoNotImplementedError extends CryptoFailed // TODO
  case object UnknownError extends CryptoFailed
  case class SomeThrowable(error: String) extends CryptoFailed
  object SomeThrowable {
    def apply(throwable: Throwable) = new SomeThrowable(throwable.getClass.getName() + ":" + throwable.getMessage)
  }
  case class CryptoErrorCollection[+E <: CryptoFailed](errors: Seq[E]) extends CryptoFailed
  object CryptoErrorCollection {
    def unfold[E <: CryptoFailed, A](x: Seq[Either[E, A]]): Either[CryptoFailed, Seq[A]] =
      x.partition(_.isLeft).asInstanceOf[(Seq[Left[E, A]], Seq[Right[E, A]])] match {
        case (l, r) if l.isEmpty => Right(r.map(_.value))
        case (l, _) =>
          val tmp = l.map(_.value)
          if (tmp.size == 1) Left(tmp.head)
          else Left(CryptoErrorCollection(tmp))
      }
  }
  case class CryptoFailToParse(error: String) extends CryptoFailed

  case object KeyMissingEpkJWEHeader extends CryptoFailed // TODO make it time safe
  case object MissingFromHeader extends CryptoFailed

  /* EX: Curve of public key does not match curve of private key */
  @jsonDiscriminator("typeOfCurveError")
  sealed trait CurveError extends CryptoFailed
  object CurveError {
    given decoder: JsonDecoder[CurveError] = DeriveJsonDecoder.gen[CurveError]
    given encoder: JsonEncoder[CurveError] = DeriveJsonEncoder.gen[CurveError]
  }
  // type CurveError = CryptoFailed

  case class UnsupportedCurve(obtained: Curve, supported: Set[Curve]) extends CurveError // rename
  case class WrongCurve(obtained: Curve, expected: Set[Curve]) extends CurveError
  case class MissingCurve(expected: Set[Curve]) extends CurveError
  case class MultiCurvesTypes(obtained: Set[Curve], expected: Set[Curve]) extends CurveError
  case object WrongKeysTypeCombination extends CurveError

  case object EncryptionFailed extends CryptoFailed
  case class DecryptionFailed(reason: String) extends CryptoFailed
  case class FailDecryptDoubleSign(outsideMsg: SignedMessage, insideMsg: SignedMessage) extends CurveError
  case class FailDecryptSignThenEncrypted(outsideMsg: SignedMessage, insideMsg: EncryptedMessage) extends CurveError
  case class FailDecryptDoubleEncrypted(outsideMsg: EncryptedMessage, insideMsg: EncryptedMessage) extends CurveError
  case object AnonDecryptAuthMsgFailed extends CryptoFailed
  case object AuthDecryptAnonMsgFailed extends CryptoFailed
  case object NoSenderOrRecipient extends CryptoFailed
  case object ValidationFailed extends CryptoFailed

  case object NoKeys extends CryptoFailed
  case class PointNotOnCurve(error: String) extends CryptoFailed
  case object IncompatibleKeys extends CryptoFailed
  case class MissingDecryptionKey(kid: String*) extends CryptoFailed {
    def `+`(ex: MissingDecryptionKey) = MissingDecryptionKey((kid ++ ex.kid): _*)
  }
  case object SignatureVerificationFailed extends CryptoFailed
  case object MACCheckFailed extends CryptoFailed
  case object MultiDifferentResults extends CryptoFailed
  case object ZeroResults extends CryptoFailed

  // ######################
  // ### Error Protocol ###
  // ######################

  @jsonDiscriminator("typeOfProtocolFail")
  sealed trait ProtocolFail extends DidFail {
    def piuri: PIURI
  }

  object ProtocolFail {
    // import CurveError.given
    given decoder: JsonDecoder[ProtocolFail] = DeriveJsonDecoder.gen[ProtocolFail]
    given encoder: JsonEncoder[ProtocolFail] = DeriveJsonEncoder.gen[ProtocolFail]
  }

  case class MissingProtocol(piuri: PIURI) extends ProtocolFail
  case class FailToEncodeMessage(piuri: PIURI, error: String) extends ProtocolFail
}
