package fmgp.did.framework

import zio._
import zio.json._
import zio.stream._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._

trait Pipeline[R] {
  def pipe: ZPipeline[R, RuntimeException, String, String]
}

class OperatorPipeline[R](val agents: Map[DIDSubject, Pipeline[R]]) {
  def pipe: ZPipeline[R, RuntimeException | String, String, String] =
    ZPipeline.branchAfter[R, RuntimeException | String, String, String](1) { chunk =>
      ZPipeline.fromChannel(
        ZChannel.fromZIO {
          for {
            msg <- Pipeline.parseMsg(chunk.asString)
            dids = msg match
              case sMsg: SignedMessage    => sMsg.publicRecipientsSubject
              case eMsg: EncryptedMessage => eMsg.publicRecipientsSubject
            fristAgentPipe = dids.flatMap { did => agents.get(did) }.headOption // TODO headOption
            nextPipe = fristAgentPipe match
              case None => ZChannel.fail("")
              case Some(value) =>
                ZPipeline
                  .prepend(chunk)
                  // .map(s => chunk.asString + ": " + s)
                  .channel
          } yield nextPipe
        }.flatten
      )
    }
}

object Pipeline {

  def parseMsg(data: String): ZIO[Any, String, SignedMessage | EncryptedMessage] = data.fromJson[Message] match
    case Left(value)                   => ZIO.fail(s"Fail to parse DID Comm Message because: $value")
    case Right(pMsg: PlaintextMessage) => ZIO.fail("Message must not be in Plaintext")
    case Right(sMsg: SignedMessage)    => ZIO.succeed(sMsg)
    case Right(eMsg: EncryptedMessage) => ZIO.succeed(eMsg)

  def parseMsgPipeline = ZPipeline.mapZIO[Any, String, String, SignedOrEncryptedMessage](parseMsg)

  def encodeMsgPipeline = ZPipeline.map[SignedOrEncryptedMessage, String] { msg =>
    msg.toJson(SignedOrEncryptedMessage.encoder)
  }

  def pipelineDIDComm[R, E](
      pipe: ZPipeline[R, E, SignedOrEncryptedMessage, SignedOrEncryptedMessage]
  ): ZPipeline[R, E | String, String, String] = parseMsgPipeline >>> pipe >>> encodeMsgPipeline

  def test: ZPipeline[Operations & Resolver, RuntimeException, String, String] =
    ZPipeline.branchAfter[Operations & Resolver, RuntimeException, String, String](1) { chunk =>
      ZPipeline.fromChannel(
        ZChannel
          .fromZIO(
            ZIO
              .succeed(
                ZPipeline.prepend(chunk)
                  >>> ZPipeline
                    .identity[String]
                    .tap {
                      case "exit" => ZIO.fail(new RuntimeException("exit!"))
                      case _      => ZIO.unit
                    }
              )
              .map(_.map(s => chunk.asString + ": " + s).channel)
          )
          .flatten
      )
    }

  def experiment = ZPipeline.fromFunction[Any, Nothing, String, String] { stream =>
    def scope: Scope = ???
    val stream: ZStream[Any, Nothing, String] = ZStream
      .repeatWithSchedule(
        java.time.format.DateTimeFormatter.ISO_LOCAL_TIME.format(java.time.LocalDateTime.now),
        Schedule.spaced(15.second)
      )
    val tmp: ZIO[Any, Nothing, ZStream[Any, Nothing, String]] = stream
      .broadcast(2, 1)
      .provideEnvironment(ZEnvironment(scope))
      .flatMap { streams =>
        streams(0).runHead.map {
          case None => ZStream.empty
          case Some(head) =>
            head.toIntOption match {
              case None        => streams(1).map("N:" ++ _)
              case Some(value) => streams(1).map("Y:" ++ _)
            }
        }
      }
    ZStream.fromZIO(tmp).flatten
  }
  // >>> parseMsg
  // >>> encodeMsg
}
