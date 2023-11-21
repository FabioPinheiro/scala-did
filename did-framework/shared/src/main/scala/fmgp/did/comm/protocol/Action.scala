package fmgp.did.comm.protocol

import fmgp.did.comm.PlaintextMessage

sealed trait Action
object NoReply extends Action
sealed trait AnyReply extends Action { def msg: PlaintextMessage }
case class Reply(msg: PlaintextMessage) extends AnyReply
