package fmgp.did.comm

import zio._
import zio.json._
import zio.stream._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.util.Transport

type SocketID = String

case class TransportManager(
    transports: Seq[Transport[Any, String]] = Seq.empty,
    ids: Map[FROMTO, Seq[SocketID]] = Map.empty,
    kids: Map[VerificationMethodReferenced, Seq[SocketID]] = Map.empty,
) {

  def link(vmr: VerificationMethodReferenced, transportID: SocketID): TransportManager =
    if (!transports.map(_.id).contains(transportID)) this // if transport is close
    else
      kids.get(vmr) match
        case Some(seq) if seq.contains(transportID) => this
        case Some(seq) => this.copy(kids = kids + (vmr -> (seq :+ transportID))).link(vmr.did.asFROMTO, transportID)
        case None      => this.copy(kids = kids + (vmr -> Seq(transportID))).link(vmr.did.asFROMTO, transportID)

  def link(from: FROMTO, transport: Transport[Any, String]): TransportManager = link(from, transport.id)
  def link(from: FROMTO, transportID: SocketID): TransportManager =
    if (!transports.map(_.id).contains(transportID)) this // if transport is close
    else
      ids.get(from) match
        case Some(seq) if seq.contains(transportID) => this
        case Some(seq)                              => this.copy(ids = ids + (from -> (seq :+ transportID)))
        case None                                   => this.copy(ids = ids + (from -> Seq(transportID)))

  def registerTransport(transport: Transport[Any, String]) =
    this.copy(transports = transport +: transports)

  def unregisterTransport(transportID: SocketID) = this.copy(
    transports = transports.filter(_.id != transportID),
    ids = ids.map { case (did, ids) => (did, ids.filter(_ != transportID)) }.filterNot(_._2.isEmpty),
    kids = kids.map { case (kid, ids) => (kid, ids.filter(_ != transportID)) }.filterNot(_._2.isEmpty),
  )

  def publish(to: TO, msg: String): ZIO[Any, Nothing, Seq[Unit]] = {
    val transportIDs = this.ids.getOrElse(to.asFROMTO, Seq.empty)
    val myChannels = transportIDs.flatMap(id => this.transports.find(_.id == id))
    ZIO.foreach(myChannels) { _.send(msg) }
  }

}

object TransportManager {

  def make = Ref.make(TransportManager())

  def registerTransport(transport: Transport[Any, String]) =
    for {
      socketManager <- ZIO.service[Ref[TransportManager]]
      _ <- socketManager.update { _.registerTransport(transport) }
      _ <- ZIO.log(s"RegisterTransport concluded")
    } yield ()

  // def newMessage(channel: WebSocketChannel, data: String, channelId: String) =
  //   for {
  //     socketManager <- ZIO.service[Ref[TransportManager]]
  //   } yield (channelId, data)

  def unregisterTransport(transportId: String) =
    for {
      socketManager <- ZIO.service[Ref[TransportManager]]
      _ <- socketManager.update { case sm: TransportManager => sm.unregisterTransport(transportId) }
      _ <- ZIO.log(s"Channel unregisterSocket")
    } yield ()

}
