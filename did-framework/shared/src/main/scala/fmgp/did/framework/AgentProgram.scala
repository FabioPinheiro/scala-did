package fmgp.did.framework

import zio._
import zio.json._
import zio.stream._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._

trait AgentProgram {
  def subject: DIDSubject

  /** receive a Message and a transport where that message was recived.
    *
    * We intend to deprecate this API in favor of acceptTransport
    */
  def receiveMsg(
      msg: SignedMessage | EncryptedMessage,
      transport: TransportDIDComm[Any]
  ): URIO[Operations & Resolver, Unit]

  /** Accept a transport where messages can be received. */
  def acceptTransport(transport: TransportDIDComm[Any]): URIO[Operations & Resolver, Unit]

}
