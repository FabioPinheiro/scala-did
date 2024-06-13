package fmgp.did.framework

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error.DidFail
import fmgp.did.comm.protocol.routing2.ForwardMessage.makeForwardMessage
import fmgp.crypto.error.ResolverErrorWarp

trait TransportFactory {
  def openTransport(uri: String): UIO[TransportDIDComm[Any]]
}

trait TransportDispatcher extends TransportFactory {
  def send(
      to: TO,
      msg: SignedMessage | EncryptedMessage,
      thid: Option[MsgID],
      pthid: Option[MsgID]
  ): ZIO[Resolver & Agent & Operations, DidFail, Unit]

  def sendViaDIDCommMessagingService(
      to: TO,
      msg: SignedMessage | EncryptedMessage
  ): ZIO[Resolver & Agent & Operations, DidFail, Either[String, TransportDIDComm[Any]]] =
    for {
      resolver <- ZIO.service[Resolver]
      doc <- resolver.didDocument(to).mapError(ResolverErrorWarp(_))
      services = {
        doc.service.toSeq.flatten
          .collect { case service: DIDServiceDIDCommMessaging => service }
      }
      mURI = services.flatMap(_.endpoints.map(_.uri)).headOption // TODO head !!!!!!!!!!
      transportOrError <- mURI match
        case None => ZIO.logWarning(s"No url to send message") *> ZIO.succeed(Left("No url to send message"))
        case Some(did) if did.startsWith("did:") => // make it more type safe
          val mediator = DIDSubject(did).asTO
          for {
            forwardMessage <- makeForwardMessage(to = mediator, next = to.toDIDSubject, msg)
            ret <- sendViaDIDCommMessagingService(mediator, forwardMessage)
          } yield ret
        case Some(uri) =>
          for {
            _ <- ZIO.log(s"Send to uri: $uri")
            transport <- openTransport(uri)
            _ <- transport.send(msg)
          } yield Right(transport)
    } yield transportOrError
}
