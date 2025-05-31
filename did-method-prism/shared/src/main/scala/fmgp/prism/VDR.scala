package fmgp.prism

import zio.json._
import fmgp.did.method.prism.DIDPrism

opaque type RefVDR = String
object RefVDR:
  def apply(hash: String): RefVDR = hash
  extension (id: RefVDR) def value: String = id
  given decoder: JsonDecoder[RefVDR] = JsonDecoder.string.map(RefVDR(_))
  given encoder: JsonEncoder[RefVDR] = JsonEncoder.string.contramap[RefVDR](_.value)

final case class VDR(
    id: RefVDR,
    did: DIDPrism,
    // ssi: SSI, ///FIXME
    latestVDRHash: Option[String],
    data: Array[Byte]
) { self =>
  def appendAny(spo: MySignedPrismOperation[OP]): VDR = spo.operation match
    // VDR
    case _: CreateStorageEntryOP     => append(spo.asInstanceOf[MySignedPrismOperation[CreateStorageEntryOP]])
    case _: UpdateStorageEntryOP     => append(spo.asInstanceOf[MySignedPrismOperation[UpdateStorageEntryOP]])
    case _: DeactivateStorageEntryOP => append(spo.asInstanceOf[MySignedPrismOperation[DeactivateStorageEntryOP]])
    // DID
    // case _: CreateDidOP     => copy(ssi = ssi.append(spo.asInstanceOf[MySignedPrismOperation[CreateDidOP]]))
    // case _: UpdateDidOP     => copy(ssi = ssi.append(spo.asInstanceOf[MySignedPrismOperation[UpdateDidOP]]))
    // case _: DeactivateDidOP => copy(ssi = ssi.append(spo.asInstanceOf[MySignedPrismOperation[DeactivateDidOP]]))
    // others
    case _ => self

  def append(spo: MySignedPrismOperation[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP]): VDR =
    spo match
      case MySignedPrismOperation(tx, b, o, signedWith, signature, operation, protobuf) =>
        operation match
          case event @ CreateStorageEntryOP(didPrismHash, nonce, data) =>
            println("CreateStorageEntryOP") // FIXME REMOVE
            assert(did.specificId == didPrismHash) // FIXME
            self

          case event @ UpdateStorageEntryOP(previousOperationHash, data) =>
            println("UpdateStorageEntryOP") // FIXME REMOVE
            self
          case event @ DeactivateStorageEntryOP(previousOperationHash) =>
            println("DeactivateStorageEntryOP") // FIXME REMOVE
            self

}

object VDR {}
