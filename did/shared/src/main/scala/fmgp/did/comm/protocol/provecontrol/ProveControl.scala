package fmgp.did.comm.protocol.provecontrol

import zio.json.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.crypto.KTY
import fmgp.crypto.SHA256

// RequestVerification VerificationChallenge Prove ConfirmVerification
extension (msg: PlaintextMessage)
  def toRequestVerification: Either[String, RequestVerification] =
    RequestVerification.fromPlaintextMessage(msg)
  def toVerificationChallenge: Either[String, VerificationChallenge] =
    VerificationChallenge.fromPlaintextMessage(msg)
  def toProve: Either[String, Prove] =
    Prove.fromPlaintextMessage(msg)
  def toConfirmVerification: Either[String, ConfirmVerification] =
    ConfirmVerification.fromPlaintextMessage(msg)

enum VerificationType:
  case Email extends VerificationType
  case DID extends VerificationType
  case Discord extends VerificationType
  case Tel extends VerificationType
  case Domain extends VerificationType
  case IP extends VerificationType
  case Address extends VerificationType

object VerificationType {
  given decoder: JsonDecoder[VerificationType] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(VerificationType.valueOf(e)))
  given encoder: JsonEncoder[VerificationType] =
    JsonEncoder.string.contramap((e: VerificationType) => e.toString)
}

// ### RequestVerification
case class RequestVerification(
    id: MsgID = MsgID(),
    to: TO,
    from: FROM,
    verificationType: VerificationType,
    subject: String,
) {
  def `type` = RequestVerification.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = `type`,
      id = id,
      to = Some(Set(to)),
      from = Some(from),
      body = Some(RequestVerification.Body(verificationType, subject).toJSON_RFC7159),
    )
}

object RequestVerification {
  def piuri = PIURI("https://fmgp.app/provecontrol/1/requestverification")

  @jsonMemberNames(SnakeCase)
  protected final case class Body(verificationType: VerificationType, subject: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, RequestVerification] =
    if (msg.`type` != piuri) Left(s"No able to create RequestVerification from a Message of type '${msg.`type`}'")
    else {
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.from match
                  case None => Left(s"'$piuri' MUST have field 'from'")
                  case Some(from) =>
                    Right(
                      RequestVerification(
                        id = msg.id,
                        to = firstTo,
                        from = from,
                        verificationType = body.verificationType,
                        subject = body.subject,
                      )
                    )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
    }
}

// ### VerificationChallenge
case class VerificationChallenge(
    id: MsgID = MsgID(),
    to: TO,
    from: FROM,
    thid: NotRequired[MsgID], // 'id' from RequestVerification
    verificationType: VerificationType,
    subject: String,
    secret: String, // MUST NOT be shared
) {
  def `type` = VerificationChallenge.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = `type`,
      id = id,
      to = Some(Set(to)),
      from = Some(from),
      thid = thid,
      body = Some(VerificationChallenge.Body(verificationType, subject, secret).toJSON_RFC7159),
    )

  def calculateProof = VerificationChallenge.calculateProof(
    verifier = from.toDID,
    user = to.toDID,
    verificationType = verificationType,
    subject = subject,
    secret = secret,
  )

  def makeProveMessage = Prove(
    to = from.asTO,
    from = to.asFROM,
    thid = id,
    verificationType = verificationType,
    subject = subject,
    proof = calculateProof,
  )
}

/** This message MUST be send using the transport in 'verificationType' */
object VerificationChallenge {
  def piuri = PIURI("https://fmgp.app/provecontrol/1/verificationchallenge")

  export Prove.calculateProof

  @jsonMemberNames(SnakeCase)
  protected final case class Body(verificationType: VerificationType, subject: String, secret: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }
  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, VerificationChallenge] =
    if (msg.`type` != piuri) Left(s"No able to create VerificationChallenge from a Message of type '${msg.`type`}'")
    else {
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.from match
                  case None => Left(s"'$piuri' MUST have field 'from'")
                  case Some(from) =>
                    Right(
                      VerificationChallenge(
                        id = msg.id,
                        to = firstTo,
                        from = from,
                        thid = msg.thid,
                        verificationType = body.verificationType,
                        subject = body.subject,
                        secret = body.secret,
                      )
                    )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
    }
}

// ### Prove
case class Prove(
    id: MsgID = MsgID(),
    to: TO,
    from: FROM,
    thid: MsgID, // 'id' from VerificationChallenge
    verificationType: VerificationType,
    subject: String,
    proof: String, // This is calculate from the VerificationChallenge
) {
  def `type` = Prove.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = `type`,
      id = id,
      to = Some(Set(to)),
      from = Some(from),
      thid = Some(thid),
      body = Some(Prove.Body(verificationType, subject, proof).toJSON_RFC7159),
    )
}

object Prove {
  def piuri = PIURI("https://fmgp.app/provecontrol/1/prove")

  def calculateProof(
      verifier: DIDSubject, // TO in Prove == FROM in VerificationChallenge
      user: DIDSubject, // FROM in Prove == TO in VerificationChallenge
      verificationType: VerificationType,
      subject: String,
      secret: String,
  ) = SHA256.digestToHex(s"$verifier|$user|$verificationType|$subject|$secret")

  @jsonMemberNames(SnakeCase)
  protected final case class Body(verificationType: VerificationType, subject: String, proof: String) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }
  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, Prove] =
    if (msg.`type` != piuri) Left(s"No able to create Prove from a Message of type '${msg.`type`}'")
    else {
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.thid match
                  case None => Left(s"'$piuri' MUST have field 'thid'")
                  case Some(thid) =>
                    msg.from match
                      case None => Left(s"'$piuri' MUST have field 'from'")
                      case Some(from) =>
                        Right(
                          Prove(
                            id = msg.id,
                            to = firstTo,
                            from = from,
                            thid = thid,
                            verificationType = body.verificationType,
                            subject = body.subject,
                            proof = body.proof,
                          )
                        )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
    }
}

// ### ConfirmVerification
case class ConfirmVerification(
    id: MsgID = MsgID(),
    to: TO,
    from: FROM,
    thid: MsgID, // 'id' from Prove
    verificationType: VerificationType,
    subject: String,
    attachments: Seq[Attachment],
) {
  def `type` = ConfirmVerification.piuri
  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      `type` = `type`,
      id = id,
      to = Some(Set(to)),
      from = Some(from),
      thid = Some(thid),
      body = Some(ConfirmVerification.Body(verificationType, subject).toJSON_RFC7159),
    )

}

object ConfirmVerification {
  def piuri = PIURI("https://fmgp.app/provecontrol/1/confirmverification")

  @jsonMemberNames(SnakeCase)
  protected final case class Body(verificationType: VerificationType, subject: String) { // TODO jwt

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, ConfirmVerification] =
    if (msg.`type` != piuri) Left(s"No able to create ConfirmVerification from a Message of type '${msg.`type`}'")
    else {
      msg.to.toSeq.flatten match // Note: toSeq is from the match
        case Seq() => Left(s"'$piuri' MUST have field 'to' with one element")
        case firstTo +: Seq() =>
          msg.body match
            case None => Left(s"'$piuri' MUST have field 'body'")
            case Some(b) =>
              b.as[Body].flatMap { body =>
                msg.thid match
                  case None => Left(s"'$piuri' MUST have field 'thid'")
                  case Some(thid) =>
                    msg.from match
                      case None => Left(s"'$piuri' MUST have field 'from'")
                      case Some(from) =>
                        Right(
                          ConfirmVerification(
                            id = msg.id,
                            to = firstTo,
                            from = from,
                            thid = thid,
                            verificationType = body.verificationType,
                            subject = body.subject,
                            attachments = msg.attachments.toSeq.flatten // attachments.toMap
                          )
                        )
              }
        case firstTo +: tail => Left(s"'$piuri' MUST have field 'to' with only one element")
    }
}
