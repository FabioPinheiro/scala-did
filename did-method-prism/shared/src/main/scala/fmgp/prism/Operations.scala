package fmgp.prism

import zio.json._
import fmgp.util.bytes2Hex
import proto.prism.CreateDIDOperation
import proto.prism.UpdateDIDOperation
import proto.prism.IssueCredentialBatchOperation
import proto.prism.RevokeCredentialsOperation
import proto.prism.ProtocolVersionUpdateOperation
import proto.prism.DeactivateDIDOperation
import proto.prism.CreateStorageEntryOperation
import proto.prism.UpdateStorageEntryOperation

sealed trait OP
object OP {
  given JsonDecoder[OP] = DeriveJsonDecoder.gen[OP]
  given JsonEncoder[OP] = DeriveJsonEncoder.gen[OP]
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

/** @param previous_operation_hash
  *   (1) - The hash of the most recent operation that was used to create or update the DID.
  * @param id
  *   (2) - exclude TODO: To be redefined after we start using this operation.
  * @param actions
  *   (3) The actual updates to perform on the DID.
  */
case class UpdateDidOP(previousOperationHash: String, id: String, actions: Seq[UpdateDidOP.Action]) extends OP {
  def previousOpHashHex = previousOperationHash
}

case class IssueCredentialBatchOP(value: String) extends OP
case class RevokeCredentialsOP(value: String) extends OP
case class ProtocolVersionUpdateOP(value: String) extends OP

/** @param previous_operation_hash
  *   (1) - The hash of the most recent operation that was used to create or update the DID.
  * @param id
  *   (2) - DID Suffix of the DID to be deactivated
  */
case class DeactivateDidOP(previousOperationHash: String, id: String) extends OP
case class CreateStorageEntryOP(value: String) extends OP
case class UpdateStorageEntryOP(value: String) extends OP

object CreateDidOP {
  import proto.prism.CreateDIDOperation.DIDCreationData
  def fromProto(p: CreateDIDOperation) = p match
    case CreateDIDOperation(None, unknownFields) => VoidOP("CreateDIDOperation is missing DIDCreationData")
    case CreateDIDOperation(Some(didData), unknownFields) =>
      didData match
        case DIDCreationData(publicKeys, services, context, unknownFields) =>
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

  def fromProto(p: UpdateDIDOperation) = {
    import proto.prism.UpdateDIDAction.{Action => UAction}
    p match
      case UpdateDIDOperation(previousOperationHash, id, actions, unknownFields) =>
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
          previousOperationHash = bytes2Hex(previousOperationHash.toByteArray()),
          id = id,
          actions = myAction,
        )
  }

}
object IssueCredentialBatchOP { def fromProto(p: IssueCredentialBatchOperation) = ??? } //FIXME
object RevokeCredentialsOP { def fromProto(p: RevokeCredentialsOperation) = ??? } //FIXME
object ProtocolVersionUpdateOP { def fromProto(p: ProtocolVersionUpdateOperation) = ??? } //FIXME
object DeactivateDidOP {
  def fromProto(p: DeactivateDIDOperation) = p match
    case DeactivateDIDOperation(previousOperationHash, id, unknownFields) =>
      DeactivateDidOP(previousOperationHash = bytes2Hex(previousOperationHash.toByteArray()), id = id)
}
object CreateStorageEntryOP { def fromProto(p: CreateStorageEntryOperation) = ??? } //FIXME
object UpdateStorageEntryOP { def fromProto(p: UpdateStorageEntryOperation) = ??? } //FIXME
