package fmgp.did.comm.mediator

import zio._
import zio.json._
import zio.http._
import zio.stream._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import zio.http._

type SocketID = String

case class MyChannel(id: SocketID, socketOutHub: Hub[String])
case class DIDSocketManager(
    sockets: Map[SocketID, MyChannel] = Map.empty,
    ids: Map[FROMTO, Seq[SocketID]] = Map.empty,
    kids: Map[VerificationMethodReferenced, Seq[SocketID]] = Map.empty,
) {

  def link(vmr: VerificationMethodReferenced, socketID: SocketID): DIDSocketManager =
    if (!sockets.keySet.contains(socketID)) this // if sockets is close
    else
      kids.get(vmr) match
        case Some(seq) if seq.contains(socketID) => this
        case Some(seq) => this.copy(kids = kids + (vmr -> (seq :+ socketID))).link(vmr.did.asFROMTO, socketID)
        case None      => this.copy(kids = kids + (vmr -> Seq(socketID))).link(vmr.did.asFROMTO, socketID)

  def link(from: FROMTO, socketID: SocketID): DIDSocketManager =
    if (!sockets.keySet.contains(socketID)) this // if sockets is close
    else
      ids.get(from) match
        case Some(seq) if seq.contains(socketID) => this
        case Some(seq)                           => this.copy(ids = ids + (from -> (seq :+ socketID)))
        case None                                => this.copy(ids = ids + (from -> Seq(socketID)))

  def registerSocket(myChannel: MyChannel) = this.copy(sockets = sockets + (myChannel.id -> myChannel))

  def unregisterSocket(socketID: SocketID) = this.copy(
    sockets = sockets.view.filterKeys(_ != socketID).toMap,
    ids = ids.map { case (did, socketsID) => (did, socketsID.filter(_ != socketID)) }.filterNot(_._2.isEmpty),
    kids = kids.map { case (kid, socketsID) => (kid, socketsID.filter(_ != socketID)) }.filterNot(_._2.isEmpty),
  )

  def publish(to: TO, msg: String): ZIO[Any, Nothing, Seq[Unit]] = {
    val socketIDs = this.ids.getOrElse(to.asFROMTO, Seq.empty)
    val myChannels = socketIDs.flatMap(id => this.sockets.get(id))
    ZIO.foreach(myChannels) { channel =>
      channel.socketOutHub
        .publish(msg)
        .flatMap {
          case true  => ZIO.logDebug(s"Publish Message to SocketID:${channel.id}")
          case false => ZIO.logWarning(s"Publish Message return false in SocketID:${channel.id}")
        }
    }
  }

}

object DIDSocketManager {
  def inBoundSize = 5
  def outBoundSize = 3

  private given JsonEncoder[Hub[String]] = JsonEncoder.string.contramap((e: Hub[String]) => "HUB")
  private given JsonEncoder[MyChannel] = DeriveJsonEncoder.gen[MyChannel]
  given encoder: JsonEncoder[DIDSocketManager] = DeriveJsonEncoder.gen[DIDSocketManager]

  def make = Ref.make(DIDSocketManager())

  def registerSocket(channel: WebSocketChannel, channelId: String) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
      hub <- Hub.bounded[String](outBoundSize)
      myChannel = MyChannel(channelId, hub)
      _ <- socketManager.update { _.registerSocket(myChannel) }
      _ <- channel.receiveAll {
        case ChannelEvent.ExceptionCaught(cause) => ZIO.log("ExceptionCaught(cause) ")
        case ChannelEvent.Read(message) =>
          message match
            case frame: WebSocketFrame.Binary => ZIO.log("Binary(bytes)")
            case frame: WebSocketFrame.Text =>
              frame.isFinal match
                case false => ZIO.logError("frame.isFinal is false! text: $frame")
                case true =>
                  frame.text match
                    case send if (send.startsWith("send")) =>
                      send.split(" ", 3).toSeq match
                        case Seq("send", did, message) =>
                          ZIO.service[Ref[DIDSocketManager]].flatMap(_.get).flatMap(_.publish(TO(did), message))
                        case test => // TODO REMOVE
                          for {
                            socketManager2 <- ZIO
                              .service[Ref[DIDSocketManager]]
                              .flatMap(_.get)
                              .flatMap(dsm =>
                                ZIO.foreach(dsm.sockets)((k, v) =>
                                  v.socketOutHub.publish(s"TEST: $test").map(e => (k, e))
                                )
                              )
                          } yield ()
                    case link if (link.startsWith("link")) =>
                      link.split(" ", 2).toSeq match
                        case Seq("link", did) =>
                          ZIO.service[Ref[DIDSocketManager]].flatMap(_.update(_.link(FROMTO(did), channelId)))
                        case text => // TODO REMOVE
                          ZIO.logWarning(s"Like from Socket fail: $text")
                    case "info" => ZIO.service[Ref[DIDSocketManager]].flatMap(_.get).debug
                    case text   => ZIO.log(s"Text:${text}")
            case WebSocketFrame.Close(status, reason) => ZIO.log("Close(status, reason)")
            case frame: WebSocketFrame.Continuation   => ZIO.log("Continuation(buffer)")
            case WebSocketFrame.Ping                  => ZIO.log("Ping")
            case WebSocketFrame.Pong                  => ZIO.log("Pong")
        case ChannelEvent.UserEventTriggered(event) => ZIO.log("UserEventTriggered(event)")
        case ChannelEvent.Registered                => ZIO.log("Registered")
        case ChannelEvent.Unregistered              => ZIO.log("Unregistered")
      }.fork
      sink = ZSink.foreach((value: String) => channel.send(ChannelEvent.Read(WebSocketFrame.text(value))))
      _ <- ZIO.log(s"Registering channel")
      _ <- ZStream.fromHub(myChannel.socketOutHub).run(sink) // TODO .fork does not work!!!
      _ <- ZIO.log(s"Channel concluded")
    } yield ()

  def newMessage(channel: WebSocketChannel, data: String, channelId: String) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
    } yield (channelId, data)

  def unregisterSocket(channel: WebSocketChannel, channelId: String) =
    for {
      socketManager <- ZIO.service[Ref[DIDSocketManager]]
      _ <- socketManager.update { case sm: DIDSocketManager => sm.unregisterSocket(channelId) }
      _ <- ZIO.log(s"Channel unregisterSocket")
    } yield ()

}
