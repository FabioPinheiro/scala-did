package fmgp.did.comm

import zio._
import zio.json._
import zio.http._
import zio.stream._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.util.TransportWSImp

type SocketID = String

// @deprecated("Use Transport/Operator/AgentExecutar")
case class DIDSocketManager(
    transports: Map[SocketID, TransportWSImp[String]] = Map.empty,
    ids: Map[FROMTO, Seq[SocketID]] = Map.empty,
    kids: Map[VerificationMethodReferenced, Seq[SocketID]] = Map.empty,
) {

  def link(vmr: VerificationMethodReferenced, socketID: SocketID): DIDSocketManager =
    if (!transports.keySet.contains(socketID)) this // if transport is close
    else
      kids.get(vmr) match
        case Some(seq) if seq.contains(socketID) => this
        case Some(seq) => this.copy(kids = kids + (vmr -> (seq :+ socketID))).link(vmr.did.asFROMTO, socketID)
        case None      => this.copy(kids = kids + (vmr -> Seq(socketID))).link(vmr.did.asFROMTO, socketID)

  def link(from: FROMTO, socketID: SocketID): DIDSocketManager =
    if (!transports.keySet.contains(socketID)) this // if transport is close
    else
      ids.get(from) match
        case Some(seq) if seq.contains(socketID) => this
        case Some(seq)                           => this.copy(ids = ids + (from -> (seq :+ socketID)))
        case None                                => this.copy(ids = ids + (from -> Seq(socketID)))

  def registerTransport(transport: TransportWSImp[String]) =
    this.copy(transports = transports + (transport.id -> transport))

  def unregisterTransport(transportID: SocketID) = this.copy(
    transports = transports.view.filterKeys(_ != transportID).toMap,
    ids = ids.map { case (did, socketsID) => (did, socketsID.filter(_ != transportID)) }.filterNot(_._2.isEmpty),
    kids = kids.map { case (kid, socketsID) => (kid, socketsID.filter(_ != transportID)) }.filterNot(_._2.isEmpty),
  )

  def publish(to: TO, msg: String): ZIO[Any, Nothing, Seq[Unit]] = {
    val socketIDs = this.ids.getOrElse(to.asFROMTO, Seq.empty)
    val myChannels = socketIDs.flatMap(id => this.transports.get(id))
    ZIO.foreach(myChannels) { _.send(msg) }
  }

}

// @deprecated("Use Transport/Operator/AgentExecutar")
object DIDSocketManager {

  def make = Ref.make(DIDSocketManager())

  def registerTransport(transport: TransportWSImp[String]) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
      _ <- socketManager.update { _.registerTransport(transport) }
      _ <- ZIO.log(s"RegisterTransport concluded")
    } yield ()

  def newMessage(channel: WebSocketChannel, data: String, channelId: String) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
    } yield (channelId, data)

  def unregisterTransport(transportId: String) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
      _ <- socketManager.update { case sm: DIDSocketManager => sm.unregisterTransport(transportId) }
      _ <- ZIO.log(s"Channel unregisterSocket")
    } yield ()

}
