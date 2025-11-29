package fmgp.did.comm.protocol

import zio.*
import zio.json.*

import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.Operations.*
import fmgp.did.comm.protocol.*
import fmgp.did.comm.protocol.basicmessage2.*
import fmgp.did.comm.protocol.trustping2.*

//TODO pick a better name // maybe "Protocol" only

trait ProtocolExecuter[-R, +E] { self =>

  def supportedPIURI: Seq[PIURI]

  def program(plaintextMessage: PlaintextMessage): ZIO[R, E, Action]

  final def mapError[E2](f: E => E2): ProtocolExecuter[R, E2] =
    flatMapError((e: E) => ZIO.succeed(f(e)))

  final def flatMapError[R1 <: R, E2](f: E => URIO[R1, E2]): ProtocolExecuter[R1, E2] =
    ProtocolExecuter.FlatMapFailure(self, f)

}

object ProtocolExecuter {
  type Services = Resolver // & Agent & Operations

  private[ProtocolExecuter] final case class FlatMapFailure[R, E1, E2](
      first: ProtocolExecuter[R, E1],
      fFlatMapError: E1 => ZIO[R, Nothing, E2],
  ) extends ProtocolExecuter[R, E2] {
    def supportedPIURI: Seq[fmgp.did.comm.PIURI] = first.supportedPIURI
    def program(plaintextMessage: fmgp.did.comm.PlaintextMessage): ZIO[R, E2, Action] =
      first.program(plaintextMessage).flatMapError(fFlatMapError)
  }
}

case class ProtocolExecuterCollection[-R, +E](
    executers: ProtocolExecuter[R, E]*
)(fallback: ProtocolExecuter[R, E])
    extends ProtocolExecuter[R, E] {

  override def supportedPIURI: Seq[PIURI] = executers.flatMap(_.supportedPIURI)

  def selectExecutersFor(piuri: PIURI) = executers.find(_.supportedPIURI.contains(piuri))

  override def program(plaintextMessage: PlaintextMessage): ZIO[R, E, Action] =
    selectExecutersFor(plaintextMessage.`type`) match
      case None     => fallback.program(plaintextMessage)
      case Some(px) => px.program(plaintextMessage)
}

trait ProtocolExecuterWithServices[-R <: ProtocolExecuter.Services, +E] extends ProtocolExecuter[R, E] {

  /* TODO FIXME REMOVE
  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, DidFail, Option[EncryptedMessage]] =
    program(plaintextMessage)
      .tap(v => ZIO.logDebug(v.toString)) // DEBUG
      .flatMap {
        case _: NoReply.type => ZIO.succeed(None)
        case action: AnyReply =>
          val reply = action.msg
          for {
            msg <- reply.from match
              case Some(value) => authEncrypt(reply)
              case None        => anonEncrypt(reply)
            // TODO forward message
            maybeSyncReplyMsg <- reply.to.map(_.toSeq) match
              case None        => ZIO.logWarning("Have a reply but the field 'to' is missing") *> ZIO.none
              case Some(Seq()) => ZIO.logWarning("Have a reply but the field 'to' is empty") *> ZIO.none
              case Some(send2DIDs) =>
                ZIO
                  .foreach(send2DIDs)(to =>
                    val job = for {
                      messageDispatcher <- ZIO.service[MessageDispatcher]
                      resolver <- ZIO.service[Resolver]
                      doc <- resolver.didDocument(to)
                      services = {
                        doc.service.toSeq.flatten
                          .collect { case service: DIDServiceDIDCommMessaging =>
                            service
                          }
                      }
                      mURL = services.flatMap(_.endpoints.map(_.uri)).headOption // TODO head
                      jobToRun <- mURL match
                        case None => ZIO.logWarning(s"No url to send message")
                        case Some(url) =>
                          ZIO.log(s"Send to url: $url") *>
                            messageDispatcher.send(msg, url)
                    } yield (jobToRun)
                    action match
                      case Reply(_)          => job
                      case SyncReplyOnly(_)  => ZIO.unit
                      case AsyncReplyOnly(_) => job
                  ) *> ZIO
                  .succeed(msg)
                  .when(
                    {
                      plaintextMessage.return_route.contains(ReturnRoute.all)
                      && {
                        plaintextMessage.from.map(_.asTO) match {
                          case None          => false
                          case Some(replyTo) => send2DIDs.contains(replyTo)
                        }
                      }
                    } || action.isInstanceOf[SyncReplyOnly]
                  )
          } yield maybeSyncReplyMsg
      }
   */

  // override def program(plaintextMessage: PlaintextMessage): ZIO[R, E, Action]
}

object NullProtocolExecute extends ProtocolExecuter[Any, MissingProtocol] {

  override def supportedPIURI = Seq()
  override def program(plaintextMessage: PlaintextMessage) =
    ZIO.fail(MissingProtocol(plaintextMessage.`type`))
}

object BasicMessageExecuter extends ProtocolExecuter[Any, FailToParse] {

  override def supportedPIURI: Seq[PIURI] = Seq(BasicMessage.piuri)
  override def program(plaintextMessage: PlaintextMessage) = for {
    job <- BasicMessage.fromPlaintextMessage(plaintextMessage) match
      case Left(error) => ZIO.fail(FailToParse(error))
      case Right(bm)   => ZIO.log(s"BasicMessage: ${bm.toString}")
  } yield NoReply
}

class TrustPingExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services, FailToParse] {

  override def supportedPIURI: Seq[PIURI] = Seq(TrustPing.piuri, TrustPingResponse.piuri)

  override def program(plaintextMessage: PlaintextMessage): ZIO[ProtocolExecuter.Services, FailToParse, Action] = {
    // the val is from the match to be definitely stable
    val piuriTrustPing = TrustPing.piuri
    val piuriTrustPingResponse = TrustPingResponse.piuri

    plaintextMessage.`type` match
      case `piuriTrustPing` =>
        TrustPing.fromPlaintextMessage(plaintextMessage) match
          case Left(error)                                    => ZIO.fail(FailToParse(error))
          case Right(ping: TrustPingWithOutRequestedResponse) => ZIO.logInfo(ping.toString()) *> ZIO.succeed(NoReply)
          case Right(ping: TrustPingWithRequestedResponse)    =>
            for {
              _ <- ZIO.logInfo(ping.toString())
              ret = ping.makeRespond
            } yield Reply(ret.toPlaintextMessage)
      case `piuriTrustPingResponse` =>
        for {
          job <- TrustPingResponse.fromPlaintextMessage(plaintextMessage) match
            case Left(error) => ZIO.fail(FailToParse(error))
            case Right(ping) => ZIO.logInfo(ping.toString())
        } yield NoReply
  }

}
