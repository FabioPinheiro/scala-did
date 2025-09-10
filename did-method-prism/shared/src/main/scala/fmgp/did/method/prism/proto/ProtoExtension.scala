package fmgp.did.method.prism.proto

import fmgp.crypto.SHA256
import fmgp.did.method.prism.proto.{CreateDidOP, CreateStorageEntryOP, OP}
import proto.prism.PrismEvent
// import proto.prism.SignedPrismEvent
import fmgp.did.method.prism.{DIDPrism, EventHash}

// extension (obj: PrismObject) {}
// extension (block: PrismBlock) {}

extension (event: PrismEvent) {
  def eventHash: Array[Byte] = SHA256.digest(event.toByteArray) // TODO deprecate for TYPE safety with EventHash
  def eventHashStr: String = SHA256.digestToHex(event.toByteArray)
  def getEventHash: EventHash = EventHash(eventHash)
  def eventOperation: OP = OP.fromPrismEvent(event)
  def didPrism: Either[String, DIDPrism] = eventOperation match // TODO PartialFunction[PrismEvent, DIDPrism] ?
    case _: CreateDidOP => Right(DIDPrism(event.eventHashStr))
    case _              => Left("IMust be a Create DID PRISM Event")
  def vdrRef: Either[String, String] = eventOperation match
    case _: CreateStorageEntryOP => Right(event.eventHashStr)
    case _                       => Left("Must be a Create Storage Entry Event")

}

// extension (signedEvent: SignedPrismEvent) {
//   def getEventHash = signedEvent.event.map(_.eventHash)
// }
