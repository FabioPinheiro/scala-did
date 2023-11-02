package fmgp.util

import scala.util.chaining._
import zio._
import zio.json._
import zio.stream._
import zio.http._
import fmgp.did._
import fmgp.crypto.error._
import fmgp.did.comm._

class TransportDIDCommOverHTTP(
    destination: String,
    inboundBuf: Hub[EncryptedMessage],
) extends Transport[Client & Scope, EncryptedMessage] {
  def id: String = Transport.nextTransportCounter
  def inbound: ZStream[Client & Scope, Transport.InErr, EncryptedMessage] = ZStream.fromHub(inboundBuf)
  def outbound: zio.stream.ZSink[Client & Scope, Transport.OutErr, EncryptedMessage, Nothing, Unit] =
    ZSink.foreach { (msg: EncryptedMessage) =>
      val contentTypeHeader = msg.`protected`.obj.typ
        .getOrElse(MediaTypes.ENCRYPTED)
        .pipe(e => Header.ContentType(MediaType(e.mainType, e.subType)))
      for {
        res <- Client
          .request(
            Request
              .post(path = destination, body = Body.fromCharSequence(msg.toJson))
              .setHeaders(Headers(Seq(contentTypeHeader)))
          )
          .tapError(ex => ZIO.logWarning(s"Fail when calling '$destination': ${ex.toString}"))
          .orDie
        data <- res.body.asString
          .tapError(ex => ZIO.logError(s"Fail parce http response body: ${ex.getMessage}"))
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
  ): ZIO[Any, Nothing, Transport[Client & Scope, EncryptedMessage]] = for {
    inbound <- Hub.bounded[EncryptedMessage](boundSize)
  } yield TransportDIDCommOverHTTP(destination, inbound)
}
