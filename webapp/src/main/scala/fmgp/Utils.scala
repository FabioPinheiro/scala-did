package fmgp

import zio._
import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.webapp.ResolverTool

object Utils {

  def runProgram(program: ZIO[Any, DidFail, Unit]) = Unsafe.unsafe { implicit unsafe => // Run side efect
    Runtime.default.unsafe.fork(
      program.catchAll { case error =>
        ZIO.succeed(println(error))
      }
    )
  }

  def programEncryptMessage(pMsg: PlaintextMessage): ZIO[
    Agent & Resolver,
    DidFail, // Nothing,
    (PlaintextMessage, EncryptedMessage)
  ] = OperationsClientRPC
    .encrypt(pMsg) // always use the message data (FROM/TO) to Encrypt
    .either
    .map(_.map((pMsg, _)))
    .flatMap(ZIO.fromEither)

  def curlProgram(msg: EncryptedMessage): ZIO[Resolver, DidFail, String] = for {
    resolver <- ZIO.service[Resolver]
    doc <- resolver.didDocument(TO(msg.recipientsSubject.head.string))
    didCommMessagingServices = doc.getDIDServiceDIDCommMessaging
    mURI = didCommMessagingServices.flatMap(_.endpoints.map(e => e.uri)).headOption
    call <- mURI match
      case None      => ZIO.fail(FailToParse("No URI in Services 'DIDCommMessaging'"))
      case Some(uri) => Client.makeDIDCommPost(msg, uri)
  } yield (call)

}
