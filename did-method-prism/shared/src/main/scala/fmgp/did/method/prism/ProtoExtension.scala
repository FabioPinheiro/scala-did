package fmgp.did.method.prism

import proto.prism.PrismOperation
import fmgp.crypto.SHA256
import fmgp.prism._

// extension (obj: PrismObject) {}
// extension (block: PrismBlock) {}

extension (event: PrismOperation) {
  def eventHash: Array[Byte] = SHA256.digest(event.toByteArray)
  def eventHashStr: String = SHA256.digestToHex(event.toByteArray)
  def eventOperation: OP = OP.fromPrismOperation(event)
  def didPrism: Either[String, DIDPrism] = eventOperation match // TODO PartialFunction[PrismOperation, DIDPrism] ?
    case _: CreateDidOP => Right(DIDPrism(event.eventHashStr))
    case _              => Left("IMust be a Create DID PRISM Event")
  def vdrRef: Either[String, String] = eventOperation match
    case _: CreateStorageEntryOP => Right(event.eventHashStr)
    case _                       => Left("Must be a Create Storage Entry Event")

}
