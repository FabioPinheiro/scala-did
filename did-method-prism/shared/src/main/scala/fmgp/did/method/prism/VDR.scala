package fmgp.did.method.prism

import zio.json._
import fmgp.did.method.prism.DIDPrism
import fmgp.prism._
import proto.prism.ProtoUpdateStorageEntry

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
    data: VDR.DataType
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
          case event @ CreateStorageEntryOP(didPrismHash, nonce, newData) =>
            println("CreateStorageEntryOP") // FIXME REMOVE
            assert(did.specificId == didPrismHash) // FIXME
            // self.copy(data = self.data.update(newData))
            self
          case event @ UpdateStorageEntryOP(previousOperationHash, newData) =>
            println("UpdateStorageEntryOP") // FIXME REMOVE
            self.copy(data = self.data.update(newData))
          case event @ DeactivateStorageEntryOP(previousOperationHash) =>
            println("DeactivateStorageEntryOP") // FIXME REMOVE
            self.copy(data = VDR.DataDeactivated(self.data))

}

object VDR {
  sealed trait DataType { self =>
    def update(op: DataUpdateType): DataType = (self, op) match
      case (old: DataByteArray, update: DataByteArray)   => update // Replace
      case (old: DataIPFS, update: DataIPFS)             => update // Replace
      case (old: DataStatusList, update: DataStatusList) => ??? // FIXME
      case (old, update)                                 => self // ignore update

  }
  sealed trait DataUpdateType
  object DataType {
    given JsonDecoder[DataType] = DeriveJsonDecoder.gen[DataType]
    given JsonEncoder[DataType] = DeriveJsonEncoder.gen[DataType]
  }
  object DataUpdateType {
    given JsonDecoder[DataUpdateType] = DeriveJsonDecoder.gen[DataUpdateType]
    given JsonEncoder[DataUpdateType] = DeriveJsonEncoder.gen[DataUpdateType]
  }

  case class DataDeactivated(data: DataType) extends DataType
  case class DataByteArray(byteArray: Array[Byte]) extends DataType with DataUpdateType
  case class DataIPFS(cid: String) extends DataType with DataUpdateType
  case class DataStatusList(status: Map[Int, Array[Byte]]) extends DataType with DataUpdateType // TODO
}
