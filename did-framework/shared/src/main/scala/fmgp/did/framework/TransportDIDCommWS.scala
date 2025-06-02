package fmgp.did.framework

import zio._
import zio.json._
import zio.stream._
import fmgp.did._
import fmgp.did.comm._

class TransportDIDCommWS[R](val transport: TransportWS[R, String]) extends TransportDIDComm[R] {

  def transmissionFlow = Transport.TransmissionFlow.BothWays
  def transmissionType = Transport.TransmissionType.MultiTransmissions

  /** Send to the other side. Out going Messages */
  def outbound: ZSink[R, Transport.OutErr, SignedMessage | EncryptedMessage, Nothing, Unit] =
    transport.outbound.contramap((_: Message).toJson)

  /** Reciving from the other side. Income Messages */
  def inbound: ZStream[R, Transport.InErr, SignedMessage | EncryptedMessage] =
    transport.inbound
      .map(_.fromJson[Message])
      .tap {
        // TODO use logDebug
        case Left(error) => ZIO.log(s"Fail to parse message in WS into SignedMessage | EncryptedMessage")
        case Right(value: PlaintextMessage) => ZIO.log(s"Got a PlaintextMessage in WS (ignored)")
        case Right(value)                   => ZIO.unit
      }
      .collect {
        case Right(sMsg: SignedMessage)    => sMsg
        case Right(eMsg: EncryptedMessage) => eMsg
      }

  def id: TransportID = transport.id

  def close = transport.ws.close
}
