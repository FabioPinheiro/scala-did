package fmgp.prism

import zio._
import zio.json._
import scala.annotation.tailrec
import fmgp.did.DIDDocument

object PrismStateInMemory {
  def empty = PrismStateInMemory(Map.empty, Map.empty, Map.empty)
}

case class PrismStateInMemory(
    opHash2op: Map[String, MySignedPrismOperation[OP]],
    tx2opId: Map[String, Seq[OpId]],
    ssi2opId: Map[String, Seq[OpId]],
) extends PrismState {

  def ssi2eventsId = tx2opId // TODO RENAME

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

  def getEventsIdBySSI(ssi: String): Seq[OpId] =
    ssi2opId.get(ssi) match
      case None      => Seq.empty
      case Some(seq) => seq

  def getEventsByHash(refHash: String): Option[MySignedPrismOperation[OP]] =
    this.opHash2op.get(refHash) match
      case None              => None
      case Some(signedEvent) => Some(signedEvent)

  def addEvent(op: MySignedPrismOperation[OP]): PrismState = op match
    case MySignedPrismOperation(tx, prismBlockIndex, prismOperationIndex, signedWith, signature, operation, pb) =>
      val opId = op.opId
      val newOpHash2op = opHash2op.updatedWith(opId.opHash) {
        case Some(value) =>
          if (value.opHash == opId.opHash) Some(op)
          else throw new RuntimeException("impossible state: duplicated operation but with different hash?")
        case None => Some(op)
      }
      val newTx2opId = tx2opId.updatedWith(tx) {
        case None      => Some(Seq(opId))
        case Some(seq) => Some(seq :+ opId)
      }
      operation match
        case VoidOP(reason)                 => this
        case IssueCredentialBatchOP(value)  => this
        case RevokeCredentialsOP(value)     => this
        case ProtocolVersionUpdateOP(value) => this
        case CreateStorageEntryOP(value)    => this
        case UpdateStorageEntryOP(value)    => this
        case CreateDidOP(publicKeys, services, context) =>
          val did = s"did:prism:${op.opHash}"
          val newSSI2opId = ssi2opId.updatedWith(did) {
            _ match
              case None      => Some(Seq(opId))
              case Some(seq) => Some(seq :+ opId)
          }
          PrismStateInMemory(opHash2op = newOpHash2op, tx2opId = newTx2opId, ssi2opId = newSSI2opId)
        case UpdateDidOP(previousOperationHash, id, actions) =>
          ssiFromPreviousOperationHash(previousOperationHash) match
            case None => this
            case Some(ssiHash) =>
              val did = s"did:prism:$ssiHash"
              val newSSI2opId = ssi2opId.updatedWith(did) {
                case None      => None
                case Some(seq) => Some(seq :+ opId)
              }
              PrismStateInMemory(opHash2op = newOpHash2op, tx2opId = newTx2opId, ssi2opId = newSSI2opId)
        case DeactivateDidOP(previousOperationHash, id) =>
          ssiFromPreviousOperationHash(previousOperationHash) match
            case None => this
            case Some(ssiHash) =>
              val did = s"did:prism:$ssiHash"
              val newSSI2opId = ssi2opId.updatedWith(did) {
                case None      => None
                case Some(seq) => Some(seq :+ opId)
              }
              PrismStateInMemory(opHash2op = newOpHash2op, tx2opId = newTx2opId, ssi2opId = newSSI2opId)

  def makeSSI: Seq[SSI] = this.ssi2opId.map { (ssi, ops) =>
    ops.foldLeft(SSI.init(ssi)) { case (tmpSSI, opId) =>
      this.opHash2op.get(opId.opHash) match
        case None     => ???
        case Some(op) => tmpSSI.appendAny(op)
    }
  }.toSeq
  def didDocuments: Seq[DIDDocument] = makeSSI.map(_.didDocument)

}
