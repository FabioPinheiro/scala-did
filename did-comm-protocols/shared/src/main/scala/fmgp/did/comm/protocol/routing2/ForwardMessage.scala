package fmgp.did.comm.protocol.routing2

import zio.*
import zio.json.*

import fmgp.did.*
import fmgp.did.comm.*
import fmgp.crypto.error.*
import fmgp.util.Base64

extension (msg: PlaintextMessage)
  def toForwardMessage: Either[String, ForwardMessage] =
    ForwardMessage.fromPlaintextMessage(msg)

extension (msg: SignedMessage | EncryptedMessage)
  def toAttachmentJson: Either[String, Attachment] =
    msg match
      case sMsg: SignedMessage    => sMsg.toJsonAST.map(json => Attachment(data = AttachmentDataJson(json = json)))
      case eMsg: EncryptedMessage => eMsg.toJsonAST.map(json => Attachment(data = AttachmentDataJson(json = json)))

/** The Forward Message is sent by the sender to the mediator to forward (the data) to a recipient.
  *
  * {{{
  * {
  *   "type": "https://didcomm.org/routing/2.0/forward",
  *   "id": "abc123xyz456",
  *   "to": ["did:example:mediator"],
  *   "expires_time": 1516385931,
  *   "body":{ "next": "did:foo:1234abcd"},
  *   "attachments": [] //The payload(s) to be forwarded
  * }
  * }}}
  *
  * @param lang
  *   See [https://identity.foundation/didcomm-messaging/spec/#routing-protocol-20]
  */

sealed trait ForwardMessage {
  def id: MsgID
  def to: Set[TO]
  def from: Option[FROM]
  def next: DIDSubject
  def expires_time: NotRequired[UTCEpoch]
  def msg: SignedMessage | EncryptedMessage

  // methods
  def `type` = ForwardMessage.piuri

  def toAttachments: Attachment

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = `type`,
      to = Some(to),
      body = Some(ForwardMessage.Body(next).toJSON_RFC7159),
      expires_time = expires_time,
      attachments = Some(Seq(toAttachments)),
    )
}

final case class ForwardMessageBase64(
    id: MsgID = MsgID(),
    to: Set[TO] = Set.empty,
    from: Option[FROM],
    next: DIDSubject, // TODO is this on the type TO?
    expires_time: NotRequired[UTCEpoch] = None,
    msg: SignedMessage | EncryptedMessage,
) extends ForwardMessage {
  def toAttachments: Attachment = Attachment(
    data = msg match
      case sMsg: SignedMessage    => AttachmentDataBase64(Base64.encode(sMsg.toJson))
      case eMsg: EncryptedMessage => AttachmentDataBase64(Base64.encode(eMsg.toJson))
  )

}

final case class ForwardMessageJson(
    id: MsgID = MsgID(),
    to: Set[TO] = Set.empty,
    from: Option[FROM],
    next: DIDSubject, // TODO is this on the type TO? //IMPROVE next MUST? be one o recipients
    expires_time: NotRequired[UTCEpoch] = None,
    msg: SignedMessage | EncryptedMessage,
) extends ForwardMessage {
  def toAttachments: Attachment = Attachment(
    /** toJSON_RFC7159 MUST not fail! */
    data = msg match
      case sMsg: SignedMessage    => AttachmentDataJson(sMsg.toJsonAST.getOrElse(JSON_RFC7159()))
      case eMsg: EncryptedMessage => AttachmentDataJson(eMsg.toJsonAST.getOrElse(JSON_RFC7159()))
  )
}

object ForwardMessage {
  def piuri = PIURI("https://didcomm.org/routing/2.0/forward")

  protected final case class Body(next: DIDSubject) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, ForwardMessage] = {
    if (msg.`type` != piuri) Left(s"No able to create ForwardMessage from a Message of the type '${msg.`type`}'")
    else {
      msg.body match
        case None    => Left(s"'$piuri' MUST have field 'body'")
        case Some(b) =>
          b.as[Body]
            .left
            .map(error => s"'$piuri' fail to parse body due to: $error")
            .flatMap { body =>
              msg.attachments match
                case None =>
                  Left(s"'$piuri' MUST have Attachments (with one attachment that include the message to foward)")
                case Some(Seq()) => Left(s"'$piuri' MUST have one Attachment (with the message to foward)")
                case Some(firstAttachment +: Seq()) =>
                  firstAttachment.data match {
                    case AttachmentDataJWS(jws, links) =>
                      Left(s"'$piuri' MUST of the Attachment type Base64 or Json (instead of JWT)")
                    case AttachmentDataLinks(links, hash) =>
                      Left(s"'$piuri' MUST of the Attachment type Base64 or Json (instead of Link)")
                    case AttachmentDataBase64(base64) =>
                      base64.decodeToString.fromJson[Message] match
                        case Left(error) =>
                          Left(
                            s"'$piuri' fail to parse the attachment (base64) as an EncryptedMessage or SignedMessage due to: $error"
                          )
                        case Right(nextMsg: PlaintextMessage) =>
                          Left(
                            s"'$piuri' fail to parse the attachment (base64) because the next Message is a PlaintextMessage: ${nextMsg.toJson}"
                          )
                        case Right(nextMsg: EncryptedMessage) =>
                          Right(
                            ForwardMessageJson(
                              id = msg.id,
                              to = msg.to.getOrElse(Set.empty),
                              from = msg.from,
                              next = body.next,
                              expires_time = msg.expires_time,
                              msg = nextMsg,
                            )
                          )
                        case Right(nextMsg: SignedMessage) =>
                          Right(
                            ForwardMessageBase64(
                              id = msg.id,
                              to = msg.to.getOrElse(Set.empty),
                              from = msg.from,
                              next = body.next,
                              expires_time = msg.expires_time,
                              msg = nextMsg,
                            )
                          )
                    case AttachmentDataJson(json) =>
                      json.as[Message] match
                        case Left(error) =>
                          Left(
                            s"'$piuri' fail to parse the attachment (json) as an EncryptedMessage or SignedMessage due to: $error"
                          )
                        case Right(nextMsg: PlaintextMessage) =>
                          Left(
                            s"'$piuri' fail to parse the attachment (json) because the next Message is a PlaintextMessage: ${nextMsg.toJson}"
                          )
                        case Right(nextMsg: EncryptedMessage) =>
                          Right(
                            ForwardMessageJson(
                              id = msg.id,
                              to = msg.to.getOrElse(Set.empty),
                              from = msg.from,
                              next = body.next,
                              expires_time = msg.expires_time,
                              msg = nextMsg,
                            )
                          )
                        case Right(nextMsg: SignedMessage) =>
                          Right(
                            ForwardMessageJson(
                              id = msg.id,
                              to = msg.to.getOrElse(Set.empty),
                              from = msg.from,
                              next = body.next,
                              expires_time = msg.expires_time,
                              msg = nextMsg,
                            )
                          )
                    case AttachmentDataAny(jws, hash, links, base64, json) =>
                      Left(s"'$piuri' has attachments of unknown type") // TODO shound we still try?
                  }
                case Some(firstAttachments +: tail) =>
                  Left(s"'$piuri' MUST have only one attachment (instead of multi attachment)")
                case Some(value) => // IMPOSIBLE
                  Left(
                    s"ERROR: '$piuri' fail to parse Attachment - This case SHOULD be IMPOSIBLE. value='$value"
                  )
            }

    }
  }

  def buildForwardMessage(
      id: MsgID = MsgID(),
      to: Set[TO] = Set.empty,
      next: DIDSubject,
      msg: SignedMessage | EncryptedMessage,
  ) = {
    val recipients = msg match
      case sMsg: SignedMessage =>
        sMsg.payloadAsPlaintextMessage.map(_.to.toSet.flatten.map(_.toDIDSubject)).getOrElse(Set.empty)
      case eMsg: EncryptedMessage => eMsg.recipientsSubject
    if (!recipients.contains(next))
      Left("'next' shound be one of the recipients")
    else
      Right(
        ForwardMessageJson(
          id = id,
          to = to,
          from = None,
          next = next,
          msg = msg,
        )
      )
  }

  // TODO make a test (but need a implementation )
  def makeForwardMessage(
      to: TO, // Mediator
      next: DIDSubject,
      msg: SignedMessage | EncryptedMessage
  ): ZIO[Operations & Resolver, DidFail, EncryptedMessage] =
    buildForwardMessage(next = next, msg = msg, to = Set(to)) match
      case Left(error1)          => ZIO.fail(FailToEncodeMessage(piuri, error1))
      case Right(forwardMessage) =>
        for {
          ops <- ZIO.service[Operations]
          encryptedMessage <- ops.anonEncrypt(forwardMessage.toPlaintextMessage)
        } yield encryptedMessage

}
