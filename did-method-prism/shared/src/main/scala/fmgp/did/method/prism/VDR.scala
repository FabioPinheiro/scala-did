package fmgp.did.method.prism

import zio.json._
import fmgp.did.method.prism.proto._
import fmgp.did.method.prism.cardano.EventCursor

final case class VDR(
    id: RefVDR,
    did: Option[DIDPrism],
    // ACL // add something similar to the linux ACL System
    latestVDRHash: Option[EventHash],
    cursor: EventCursor, // append cursor
    nonce: Option[Array[Byte]],
    data: VDR.DataType,
    unsupportedValidationField: Boolean,
    // REMOVE disabled: Boolean,  // This is already on DataType VDR.DataDeactivated
) { self =>
  def appendAny(spo: MySignedPrismOperation[OP], ssi: SSIHistory): VDR = spo.operation match
    case _: CreateStorageEntryOP     => append(spo.asInstanceOf[MySignedPrismOperation[CreateStorageEntryOP]], ssi)
    case _: UpdateStorageEntryOP     => append(spo.asInstanceOf[MySignedPrismOperation[UpdateStorageEntryOP]], ssi)
    case _: DeactivateStorageEntryOP => append(spo.asInstanceOf[MySignedPrismOperation[DeactivateStorageEntryOP]], ssi)
    case _ /* others */              => self.copy(cursor = Ordering[EventCursor].max(cursor, spo.eventCursor))

  /** Int proto with have {{{reserved 3 to 49;}}} those field will be used for validation the Storage Events in the
    * future
    */
  private def nextUnsupportedValidationField(unknownFields: Set[Int]) =
    self.unsupportedValidationField | unknownFields.exists(_ <= 49)

  def append(
      spo: MySignedPrismOperation[CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP],
      ssiHistory: SSIHistory // TODO make it more type safe with a bew opaque type
  ): VDR = {
    did match
      case None => // ok
      case Some(valueDIDPrism) =>
        assert(valueDIDPrism == ssiHistory.didPrism, s"$valueDIDPrism != ${ssiHistory.didPrism}") // FIXME
    val ssi = ssiHistory.latestVersionBefore(spo.eventCursor)
    assert(Ordering[EventCursor].gt(spo.eventCursor, ssi.cursor)) // SSI check
    if (Ordering[EventCursor].lteq(spo.eventCursor, this.cursor)) self // Ignore if the event its already process
    else
      {
        if (!ssi.checkVdrSignature(spo)) self
        else {
          spo match
            case MySignedPrismOperation(tx, b, o, signedWith, signature, protobuf) =>
              spo.operation match
                case event @ CreateStorageEntryOP(didPrism, nonce, newData, unknownFields) =>
                  if (latestVDRHash.isDefined) self
                  else
                    assert(
                      self.id == RefVDR.fromEventHash(spo.eventHash),
                      s"${self.id} != ${RefVDR.fromEventHash(spo.eventHash)}"
                    )

                    self.copy(
                      did = Some(didPrism),
                      latestVDRHash = Some(spo.eventRef.eventHash),
                      nonce = Some(event.nonce).filter(_.isEmpty),
                      data = newData,
                      unsupportedValidationField = nextUnsupportedValidationField(unknownFields)
                    )
                    // unsupportedValidationField
                case event @ UpdateStorageEntryOP(previousOperationHash, newData, unknownFields) =>
                  self.latestVDRHash match
                    case None => self
                    case Some(thisLatestVDRHash) if thisLatestVDRHash.hex == previousOperationHash =>
                      self.copy(
                        latestVDRHash = Some(spo.eventRef.eventHash),
                        data = self.data.update(newData),
                        unsupportedValidationField = nextUnsupportedValidationField(unknownFields)
                      )
                    case Some(value) => self // Ignore if the update points to an old state
                case event @ DeactivateStorageEntryOP(previousOperationHash, unknownFields) =>
                  self.copy(
                    latestVDRHash = Some(spo.eventRef.eventHash),
                    data = VDR.DataDeactivated(self.data),
                    unsupportedValidationField = nextUnsupportedValidationField(unknownFields)
                  )
        }
      }.copy(cursor = spo.eventCursor)
  }

}

object VDR {
  given decoder: JsonDecoder[VDR] = DeriveJsonDecoder.gen[VDR]
  given encoder: JsonEncoder[VDR] = DeriveJsonEncoder.gen[VDR]

  def init(ref: RefVDR) =
    VDR(
      id = ref,
      did = None, // create.didPrism,
      latestVDRHash = None, // ref.value,
      cursor = EventCursor.init,
      nonce = None, // create.nonce,
      data = DataEmpty(), // create.data,
      unsupportedValidationField = false,
    )

  def make(vdrRef: RefVDR, ssiHistory: SSIHistory, ops: Seq[MySignedPrismOperation[OP]]) =
    ops.foldLeft(VDR.init(vdrRef)) { case (tmpVDR, op) => tmpVDR.appendAny(op, ssiHistory) }

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
