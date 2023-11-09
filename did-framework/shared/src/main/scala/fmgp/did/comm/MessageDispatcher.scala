package fmgp.did.comm

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import fmgp.util.MyHeaders

trait MessageDispatcher {
  // TODO deprecate this
  def send(
      msg: EncryptedMessage,
      /*context*/
      destination: String,
  ): ZIO[Any, DidFail, String]
}
