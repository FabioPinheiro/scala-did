package fmgp.did.comm

import zio._
import zio.json._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol._
import fmgp.util._
import zio.stream.ZStream

trait AgentExecutar {
  def subject: DIDSubject

  /** This is the entry point. The Operator call this method */
  def receiveMsg(msg: EncryptedMessage, transport: Transport[Any, String]): URIO[Operations, Unit]
}
