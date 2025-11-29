package fmgp.did.comm.protocol

import zio.json.*
import fmgp.did.*
import fmgp.did.comm.*

package object pickup3 {
  extension (msg: PlaintextMessage)
    def toStatusRequest: Either[String, StatusRequest] =
      StatusRequest.fromPlaintextMessage(msg)
    def toStatus: Either[String, Status] =
      Status.fromPlaintextMessage(msg)
    def toDeliveryRequest: Either[String, DeliveryRequest] =
      DeliveryRequest.fromPlaintextMessage(msg)
    def toMessageDelivery: Either[String, MessageDelivery] =
      MessageDelivery.fromPlaintextMessage(msg)
    def toMessagesReceived: Either[String, MessagesReceived] =
      MessagesReceived.fromPlaintextMessage(msg)
    def toLiveModeChange: Either[String, LiveModeChange] =
      LiveModeChange.fromPlaintextMessage(msg)
}
