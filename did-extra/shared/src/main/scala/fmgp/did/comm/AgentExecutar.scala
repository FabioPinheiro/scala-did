package fmgp.did.comm

import zio._
import zio.json._
import zio.stream._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.util._

trait AgentExecutar {
  def subject: DIDSubject

  def receiveMsg(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any]
  ): URIO[Operations, Unit]

}
