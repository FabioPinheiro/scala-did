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

object PrismStateInMemoryData {
  def empty = PrismStateInMemoryData(Map.empty, Map.empty, Map.empty, Map.empty)
  given JsonDecoder[PrismStateInMemoryData] = DeriveJsonDecoder.gen[PrismStateInMemoryData]
  given JsonEncoder[PrismStateInMemoryData] = DeriveJsonEncoder.gen[PrismStateInMemoryData]
}

case class PrismStateInMemoryData(
    opHash2op: Map[String, MySignedPrismEvent[OP]], // TODO type EventHash
    tx2eventRef: Map[String, Seq[EventRef]],
    ssi2eventRef: Map[DIDSubject, Seq[EventRef]],
    vdr2eventRef: Map[RefVDR, Seq[EventRef]],
)

object PrismStateInMemory {
  def empty: ZIO[Any, Nothing, PrismStateInMemory] =
    Ref.make(PrismStateInMemoryData(Map.empty, Map.empty, Map.empty, Map.empty)).map(e => PrismStateInMemory(e))
  // given JsonDecoder[PrismStateInMemory] = DeriveJsonDecoder.gen[PrismStateInMemory]
  // given JsonEncoder[PrismStateInMemory] = DeriveJsonEncoder.gen[PrismStateInMemory]
}

case class PrismStateInMemory(ref: Ref[PrismStateInMemoryData]) extends PrismState {

  override def ssi2eventsRef: ZIO[Any, Nothing, Map[DIDSubject, Seq[EventRef]]] =
    ref.get.map(_.ssi2eventRef)

  override def vdr2eventsRef: ZIO[Any, Nothing, Map[RefVDR, Seq[EventRef]]] =
    ref.get.map(_.vdr2eventRef)

  override def getEventsIdByVDR(id: RefVDR): ZIO[Any, Nothing, Seq[EventRef]] =
    ref.get.map(_.vdr2eventRef.get(id).getOrElse(Seq.empty))

  // @scala.annotation.tailrec // TODO fix Cannot rewrite recursive call (ssiFromPreviousEventHash): it is not in tail positionbloop
  final def ssiFromPreviousEventHash(previousHash: String): ZIO[Any, Nothing, Option[String]] = ref.get.flatMap {
    _.opHash2op.get(previousHash) match
      case None             => ZIO.succeed(None)
      case Some(previousOp) =>
        previousOp.event match
          case CreateDidOP(publicKeys, services, context)     => ZIO.succeed(Some(previousOp.opHash))
          case UpdateDidOP(previousPreviousHash, id, actions) => ssiFromPreviousEventHash(previousPreviousHash)
          case DeactivateDidOP(previousEventHash, id)         => ZIO.succeed(None)
          case _                                              => ZIO.succeed(None)
  }

  // @scala.annotation.tailrec //TODO
  final def vdrFromPreviousEventHash(previousHash: String): ZIO[Any, Nothing, Option[RefVDR]] = ref.get.flatMap {
    _.opHash2op.get(previousHash) match
      case None             => ZIO.succeed(None)
      case Some(previousOp) =>
        previousOp.event match
          case CreateStorageEntryOP(didPrism, nonce, data, _)   => ZIO.succeed(Some(RefVDR.fromEvent(previousOp)))
          case UpdateStorageEntryOP(previousEventHash, data, _) => vdrFromPreviousEventHash(previousEventHash)
          case DeactivateStorageEntryOP(previousEventHash, _)   => ZIO.succeed(None)
          case _                                                => ZIO.succeed(None)
  }

  override def getEventsIdBySSI(ssi: DIDSubject): ZIO[Any, Nothing, Seq[EventRef]] = ref.get.map {
    _.ssi2eventRef.get(ssi) match
      case None      => Seq.empty
      case Some(seq) => seq
  }

  override def getEventByHash(refHash: EventHash): ZIO[Any, Nothing, Option[MySignedPrismEvent[OP]]] = ref.get.map {
    _.opHash2op.get(refHash.hex) match
      case None              => None
      case Some(signedEvent) => Some(signedEvent)
  }

  /** We add/index events with out validating them.
    *
    * Validation is then when it's been resolved.
    */
  override def addEvent(op: MySignedPrismEvent[OP]): ZIO[Any, Nothing, Unit] = op match
    case MySignedPrismEvent(tx, prismBlockIndex, prismEventIndex, signedWith, signature, pb) => {
      val opId = op.eventRef // rename to eventRef

      def newTx2eventRef(data: PrismStateInMemoryData) = data.tx2eventRef.updatedWith(tx) {
        case None      => Some(Seq(opId))
        case Some(seq) => Some(seq :+ opId)
      }

      def newOpHash2op(data: PrismStateInMemoryData) = data.opHash2op.updatedWith(opId.eventHash.hex) {
        case Some(value) =>
          if (value.opHash == opId.eventHash.hex) Some(op)
          else throw new RuntimeException("impossible state: duplicated event but with different hash?")
        case None => Some(op)
      }

      val same = ZIO.unit

      op.event match {
        case VoidOP(reason)                             => same
        case IssueCredentialBatchOP(value)              => same
        case RevokeCredentialsOP(value)                 => same
        case ProtocolVersionUpdateOP(value)             => same
        case CreateDidOP(publicKeys, services, context) =>
          ref.updateSome { data =>
            val did = DIDPrism(op.opHash)
            val newSSI2eventRef = data.ssi2eventRef.updatedWith(did) {
              _ match
                case None      => Some(Seq(opId)) // Normal case
                case Some(seq) => Some(seq :+ opId) // Repeated event
            }
            PrismStateInMemoryData(
              opHash2op = newOpHash2op(data),
              tx2eventRef = newTx2eventRef(data),
              ssi2eventRef = newSSI2eventRef,
              vdr2eventRef = data.vdr2eventRef,
            )
          }
        case UpdateDidOP(previousEventHash, id, actions) =>
          ssiFromPreviousEventHash(previousEventHash).flatMap {
            case None          => same // TODO add to the void
            case Some(ssiHash) =>
              ref.updateSome { data =>
                val did = DIDPrism(ssiHash)
                val newSSI2eventRef = data.ssi2eventRef.updatedWith(did) {
                  case None      => None
                  case Some(seq) => Some(seq :+ opId)
                }
                PrismStateInMemoryData(
                  opHash2op = newOpHash2op(data),
                  tx2eventRef = newTx2eventRef(data),
                  ssi2eventRef = newSSI2eventRef,
                  vdr2eventRef = data.vdr2eventRef,
                )
              }
          }
        case DeactivateDidOP(previousEventHash, id) =>
          ssiFromPreviousEventHash(previousEventHash).flatMap {
            case None          => same // TODO add to the void
            case Some(ssiHash) =>
              ref.updateSome { data =>
                val did = DIDPrism(ssiHash)
                val newSSI2eventRef = data.ssi2eventRef.updatedWith(did) {
                  case None      => None
                  case Some(seq) => Some(seq :+ opId)
                }
                PrismStateInMemoryData(
                  opHash2op = newOpHash2op(data),
                  tx2eventRef = newTx2eventRef(data),
                  ssi2eventRef = newSSI2eventRef,
                  vdr2eventRef = data.vdr2eventRef,
                )
              }
          }
        case CreateStorageEntryOP(didPrism, nonce, data, unknownFields) =>
          ref.updateSome { data =>
            val vdrRef = RefVDR.fromEvent(op)
            val newVDR2eventRef = data.vdr2eventRef.updatedWith(vdrRef) {
              case None      => Some(Seq(opId)) // Normal case
              case Some(seq) => Some(seq :+ opId) // Repeated event
            }
            PrismStateInMemoryData(
              opHash2op = newOpHash2op(data),
              tx2eventRef = newTx2eventRef(data),
              ssi2eventRef = data.ssi2eventRef,
              vdr2eventRef = newVDR2eventRef,
            )
          }
        case UpdateStorageEntryOP(previousEventHash, data, unknownFields) =>
          vdrFromPreviousEventHash(previousEventHash).flatMap {
            case None         => same
            case Some(vdrRef) =>
              ref.updateSome { data =>
                val newVDR2eventRef = data.vdr2eventRef.updatedWith(vdrRef) {
                  case None      => None // TODO add to the void
                  case Some(seq) => Some(seq :+ opId)
                }
                PrismStateInMemoryData(
                  opHash2op = newOpHash2op(data),
                  tx2eventRef = newTx2eventRef(data),
                  ssi2eventRef = data.ssi2eventRef,
                  vdr2eventRef = newVDR2eventRef,
                )
              }
          }
        case DeactivateStorageEntryOP(previousEventHash, unknownFields) =>
          vdrFromPreviousEventHash(previousEventHash).flatMap {
            case None         => same
            case Some(vdrRef) =>
              ref.updateSome { data =>
                val newVDR2eventRef = data.vdr2eventRef.updatedWith(vdrRef) {
                  case None      => None // TODO add to the void
                  case Some(seq) => Some(seq :+ opId)
                }
                PrismStateInMemoryData(
                  opHash2op = newOpHash2op(data),
                  tx2eventRef = newTx2eventRef(data),
                  ssi2eventRef = data.ssi2eventRef,
                  vdr2eventRef = newVDR2eventRef,
                )
              }
          }
      }
    }

  def makeSSI: ZIO[Any, Nothing, Seq[SSI]] = ref.get.map { data =>
    data.ssi2eventRef.map { (ssi, ops) =>
      ops.foldLeft(SSI.init(ssi)) { case (tmpSSI, opId) =>
        data.opHash2op.get(opId.eventHash.hex) match
          case None     => ???
          case Some(op) => tmpSSI.appendAny(op)
      }
    }.toSeq
  }

  def didDocuments: ZIO[Any, Nothing, Seq[DIDDocument]] = makeSSI.map(_.flatMap(_.didDocument))

}
