package fmgp.did.framework

import zio.*
import zio.json.*
import zio.stream.*
import fmgp.crypto.error.*
import fmgp.did.*
import fmgp.did.comm.*

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
