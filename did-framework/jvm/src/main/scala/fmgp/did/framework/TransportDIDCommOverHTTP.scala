package fmgp.did.framework

import scala.util.chaining.*
import zio.*
import zio.json.*
import zio.stream.*
import zio.http.*
import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*

class TransportDIDCommOverHTTP(
    destination: String,
    inboundBuf: Hub[SignedMessage | EncryptedMessage],
) extends TransportDIDComm[Client & Scope] {

  def transmissionFlow = Transport.TransmissionFlow.BothWays
  def transmissionType = Transport.TransmissionType.SingleTransmission

  def id: String = TransportID.ws
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
          case false => ZIO.logInfo(data)
      } yield (data)
    }
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
