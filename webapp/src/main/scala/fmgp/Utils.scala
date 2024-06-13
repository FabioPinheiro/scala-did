package fmgp

import zio._
import zio.json._
import fmgp.did._
import fmgp.util._
import fmgp.did.comm._
import fmgp.did.framework._
import fmgp.crypto.error._
import fmgp.webapp.ResolverTool
import fmgp.webapp.Global

object Utils {

  def runProgram[E](program: ZIO[Any, E, Unit]) = Unsafe.unsafe { implicit unsafe => // Run side effect
    Runtime.default.unsafe.fork(
      program
        .catchAllCause(cause => ZIO.logErrorCause("runProgram Fail", cause))
        .catchAllDefect(error => ZIO.logError("runProgram with Defect: " + error.getMessage()))
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

  def curlProgram(msg: EncryptedMessage): ZIO[Resolver, DidFail, Option[String]] = for {
    resolver <- ZIO.service[Resolver]
    doc <- resolver
      .didDocument(TO(msg.recipientsSubject.head.string))
      .mapError(ResolverErrorWarp(_))
    didCommMessagingServices = doc.getDIDServiceDIDCommMessaging
    mURI = didCommMessagingServices.flatMap(_.endpoints.map(e => e.uri)).headOption
    call <- mURI match
      case None      => ZIO.fail(FailToParse("No URI in Services 'DIDCommMessaging'"))
      case Some(uri) => Client.makeDIDCommPost(msg, uri)
  } yield (call)

  def sendAndReceiveProgram(msg: PlaintextMessage) =
    for {
      tmp <- Utils.programEncryptMessage(msg) // encrypt
      pMsg = tmp._1
      eMsg = tmp._2
      maybeResponseData <- Utils.curlProgram(eMsg)
      responseMsg <- maybeResponseData match
        case None => ZIO.fail("Response not text Data in body")
        case Some(data) =>
          data.fromJson[EncryptedMessage] match {
            case Left(value)        => ZIO.fail(FailToParse(value))
            case Right(responseMsg) => ZIO.succeed(responseMsg)
          }
      response <- OperationsClientRPC.decrypt(responseMsg).flatMap {
        case value: EncryptedMessage => ZIO.fail(FailDecryptDoubleEncrypted(responseMsg, value))
        case plaintext: PlaintextMessage =>
          ZIO.succeed(Global.messageRecive(eMsg, plaintext)) // side effect!
            *> ZIO.succeed(plaintext)
        case sMsg @ SignedMessage(payload, signatures) =>
          payload.content.fromJson[Message] match
            case Left(value) => ZIO.fail(FailToParse(value))
            case Right(plaintext: PlaintextMessage) =>
              ZIO.succeed(Global.messageRecive(eMsg, plaintext)) // side effect!
                *> ZIO.succeed(plaintext)
            case Right(value: SignedMessage)    => ZIO.fail(FailDecryptDoubleSign(sMsg, value))
            case Right(value: EncryptedMessage) => ZIO.fail(FailDecryptSignThenEncrypted(sMsg, value))
      }
    } yield response

  def decryptProgram(eMsg: EncryptedMessage): ZIO[Agent & Resolver, DidFail, (EncryptedMessage, PlaintextMessage)] =
    OperationsClientRPC
      .decryptRaw(eMsg)
      .flatMap { data => Operations.parseMessage(data).map((eMsg, _)) }
      .flatMap {
        case (eMsg: EncryptedMessage, plaintext: PlaintextMessage) =>
          ZIO.succeed((eMsg, plaintext))
        case (eMsg: EncryptedMessage, sMsg: SignedMessage) =>
          verifyProgram(sMsg: SignedMessage).map(e => (eMsg, e._2))
        case (outsideMsg: EncryptedMessage, insideMsg: EncryptedMessage) =>
          ZIO.fail(FailDecryptDoubleEncrypted(outsideMsg, insideMsg))
      }

  def verifyProgram(sMsg: SignedMessage): ZIO[Resolver, CryptoFailed, (SignedMessage, PlaintextMessage)] =
    OperationsClientRPC
      .verify2PlaintextMessage(sMsg)
      .map { pMsg => (sMsg, pMsg) }

  def openWsProgram(wsUrl: String, timeout: Int = 10): ZIO[Any, Nothing, TransportDIDCommWS[Any]] =
    for {
      _ <- ZIO.logDebug(s"openWsProgram to $wsUrl for $timeout")
      transport <- TransportWSImp.make(wsUrl = wsUrl)
      transportWarp = TransportDIDCommWS(transport)
      closeFiber <- transportWarp.close.delay(duration = timeout.second).debug.fork
      _ <- ZIO.logDebug(s"The ws ${transportWarp.id} is open for more $timeout seconds")
    } yield transportWarp

}
