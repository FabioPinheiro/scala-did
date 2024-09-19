package fmgp.did.comm

import zio._
import zio.json._
import zio.stream._
import zio.http._
import zio.http.Header.{AccessControlAllowOrigin, AccessControlAllowMethods}

import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.framework._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.method.peer.DIDPeer.AgentDIDPeer
import fmgp.did.demo.AppConfig
import fmgp.util._

object DIDCommRoutes {

  def appRoutes: Routes[Operator & Operations & Resolver, Nothing] = Routes(
    Method.GET / "pipe" -> handler { (req: Request) =>
      for {
        _ <- ZIO.log("Stream Pipe started")
        operator <- ZIO.service[Operator]
        ws <- StreamFromWebSocket.zioWebSocketApp(operator.pipeline)
        ret <- ws.toResponse
      } yield ret
    },
    Method.GET / "ws" -> handler { (req: Request) =>
      for {
        annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
        webSocketApp = TransportWSImp.createWebSocketAppWithOperator(annotationMap)
        ret <- webSocketApp.toResponse
      } yield (ret)
    },
    Method.POST / trailing -> handler { (req: Request) =>
      val SignedTyp = MediaTypes.SIGNED.typ
      val EncryptedTyp = MediaTypes.ENCRYPTED.typ
      req.header(Header.ContentType).map(_.mediaType.fullType) match
        case Some(`SignedTyp`) | Some(`EncryptedTyp`) =>
          (for {
            data <- req.body.asString
            msg <- data.fromJson[Message] match
              case Left(value) => ZIO.fail(Response.badRequest(s"Fail to parse DID Comm Message because: $value"))
              case Right(pMsg: PlaintextMessage) => ZIO.fail(Response.badRequest("Message must not be in Plaintext"))
              case Right(sMsg: SignedMessage)    => ZIO.succeed(sMsg)
              case Right(eMsg: EncryptedMessage) => ZIO.succeed(eMsg)
            inboundQueue <- Queue.bounded[SignedMessage | EncryptedMessage](1)
            outboundQueue <- Queue.bounded[SignedMessage | EncryptedMessage](1)
            transport = new TransportDIDComm[Any] {
              def transmissionFlow = Transport.TransmissionFlow.BothWays
              def transmissionType = Transport.TransmissionType.SingleTransmission
              def id: TransportID = TransportID.http(req.headers.get("request_id"))
              def inbound: ZStream[Any, Transport.InErr, SignedMessage | EncryptedMessage] =
                ZStream.fromQueue(inboundQueue)
              def outbound: ZSink[Any, Transport.OutErr, SignedMessage | EncryptedMessage, Nothing, Unit] =
                ZSink.fromQueue(outboundQueue)
            }
            operator <- ZIO.service[Operator]
            fiber <- operator.receiveTransport(transport).fork
            _ <- inboundQueue.offer(msg)
            ret <- outboundQueue.take
              .timeout(AppConfig.timeout)
              .tap(e => ZIO.logDebug("Request Timeout").when(e.isEmpty))
              .map {
                case None => Response.status(Status.Accepted)
                case Some(msg: SignedMessage) =>
                  Response(Status.Ok, Headers(MediaTypes.SIGNED.asContentType), Body.fromCharSequence(msg.toJson))
                case Some(msg: EncryptedMessage) =>
                  Response(Status.Ok, Headers(MediaTypes.ENCRYPTED.asContentType), Body.fromCharSequence(msg.toJson))
              }
            _ <- fiber.join
            shutdown <- inboundQueue.shutdown <&> outboundQueue.shutdown
          } yield ret)
            .tapErrorCause(ZIO.logErrorCause("Error", _))
            .catchAllCause(cause => ZIO.succeed(Response.fromCause(cause)))
        case Some(_) | None => ZIO.succeed(Response.badRequest(s"The content-type must be $SignedTyp or $EncryptedTyp"))

    },
  )
}
