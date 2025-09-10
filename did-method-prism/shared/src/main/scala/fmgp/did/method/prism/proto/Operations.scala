package fmgp.did.method.prism.proto

import zio.json._
import fmgp.util.bytes2Hex
import proto.prism._
import fmgp.did.method.prism.VDR
import fmgp.did.method.prism.DIDPrism

sealed trait OP
object OP {
  type TypeDidEvent = CreateDidOP | UpdateDidOP | DeactivateDidOP
  type TypeStorageEntryEvent = CreateStorageEntryOP | UpdateStorageEntryOP | DeactivateStorageEntryOP

  given JsonDecoder[OP] = DeriveJsonDecoder.gen[OP]
  given JsonEncoder[OP] = DeriveJsonEncoder.gen[OP]

  def fromPrismEvent(prismEvent: PrismEvent): OP = {
    import proto.prism.PrismEvent.Event
    prismEvent.event match {
      case Event.Empty                     => VoidOP("PRISM Event is missing")
      case Event.CreateDid(p)              => CreateDidOP.fromProto(p)
      case Event.UpdateDid(p)              => UpdateDidOP.fromProto(p)
      case Event.IssueCredentialBatch(p)   => VoidOP("TODO IssueCredential") // IssueCredentialBatchOP.fromProto(p)
      case Event.RevokeCredentials(p)      => VoidOP("TODO RevokeCredentials") // RevokeCredentialsOP.fromProto(p)
      case Event.ProtocolVersionUpdate(p)  => VoidOP("TODO") // ProtocolVersionUpdateOP.fromProto(p)
      case Event.DeactivateDid(p)          => DeactivateDidOP.fromProto(p)
      case Event.CreateStorageEntry(p)     => CreateStorageEntryOP.fromProto(p)
      case Event.UpdateStorageEntry(p)     => UpdateStorageEntryOP.fromProto(p)
      case Event.DeactivateStorageEntry(p) => DeactivateStorageEntryOP.fromProto(p)
    }
  }
}
case class VoidOP(reason: String) extends OP

/** @param publicKeys
  *   (2) The keys that belong to this DID Document.
  * @param services
  *   (3) The list of services that belong to this DID Document.
  * @param context
  *   (4) The list of @ context values to consider on JSON-LD representations
  */
case class CreateDidOP( // Like DIDCreationData
    publicKeys: Seq[PrismPublicKey],
    services: Seq[MyService],
    context: Seq[String],
) extends OP

/** @param previous_event_hash
  *   (1) - The hash of the most recent event that was used to create or update the DID.
  * @param id
  *   (2) - exclude TODO: To be redefined after we start using this event.
  * @param actions
  *   (3) The actual updates to perform on the DID.
  */
case class UpdateDidOP(previousEventHash: String, id: String, actions: Seq[UpdateDidOP.Action]) extends OP {
  def previousOpHashHex = previousEventHash
}

case class IssueCredentialBatchOP(value: String) extends OP
case class RevokeCredentialsOP(value: String) extends OP
case class ProtocolVersionUpdateOP(value: String) extends OP

/** @param previous_event_hash
  *   (1) - The hash of the most recent event that was used to create or update the DID.
  * @param id
  *   (2) - DID Suffix of the DID to be deactivated
  */
case class DeactivateDidOP(previousEventHash: String, id: String) extends OP

case class CreateStorageEntryOP(
    didPrism: DIDPrism,
    nonce: Array[Byte],
    data: VDR.DataType,
    unknownFields: Set[Int],
) extends OP {
  def nonceInHex = bytes2Hex(nonce)
}
case class UpdateStorageEntryOP(
    previousEventHash: String,
    data: VDR.DataUpdateType,
    unknownFields: Set[Int],
) extends OP
case class DeactivateStorageEntryOP(
    previousEventHash: String,
    unknownFields: Set[Int],
) extends OP

object CreateDidOP {
  import proto.prism.ProtoCreateDID.DIDCreationData
  def fromProto(p: ProtoCreateDID) = p match
    case ProtoCreateDID(None, unknownFields) => VoidOP("ProtoCreateDID is missing DIDCreationData")
    case ProtoCreateDID(Some(didData), unknownFields) =>
      didData match
        case ProtoCreateDID.DIDCreationData(publicKeys, services, context, unknownFields) =>
          CreateDidOP(
            publicKeys = publicKeys.map(PrismPublicKey.fromProto(_)),
            services = services.map(MyService.fromProto),
            context = context,
          )
}
object UpdateDidOP {
  // AddKeyAction add_key = 1; // Used to add a new key to the DID.
  // RemoveKeyAction remove_key = 2; // Used to remove a key from the DID.
  // AddServiceAction add_service = 3; // Used to add a new service to a DID,
  // RemoveServiceAction remove_service = 4; // Used to remove an existing service from a DID,
  // UpdateServiceAction update_service = 5; // Used to Update a list of service endpoints of a given service on a given DID.
  // PatchContextAction patch_context = 6; // Used to Update a list of `@context` strings used during resolution for a given DID.
  sealed trait Action
  object Action {
    given JsonDecoder[Action] = DeriveJsonDecoder.gen[Action]
    given JsonEncoder[Action] = DeriveJsonEncoder.gen[Action]
  }
  case class VoidAction(reason: String) extends Action
  case class AddKey(key: PrismPublicKey /*(1)*/ ) extends Action
  case class RemoveKey(keyId: String /*(1)*/ ) extends Action
  case class AddService(service: MyService /*(1)*/ ) extends Action
  case class RemoveService(serviceId: String /*(1)*/ ) extends Action
  case class UpdateService(
      serviceId: String /*(1)*/,
      newType: Option[String] /*(2)*/,
      serviceEndpoints: String /*(3)*/
  ) extends Action
  case class PatchContext(context: Seq[String]) extends Action

  def fromProto(p: ProtoUpdateDID) = {
    import proto.prism.UpdateDIDAction.{Action => UAction}
    p match
      case ProtoUpdateDID(previousEventHash, id, actions, unknownFields) =>
        import proto.prism._
        val myAction = actions.map { case UpdateDIDAction(action, unknownFields) =>
          action match
            case UAction.Empty => VoidAction("Action Empty")

            // AddKey
            case UAction.AddKey(AddKeyAction(None, unknownFields)) =>
              VoidAction("Action AddKey with missing key")
            case UAction.AddKey(AddKeyAction(Some(key), unknownFields)) =>
              AddKey(key = PrismPublicKey.fromProto(key))

            // RemoveKey
            case UAction.RemoveKey(RemoveKeyAction("", unknownFields)) =>
              VoidAction("Action RemoveKey with missing keyId")
            case UAction.RemoveKey(RemoveKeyAction(keyId, unknownFields)) =>
              RemoveKey(keyId = keyId)

            // AddService
            case UAction.AddService(AddServiceAction(None, unknownFields)) =>
              VoidAction("Action AddService with missing service")
            case UAction.AddService(AddServiceAction(Some(service), unknownFields)) =>
              AddService(service = MyService.fromProto(service))

            // RemoveService
            case UAction.RemoveService(RemoveServiceAction("", unknownFields)) =>
              VoidAction("Action RemoveService with missing serviceId")
            case UAction.RemoveService(RemoveServiceAction(serviceId, unknownFields)) =>
              RemoveService(serviceId = serviceId)

            // UpdateService
            case UAction.UpdateService(UpdateServiceAction("", newType, serviceEndpoints, unknownFields)) =>
              VoidAction("Action UpdateService with missing serviceId")
            case UAction.UpdateService(UpdateServiceAction(serviceId, newType, serviceEndpoints, unknownFields)) =>
              UpdateService(
                serviceId = serviceId,
                newType = Some(newType).filterNot(_.isEmpty()),
                serviceEndpoints = serviceEndpoints,
              )

            // PatchContext
            case UAction.PatchContext(PatchContextAction(Seq(), unknownFields)) =>
              VoidAction("Action PatchContext with zero patchs")
            case UAction.PatchContext(PatchContextAction(context, unknownFields)) =>
              PatchContext(context)
        }

        UpdateDidOP(
          previousEventHash = bytes2Hex(previousEventHash.toByteArray()),
          id = id,
          actions = myAction,
        )
  }

}
object IssueCredentialBatchOP { def fromProto(p: ProtoIssueCredentialBatch) = ??? } //FIXME
object RevokeCredentialsOP { def fromProto(p: ProtoRevokeCredentials) = ??? } //FIXME
object ProtocolVersionUpdateOP { def fromProto(p: ProtoProtocolVersionUpdate) = ??? } //FIXME
object DeactivateDidOP {
  def fromProto(p: ProtoDeactivateDID) = p match
    case ProtoDeactivateDID(previousEventHash, id, unknownFields) =>
      DeactivateDidOP(previousEventHash = bytes2Hex(previousEventHash.toByteArray()), id = id)
}

object CreateStorageEntryOP {
  def fromProto(p: ProtoCreateStorageEntry) = p match
    case ProtoCreateStorageEntry(didPrismHash, nonce, data, unknownFields) =>
      CreateStorageEntryOP(
        didPrism = DIDPrism.fromEventHash(didPrismHash.toByteArray()),
        nonce = nonce.toByteArray(),
        data = {
          data match
            case ProtoCreateStorageEntry.Data.Empty        => VDR.DataEmpty()
            case ProtoCreateStorageEntry.Data.Bytes(value) => VDR.DataByteArray(value.toByteArray())
            case ProtoCreateStorageEntry.Data.Ipfs(cid)    => VDR.DataIPFS(cid)
        },
        unknownFields = unknownFields.asMap.keySet,
      )
}
object UpdateStorageEntryOP {
  def fromProto(p: ProtoUpdateStorageEntry) = p match
    case ProtoUpdateStorageEntry(previousEventHash, data, unknownFields) =>
      UpdateStorageEntryOP(
        previousEventHash = bytes2Hex(previousEventHash.toByteArray()),
        data = data match {
          case ProtoUpdateStorageEntry.Data.Empty        => VDR.DataEmpty()
          case ProtoUpdateStorageEntry.Data.Bytes(value) => VDR.DataByteArray(value.toByteArray())
          case ProtoUpdateStorageEntry.Data.Ipfs(cid)    => VDR.DataIPFS(cid)
        },
        unknownFields = unknownFields.asMap.keySet,
      )
}

object DeactivateStorageEntryOP {
  def fromProto(p: ProtoDeactivateStorageEntry) = p match
    case ProtoDeactivateStorageEntry(previousEventHash, unknownFields) =>
      DeactivateStorageEntryOP(
        previousEventHash = bytes2Hex(previousEventHash.toByteArray()),
        unknownFields = unknownFields.asMap.keySet,
      )
}
