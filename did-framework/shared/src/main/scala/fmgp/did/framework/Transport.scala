package fmgp.did.framework

import zio.*
import zio.stream.*
import fmgp.did.*
import fmgp.did.comm.{SignedMessage, EncryptedMessage}

type TransportDIDComm[R] = Transport[R, SignedMessage | EncryptedMessage, SignedMessage | EncryptedMessage]

//TODO maybe remove the R ? Do we really need it?
/** The goal is to make this DID Comm library Transport-agnostic */
trait Transport[R, IN, OUT] { self =>

  /** Describe properties about the Flow (direction) of data for this transport */
  def transmissionFlow: Transport.TransmissionFlow

  /** Describe properties about the Type connection in this transport */
  def transmissionType: Transport.TransmissionType

  /** Send to the other side. Out going Messages */
  def outbound: ZSink[R, Transport.OutErr, OUT, Nothing, Unit]

  /** Reciving from the other side. Income Messages */
  def inbound: ZStream[R, Transport.InErr, IN]

  def id: TransportID

  def send(message: OUT): ZIO[R, Nothing, Unit] =
    ZStream.succeed(message).run(outbound)

  def provide(env: ZEnvironment[R]): Transport[Any, IN, OUT] = Transport.Provided(self, env)
}

object Transport {
  type OutErr = Nothing
  type InErr = Nothing

  private[framework] final case class Provided[Env, IN, OUT](
      transport: Transport[Env, IN, OUT],
      env: ZEnvironment[Env],
  ) extends Transport[Any, IN, OUT] {

    def transmissionFlow: Transport.TransmissionFlow = transport.transmissionFlow
    def transmissionType: Transport.TransmissionType = transport.transmissionType
    def outbound: ZSink[Any, Transport.OutErr, OUT, Nothing, Unit] = transport.outbound.provideEnvironment(env)
    def inbound: ZStream[Any, Transport.InErr, IN] = transport.inbound.provideEnvironment(env)
    def id: TransportID = transport.id
    override def send(message: OUT): ZIO[Any, Nothing, Unit] = transport.send(message).provideEnvironment(env)

    override def toString() = s"Transport.Provided(${transport}, ${env})"
  }

  // ############################
  // ### transport properties ###
  // ############################

  /** Describe properties about the Flow (direction) of data */
  enum TransmissionFlow:
    case Sending extends TransmissionFlow
    case Receiving extends TransmissionFlow
    case BothWays extends TransmissionFlow

  // object TransmissionFlow {
  //   given decoder: JsonDecoder[TransmissionFlow] = JsonDecoder.string.mapOrFail(e => safeValueOf(TransmissionFlow.valueOf(e)))
  //   given encoder: JsonEncoder[TransmissionFlow] = JsonEncoder.string.contramap((e: TransmissionFlow) => e.toString)
  // }

  /** Describe properties about the Type connection */
  enum TransmissionType:
    case SingleTransmission extends TransmissionType
    case MultiTransmissions extends TransmissionType

  // object TransmissionType {
  //   given decoder: JsonDecoder[TransmissionType] = JsonDecoder.string.mapOrFail(e => safeValueOf(TransmissionType.valueOf(e)))
  //   given encoder: JsonEncoder[TransmissionType] = JsonEncoder.string.contramap((e: TransmissionType) => e.toString)
  // }

}

object TransportID:
  val TRANSPORT_ID = "TRANSPORT_ID"
  private var transportCounter = 1 // TODO use scala.util.Random.nextLong().toString
  private def nextTransportCounter = +this.synchronized { transportCounter += 1; transportCounter }
  def apply(value: String): TransportID = value
  def apply(): TransportID = "transport:" + nextTransportCounter
  def ws: TransportID = "transport:ws:" + nextTransportCounter
  def http: TransportID = "transport:http:" + nextTransportCounter
  def http(requestID: String): TransportID = http + ":" + requestID
  def http(requestID: Option[String]): TransportID = http + requestID.map(":" + _).getOrElse("")
  extension (value: TransportID)
    def id: String = value
    def logAnnotation = LogAnnotation(TRANSPORT_ID, value)
