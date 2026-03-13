package fmgp.did.method.prism.cardano

import proto.prism.*
import com.google.protobuf.ByteString
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.proto.*
import fmgp.util.bytes2Hex

object DIDExtra {

  def createDID(
      masterKeys: Seq[(String, Secp256k1PrivateKey)],
      vdrKeys: Seq[(String, Secp256k1PrivateKey)]
  ): (DIDPrism, SignedPrismEvent) = {
    def op = PrismEvent(
      event = PrismEvent.Event.CreateDid(
        value = ProtoCreateDID(
          didData = Some(
            ProtoCreateDID.DIDCreationData(
              publicKeys = Seq.empty ++
                masterKeys.map { (keyName, pk) =>
                  PublicKey(
                    id = keyName,
                    usage = KeyUsage.MASTER_KEY,
                    keyData = pk.compressedKeyData
                  )
                } ++
                vdrKeys.map { (keyName, pk) =>
                  PublicKey(
                    id = keyName,
                    usage = KeyUsage.VDR_KEY,
                    keyData = pk.compressedKeyData
                  )
                },
              services = Seq.empty, // Seq[proto.prism.Service],
              context = Seq.empty // Seq[String]
            )
          )
        )
      )
    )
    def signedPrismCreateEventDID = SignedPrismEvent(
      signedWith = masterKeys.head._1,
      signature = ByteString.copyFrom(masterKeys.head._2.sign(op.toByteArray)),
      event = Some(op)
    )
    def didPrism: DIDPrism = op.didPrism.getOrElse(???)
    (didPrism, signedPrismCreateEventDID)
  }

  /* https://github.com/input-output-hk/prism-did-method-spec/blob/main/extensions/deterministic-prism-did-generation-proposal.md */
  def createDeterministicDID(
      masterKey: Secp256k1PrivateKey,
      actions: Seq[UpdateDidOP.Action],
  ): (DIDPrism, SignedPrismEvent, Option[SignedPrismEvent]) = {
    val createEvent: PrismEvent = PrismEvent(
      event = PrismEvent.Event.CreateDid(
        value = ProtoCreateDID(
          didData = Some(
            ProtoCreateDID.DIDCreationData(
              publicKeys = Seq(
                PublicKey(
                  id = Cip0000.keyLabel, // keyName
                  usage = KeyUsage.MASTER_KEY,
                  keyData = masterKey.compressedKeyData
                )
              ),
              services = Seq.empty, // Seq[proto.prism.Service],
              context = Seq.empty // Seq[String]
            )
          )
        )
      )
    )
    val signedPrismCreateEventDID = SignedPrismEvent(
      signedWith = Cip0000.keyLabel,
      signature = ByteString.copyFrom(masterKey.sign(createEvent.toByteArray)),
      event = Some(createEvent)
    )
    val didPrism: DIDPrism = createEvent.didPrism.getOrElse(???) // '???' is ok

    val signedPrismUpdateEventDID =
      if (actions.isEmpty) None
      else
        Some {
          val updateEvent: PrismEvent =
            PrismEvent(
              event = PrismEvent.Event.UpdateDid(
                value = ProtoUpdateDID(
                  previousEventHash = createEvent.getEventHash.byteString,
                  id = { // id = "", // defualt (not in use)
                    // See https://github.com/input-output-hk/prism-did-method-spec/issues/70
                    assert(didPrism.specificId == bytes2Hex(createEvent.getEventHash.byteArray))
                    didPrism.specificId
                  },
                  actions = actions.map(_.toProto)
                )
              )
            )

          val signedPrismUpdateEventDID = SignedPrismEvent(
            signedWith = Cip0000.keyLabel,
            signature = ByteString.copyFrom(masterKey.sign(updateEvent.toByteArray)),
            event = Some(updateEvent)
          )
          // println(MaybeEvent.fromProtoForce2DIDEvent(signedPrismUpdateEventDID).view.operation.asDebugJson)
          signedPrismUpdateEventDID
        }

    (didPrism, signedPrismCreateEventDID, signedPrismUpdateEventDID)
  }
}
