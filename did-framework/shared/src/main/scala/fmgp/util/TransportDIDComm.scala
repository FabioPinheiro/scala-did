package fmgp.util

import zio._
import zio.stream._
import fmgp.did.comm._

trait TransportDIDComm[R] extends Transport[R, SignedMessage | EncryptedMessage, SignedMessage | EncryptedMessage]
