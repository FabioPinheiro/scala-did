package fmgp.did.method.prism

import zio._
import zio.json._
import scala.annotation.tailrec
import fmgp.did.DIDDocument
import fmgp.did.method.prism._
import fmgp.did.DIDSubject
import fmgp.did.method.prism.SSI
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.proto._

object PrismStateInMemory {
  def empty = PrismStateInMemory(Map.empty, Map.empty, Map.empty)
}

case class PrismStateInMemory(
    opHash2op: Map[String, MySignedPrismOperation[OP]],
    tx2eventRef: Map[String, Seq[EventRef]],
    ssi2eventRef: Map[DIDSubject, Seq[EventRef]],
) extends PrismState {

  override def ssi2eventsId: Map[DIDSubject, Seq[EventRef]] = ssi2eventRef // TODO RENAME
  override def getEventsIdByVDR(id: RefVDR): Seq[EventRef] = ??? // FIXME TODO

  @scala.annotation.tailrec
  final def ssiFromPreviousOperationHash(previousHash: String): Option[String] = {
    opHash2op.get(previousHash) match
      case None => None
      case Some(previousOp) =>
        previousOp.operation match
          case CreateDidOP(publicKeys, services, context)     => Some(previousOp.opHash)
          case UpdateDidOP(previousPreviousHash, id, actions) => ssiFromPreviousOperationHash(previousPreviousHash)
          case _                                              => None
  }

  override def getEventsIdBySSI(ssi: DIDSubject): Seq[EventRef] =
    ssi2eventRef.get(ssi) match
      case None      => Seq.empty
      case Some(seq) => seq

  override def getEventsByHash(refHash: String): Option[MySignedPrismOperation[OP]] =
    this.opHash2op.get(refHash) match
      case None              => None
      case Some(signedEvent) => Some(signedEvent)

  override def addEvent(op: MySignedPrismOperation[OP]): PrismState = op match
    case MySignedPrismOperation(tx, prismBlockIndex, prismOperationIndex, signedWith, signature, operation, pb) =>
      val opId = op.eventRef
      val newOpHash2op = opHash2op.updatedWith(opId.eventHash) {
        case Some(value) =>
          if (value.opHash == opId.eventHash) Some(op)
          else throw new RuntimeException("impossible state: duplicated operation but with different hash?")
        case None => Some(op)
      }
      val newTx2eventRef = tx2eventRef.updatedWith(tx) {
        case None      => Some(Seq(opId))
        case Some(seq) => Some(seq :+ opId)
      }
      operation match
        case VoidOP(reason)                                    => this
        case IssueCredentialBatchOP(value)                     => this
        case RevokeCredentialsOP(value)                        => this
        case ProtocolVersionUpdateOP(value)                    => this
        case CreateStorageEntryOP(didPrismHash, nonce, data)   => this // TODO
        case UpdateStorageEntryOP(previousOperationHash, data) => this // TODO
        case DeactivateStorageEntryOP(previousOperationHash)   => this // TODO
        case CreateDidOP(publicKeys, services, context) =>
          val did = DIDPrism(op.opHash)
          val newSSI2eventRef = ssi2eventRef.updatedWith(did) {
            _ match
              case None      => Some(Seq(opId))
              case Some(seq) => Some(seq :+ opId)
          }
          PrismStateInMemory(opHash2op = newOpHash2op, tx2eventRef = newTx2eventRef, ssi2eventRef = newSSI2eventRef)
        case UpdateDidOP(previousOperationHash, id, actions) =>
          ssiFromPreviousOperationHash(previousOperationHash) match
            case None => this
            case Some(ssiHash) =>
              val did = DIDPrism(ssiHash)
              val newSSI2eventRef = ssi2eventRef.updatedWith(did) {
                case None      => None
                case Some(seq) => Some(seq :+ opId)
              }
              PrismStateInMemory(opHash2op = newOpHash2op, tx2eventRef = newTx2eventRef, ssi2eventRef = newSSI2eventRef)
        case DeactivateDidOP(previousOperationHash, id) =>
          ssiFromPreviousOperationHash(previousOperationHash) match
            case None => this
            case Some(ssiHash) =>
              val did = DIDPrism(ssiHash)
              val newSSI2eventRef = ssi2eventRef.updatedWith(did) {
                case None      => None
                case Some(seq) => Some(seq :+ opId)
              }
              PrismStateInMemory(opHash2op = newOpHash2op, tx2eventRef = newTx2eventRef, ssi2eventRef = newSSI2eventRef)

  def makeSSI: Seq[SSI] = this.ssi2eventRef.map { (ssi, ops) =>
    ops.foldLeft(fmgp.did.method.prism.SSI.init(ssi)) { case (tmpSSI, opId) =>
      this.opHash2op.get(opId.eventHash) match
        case None     => ???
        case Some(op) => tmpSSI.appendAny(op)
    }
  }.toSeq
  def didDocuments: Seq[DIDDocument] = makeSSI.map(_.didDocument)

}
