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
  def empty = PrismStateInMemory(Map.empty, Map.empty, Map.empty, Map.empty)
  given JsonDecoder[PrismStateInMemory] = DeriveJsonDecoder.gen[PrismStateInMemory]
  given JsonEncoder[PrismStateInMemory] = DeriveJsonEncoder.gen[PrismStateInMemory]
}

case class PrismStateInMemory(
    opHash2op: Map[String, MySignedPrismOperation[OP]], // TODO type EventHash
    tx2eventRef: Map[String, Seq[EventRef]],
    ssi2eventRef: Map[DIDSubject, Seq[EventRef]],
    vdr2eventRef: Map[RefVDR, Seq[EventRef]],
) extends PrismState {

  override def ssi2eventsId: Map[DIDSubject, Seq[EventRef]] = ssi2eventRef // TODO RENAME
  override def getEventsIdByVDR(id: RefVDR): Seq[EventRef] = vdr2eventRef.get(id).getOrElse(Seq.empty)

  @scala.annotation.tailrec
  final def ssiFromPreviousEventHash(previousHash: String): Option[String] = {
    opHash2op.get(previousHash) match
      case None => None
      case Some(previousOp) =>
        previousOp.operation match
          case CreateDidOP(publicKeys, services, context)     => Some(previousOp.opHash)
          case UpdateDidOP(previousPreviousHash, id, actions) => ssiFromPreviousEventHash(previousPreviousHash)
          case DeactivateDidOP(previousOperationHash, id)     => None
          case _                                              => None
  }

  @scala.annotation.tailrec
  final def vdrFromPreviousEventHash(previousHash: String): Option[RefVDR] = {
    opHash2op.get(previousHash) match
      case None => None
      case Some(previousOp) =>
        previousOp.operation match
          case CreateStorageEntryOP(didPrism, nonce, data, _)   => Some(RefVDR.fromEvent(previousOp))
          case UpdateStorageEntryOP(previousEventHash, data, _) => vdrFromPreviousEventHash(previousEventHash)
          case DeactivateStorageEntryOP(previousEventHash, _)   => None
          case _                                                => None
  }

  override def getEventsIdBySSI(ssi: DIDSubject): Seq[EventRef] =
    ssi2eventRef.get(ssi) match
      case None      => Seq.empty
      case Some(seq) => seq

  override def getEventsByHash(refHash: EventHash): Option[MySignedPrismOperation[OP]] =
    this.opHash2op.get(refHash.hex) match
      case None              => None
      case Some(signedEvent) => Some(signedEvent)

  /** We add/index events with out validating them.
    *
    * Validation is then when it's been resolved.
    */
  override def addEvent(op: MySignedPrismOperation[OP]): PrismState = op match
    case MySignedPrismOperation(tx, prismBlockIndex, prismOperationIndex, signedWith, signature, operation, pb) =>
      val opId = op.eventRef // rename to eventRef
      val newOpHash2op = opHash2op.updatedWith(opId.eventHash.hex) {
        case Some(value) =>
          if (value.opHash == opId.eventHash.hex) Some(op)
          else throw new RuntimeException("impossible state: duplicated operation but with different hash?")
        case None => Some(op)
      }
      val newTx2eventRef = tx2eventRef.updatedWith(tx) {
        case None      => Some(Seq(opId))
        case Some(seq) => Some(seq :+ opId)
      }
      operation match {
        case VoidOP(reason)                 => this
        case IssueCredentialBatchOP(value)  => this
        case RevokeCredentialsOP(value)     => this
        case ProtocolVersionUpdateOP(value) => this
        case CreateDidOP(publicKeys, services, context) =>
          val did = DIDPrism(op.opHash)
          val newSSI2eventRef = ssi2eventRef.updatedWith(did) {
            _ match
              case None      => Some(Seq(opId)) // Normal case
              case Some(seq) => Some(seq :+ opId) // Repeated event
          }
          PrismStateInMemory(
            opHash2op = newOpHash2op,
            tx2eventRef = newTx2eventRef,
            ssi2eventRef = newSSI2eventRef,
            vdr2eventRef = this.vdr2eventRef,
          )
        case UpdateDidOP(previousOperationHash, id, actions) =>
          ssiFromPreviousEventHash(previousOperationHash) match
            case None => this // TODO add to the void
            case Some(ssiHash) =>
              val did = DIDPrism(ssiHash)
              val newSSI2eventRef = ssi2eventRef.updatedWith(did) {
                case None      => None
                case Some(seq) => Some(seq :+ opId)
              }
              PrismStateInMemory(
                opHash2op = newOpHash2op,
                tx2eventRef = newTx2eventRef,
                ssi2eventRef = newSSI2eventRef,
                vdr2eventRef = this.vdr2eventRef,
              )
        case DeactivateDidOP(previousOperationHash, id) =>
          ssiFromPreviousEventHash(previousOperationHash) match
            case None => this // TODO add to the void
            case Some(ssiHash) =>
              val did = DIDPrism(ssiHash)
              val newSSI2eventRef = ssi2eventRef.updatedWith(did) {
                case None      => None
                case Some(seq) => Some(seq :+ opId)
              }
              PrismStateInMemory(
                opHash2op = newOpHash2op,
                tx2eventRef = newTx2eventRef,
                ssi2eventRef = newSSI2eventRef,
                vdr2eventRef = this.vdr2eventRef,
              )
        case CreateStorageEntryOP(didPrism, nonce, data, unknownFields) =>
          val vdrRef = RefVDR.fromEvent(op)
          val newVDR2eventRef = vdr2eventRef.updatedWith(vdrRef) {
            case None      => Some(Seq(opId)) // Normal case
            case Some(seq) => Some(seq :+ opId) // Repeated event
          }
          PrismStateInMemory(
            opHash2op = newOpHash2op,
            tx2eventRef = newTx2eventRef,
            ssi2eventRef = this.ssi2eventRef,
            vdr2eventRef = newVDR2eventRef,
          )
        case UpdateStorageEntryOP(previousOperationHash, data, unknownFields) =>
          vdrFromPreviousEventHash(previousOperationHash) match
            case None => this
            case Some(vdrRef) =>
              val newVDR2eventRef = vdr2eventRef.updatedWith(vdrRef) {
                case None      => None // TODO add to the void
                case Some(seq) => Some(seq :+ opId)
              }
              PrismStateInMemory(
                opHash2op = newOpHash2op,
                tx2eventRef = newTx2eventRef,
                ssi2eventRef = this.ssi2eventRef,
                vdr2eventRef = newVDR2eventRef,
              )
        case DeactivateStorageEntryOP(previousOperationHash, unknownFields) =>
          vdrFromPreviousEventHash(previousOperationHash) match
            case None => this
            case Some(vdrRef) =>
              val newVDR2eventRef = vdr2eventRef.updatedWith(vdrRef) {
                case None      => None // TODO add to the void
                case Some(seq) => Some(seq :+ opId)
              }
              PrismStateInMemory(
                opHash2op = newOpHash2op,
                tx2eventRef = newTx2eventRef,
                ssi2eventRef = this.ssi2eventRef,
                vdr2eventRef = newVDR2eventRef,
              )
      }

  def makeSSI: Seq[SSI] = this.ssi2eventRef.map { (ssi, ops) =>
    ops.foldLeft(fmgp.did.method.prism.SSI.init(ssi)) { case (tmpSSI, opId) =>
      this.opHash2op.get(opId.eventHash.hex) match
        case None     => ???
        case Some(op) => tmpSSI.appendAny(op)
    }
  }.toSeq
  def didDocuments: Seq[DIDDocument] = makeSSI.flatMap(_.didDocument)

}
