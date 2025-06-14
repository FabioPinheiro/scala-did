package fmgp.did.method.prism

import zio.json._
import fmgp.did.method.prism.proto._
import fmgp.util.bytes2Hex
import fmgp.did.method.prism.cardano.EventCursor

opaque type RefVDR = String
object RefVDR:
  def apply(hash: String): RefVDR = hash
  def fromEventHash(hash: Array[Byte]): RefVDR = bytes2Hex(hash)
  def fromEvent(event: MySignedPrismOperation[OP]): RefVDR =
    event.operation match
      case CreateStorageEntryOP(didPrism, nonce, data)       => event.eventRef.eventHash
      case UpdateStorageEntryOP(previousOperationHash, data) => event.eventRef.eventHash
      case DeactivateStorageEntryOP(previousOperationHash)   => event.eventRef.eventHash
      case _                                                 => ??? // FIXME

  extension (id: RefVDR) def value: String = id

  given decoder: JsonDecoder[RefVDR] = JsonDecoder.string.map(RefVDR(_))
  given encoder: JsonEncoder[RefVDR] = JsonEncoder.string.contramap[RefVDR](_.value)
  // These given are useful if we use the RefVDR as a Key (ex: Map[RefVDR , Value])
  given JsonFieldDecoder[RefVDR] = JsonFieldDecoder.string.map(s => RefVDR(s)) // TODO use either
  given JsonFieldEncoder[RefVDR] = JsonFieldEncoder.string.contramap(e => e.value)

final case class VDR(
    id: RefVDR,
    did: Option[DIDPrism],
    // ACL // add something similar to the linux ACL System
    latestVDRHash: Option[String],
    cursor: EventCursor, // append cursor
    nonce: Option[Array[Byte]],
    data: VDR.DataType,
    // REMOVE disabled: Boolean,  // This is already on DataType VDR.DataDeactivated
) { self =>
  def appendAny(spo: MySignedPrismOperation[OP], ssi: SSI): VDR = spo.operation match
    case _: CreateStorageEntryOP     => append(spo.asInstanceOf[MySignedPrismOperation[CreateStorageEntryOP]], ssi)
    case _: UpdateStorageEntryOP     => append(spo.asInstanceOf[MySignedPrismOperation[UpdateStorageEntryOP]], ssi)
    case _: DeactivateStorageEntryOP => append(spo.asInstanceOf[MySignedPrismOperation[DeactivateStorageEntryOP]], ssi)
    case _ /* others */              => self.copy(cursor = Ordering[EventCursor].max(cursor, spo.eventCursor))

  def append(
      spo: MySignedPrismOperation[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP],
      ssi: SSI // TODO make it more type safe with a bew opaque type
  ): VDR = {
    assert(did == ssi.didPrism) // FIXME
    assert(Ordering[EventCursor].gt(spo.eventCursor, ssi.cursor)) // SSI check
    if (Ordering[EventCursor].lteq(spo.eventCursor, this.cursor)) self // Ignore if the event its already process
    else
      {
        if (!ssi.checkVdrSignature(spo)) self
        else {
          spo match
            case MySignedPrismOperation(tx, b, o, signedWith, signature, operation, protobuf) =>
              operation match
                case event @ CreateStorageEntryOP(didPrism, nonce, newData) =>
                  assert(did == didPrism) // FIXME
                  if (latestVDRHash.isDefined) self
                  else
                    assert(self.id == spo.eventRef)
                    self.copy(
                      did = Some(didPrism),
                      nonce = Some(event.nonce).filter(_.isEmpty),
                      data = newData,
                    )
                case event @ UpdateStorageEntryOP(previousOperationHash, newData) =>
                  self.latestVDRHash match
                    case None => self
                    case Some(thisLatestVDRHash) if thisLatestVDRHash == previousOperationHash =>
                      self.copy(data = self.data.update(newData))
                    case Some(value) => self // Ignore if the update points to an old state
                case event @ DeactivateStorageEntryOP(previousOperationHash) =>
                  self.copy(data = VDR.DataDeactivated(self.data))
        }
      }.copy(cursor = spo.eventCursor)
  }

}

object VDR {

  def init(ref: RefVDR) =
    VDR(
      id = ref,
      did = None, // create.didPrism,
      latestVDRHash = None, // ref.value,
      cursor = EventCursor.init,
      nonce = None, // create.nonce,
      data = DataEmpty(), // create.data,
    )

  sealed trait DataType { self =>
    def update(op: DataUpdateType): DataType = (self, op) match
      // when this storage system is created with Nothing in data)=
      case (old: DataEmpty, update: DataByteArray)  => update // init
      case (old: DataEmpty, update: DataIPFS)       => update // init
      case (old: DataEmpty, update: DataStatusList) => update // init
      // Operations on the estate machine
      case (old: DataByteArray, update: DataByteArray)   => update // Replace
      case (old: DataIPFS, update: DataIPFS)             => update // Replace
      case (old: DataStatusList, update: DataStatusList) => ??? // TOOD
      // Invalid operations on the data state of machine
      case (old, update) => self // ignore update

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

  case class DataEmpty() extends DataType with DataUpdateType
  case class DataDeactivated(data: DataType) extends DataType
  case class DataByteArray(byteArray: Array[Byte]) extends DataType with DataUpdateType
  case class DataIPFS(cid: String) extends DataType with DataUpdateType
  case class DataStatusList(status: Map[Int, Array[Byte]]) extends DataType with DataUpdateType // TODO
}
