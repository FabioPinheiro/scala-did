package fmgp.prism

import zio._
import zio.json._
import scala.annotation.tailrec
import fmgp.did.DIDDocument

case class OpId(b: Int, o: Int, opHash: String)
object OpId {
  given JsonDecoder[OpId] = DeriveJsonDecoder.gen[OpId]
  given JsonEncoder[OpId] = DeriveJsonEncoder.gen[OpId]
}

object PrismState {
  given JsonDecoder[PrismState] = DeriveJsonDecoder.gen[PrismState]
  given JsonEncoder[PrismState] = DeriveJsonEncoder.gen[PrismState]
  def empty = PrismState(Map.empty, Map.empty, Map.empty)
}

case class PrismState(
    opHash2op: Map[String, MySignedPrismOperation[OP]],
    tx2opId: Map[String, Seq[OpId]],
    ssi2opId: Map[String, Seq[OpId]],
) {

  def lastSyncedBlockEpochSecondNano: (Long, Int) = {
    val now = java.time.Instant.now // FIXME
    (now.getEpochSecond, now.getNano)
  }

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

  def allOpForDID(did: String): Seq[MySignedPrismOperation[OP]] =
    ssi2opId
      .get(did)
      .getOrElse(Seq.empty)
      .map(opId =>
        this.opHash2op.getOrElse(
          opId.opHash,
          throw new RuntimeException("impossible state: missing Operation Hash")
        )
      )

  def addOp(op: MySignedPrismOperation[OP]) = op match
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
          PrismState(opHash2op = newOpHash2op, tx2opId = newTx2opId, ssi2opId = newSSI2opId)
        case UpdateDidOP(previousOperationHash, id, actions) =>
          ssiFromPreviousOperationHash(previousOperationHash) match
            case None => this
            case Some(ssiHash) =>
              val did = s"did:prism:$ssiHash"
              val newSSI2opId = ssi2opId.updatedWith(did) {
                case None      => None
                case Some(seq) => Some(seq :+ opId)
              }
              PrismState(opHash2op = newOpHash2op, tx2opId = newTx2opId, ssi2opId = newSSI2opId)
        case DeactivateDidOP(previousOperationHash, id) =>
          ssiFromPreviousOperationHash(previousOperationHash) match
            case None => this
            case Some(ssiHash) =>
              val did = s"did:prism:$ssiHash"
              val newSSI2opId = ssi2opId.updatedWith(did) {
                case None      => None
                case Some(seq) => Some(seq :+ opId)
              }
              PrismState(opHash2op = newOpHash2op, tx2opId = newTx2opId, ssi2opId = newSSI2opId)

  def makeSSI: Seq[SSI] = this.ssi2opId.map { (ssi, ops) =>
    ops.foldLeft(SSI.init(ssi)) { case (tmpSSI, opId) =>
      this.opHash2op.get(opId.opHash) match
        case None     => ???
        case Some(op) => tmpSSI.appendAny(op)
    }
  }.toSeq
  def didDocuments: Seq[DIDDocument] = makeSSI.map(_.didDocument)

}
