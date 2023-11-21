package fmgp.did.framework

import zio._
import zio.json._
import zio.stream._
import fmgp.did._
import fmgp.did.comm._

class TransportDIDCommWS[R](transport: TransportWS[R, String]) extends TransportDIDComm[R] {

  /** Send to the other side. Out going Messages */
  def outbound: ZSink[R, Transport.OutErr, SignedMessage | EncryptedMessage, Nothing, Unit] =
    transport.outbound.contramap((_: Message).toJson)

  /** Reciving from the other side. Income Messages */
  def inbound: ZStream[R, Transport.InErr, SignedMessage | EncryptedMessage] =
    transport.inbound.map(_.fromJson[Message]).collect {
      case Right(sMsg: SignedMessage)    => sMsg
      case Right(eMsg: EncryptedMessage) => eMsg
    }

  def id: TransportID = transport.id

  def close = transport.ws.close
}
