package fmgp.did.framework

import scala.util.chaining.*
import zio.*
import zio.json.*
import zio.stream.*
import zio.http.*
import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*

/** Client-side HTTP transport for DIDComm messaging.
  *
  * Implements the Transport abstraction over HTTP. Outbound and inbound are independent, unrelated message flows — they
  * are NOT coupled as HTTP request-response pairs.
  *
  * ==Features==
  *
  *   - R1 — Outbound delivery: POST the DIDComm message as JSON to `destination` with the correct Content-Type header
  *     (application/didcomm-signed+json or application/didcomm-encrypted+json).
  *   - R2 — Inbound via Hub (broadcast): `inbound` supports multiple concurrent subscribers (pub-sub / broadcast
  *     semantics via Hub). Every subscriber receives every message published after their subscription.
  *   - R3 — No late subscriber guarantee: Hub does not replay past messages. Subscribers that connect after a message
  *     was published will not receive it. This is by design.
  *   - R4 — HTTP response parsing: After each outbound POST, the HTTP response body is parsed. If it contains a valid
  *     DIDComm message (SignedMessage or EncryptedMessage), it is published to the inbound Hub. If the body is empty,
  *     not valid JSON, or not a DIDComm message, it is logged and ignored.
  *   - R5 — TransportID: Uses `TransportID.http`.
  *   - R6 — Error handling: HTTP errors (non-2xx status) are logged but not delivered to `inbound`.
  *     Network/connection failures are logged and the effect dies (orDie).
  *   - R7 — SingleTransmission semantics: TransmissionType is SingleTransmission, unlike WebSocket's
  *     MultiTransmissions.
  *   - R8 — Environment: Requires `Client & Scope` from zio-http. The companion object provides `makeWithEnvironment`
  *     to pre-inject the environment and return a `TransportDIDComm[Any]`.
  */
class TransportDIDCommOverHTTP(
    destination: String,
    inboundBuf: Hub[SignedMessage | EncryptedMessage],
) extends TransportDIDComm[Client & Scope] {

  def transmissionFlow = Transport.TransmissionFlow.BothWays
  def transmissionType = Transport.TransmissionType.SingleTransmission

  def id: String = TransportID.http
  def inbound: ZStream[Client & Scope, Transport.InErr, SignedMessage | EncryptedMessage] = ZStream.fromHub(inboundBuf)
  def outbound: zio.stream.ZSink[Client & Scope, Transport.OutErr, SignedMessage | EncryptedMessage, Nothing, Unit] =
    ZSink.foreach { (msg: SignedMessage | EncryptedMessage) =>
      val contentTypeHeader = msg match
        case _: SignedMessage    => MediaTypes.SIGNED.pipe(e => Header.ContentType(MediaType(e.mainType, e.subType)))
        case _: EncryptedMessage => MediaTypes.ENCRYPTED.pipe(e => Header.ContentType(MediaType(e.mainType, e.subType)))
      for {
        res <- Client
          .batched( // Shound we use .batched() or .streaming()testAll
            Request
              .post(path = destination, body = Body.fromCharSequence((msg: Message).toJson))
              .setHeaders(Headers(Seq(contentTypeHeader)))
          )
          .tapError(ex => ZIO.logWarning(s"Fail when calling '$destination': ${ex.toString}"))
          .orDie
        data <- res.body.asString
          .tapError(ex => ZIO.logError(s"Fail parse http response body: ${ex.getMessage}"))
          .orDie
        _ <- res.status.isError match
          case true  => ZIO.logError(data)
          case false => ZIO.logDebug(data)
        _ <- parseResponse(data).flatMap {
          case Some(msg) => publishInbound(msg)
          case None      => ZIO.unit
        }
      } yield ()
    }

  private def parseResponse(data: String): UIO[Option[SignedMessage | EncryptedMessage]] =
    if (data.isBlank) ZIO.succeed(None)
    else
      data.fromJson[SignedOrEncryptedMessage](using SignedOrEncryptedMessage.decoder) match
        case Left(error) =>
          ZIO.logDebug(s"HTTP response is not a DIDComm message: $error") *> ZIO.succeed(None)
        case Right(msg) => ZIO.succeed(Some(msg))

  private def publishInbound(msg: SignedMessage | EncryptedMessage): UIO[Unit] =
    inboundBuf.publish(msg).unit
}

object TransportDIDCommOverHTTP {

  def make(
      destination: String,
      boundSize: Int = 3,
  ): ZIO[Any, Nothing, TransportDIDComm[Client & Scope]] = for {
    inbound <- Hub.bounded[SignedMessage | EncryptedMessage](boundSize)
  } yield TransportDIDCommOverHTTP(destination, inbound)

  def makeWithEnvironment(
      destination: String,
      boundSize: Int = 3,
      env: ZEnvironment[Client & Scope]
  ): ZIO[Any, Nothing, TransportDIDComm[Any]] = for {
    inbound <- Hub.bounded[SignedMessage | EncryptedMessage](boundSize)
  } yield TransportDIDCommOverHTTP(destination, inbound).provide(env)
}
