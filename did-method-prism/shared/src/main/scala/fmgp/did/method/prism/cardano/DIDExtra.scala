package fmgp.did.method.prism.cardano

import proto.prism.*
import com.google.protobuf.ByteString
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.proto.*

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
                    keyData = pk.compressedEcKeyData
                  )
                } ++
                vdrKeys.map { (keyName, pk) =>
                  PublicKey(
                    id = keyName,
                    usage = KeyUsage.VDR_KEY,
                    keyData = pk.compressedEcKeyData
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
      masterKey: (String /*keyName*/, Secp256k1PrivateKey),
      actions: Seq[UpdateDidOP.Action],
  ): (DIDPrism, SignedPrismEvent, SignedPrismEvent) = {
    val createEvent: PrismEvent = PrismEvent(
      event = PrismEvent.Event.CreateDid(
        value = ProtoCreateDID(
          didData = Some(
            ProtoCreateDID.DIDCreationData(
              publicKeys = Seq(
                PublicKey(
                  id = masterKey._1, // keyName
                  usage = KeyUsage.MASTER_KEY,
                  keyData = masterKey._2.compressedEcKeyData
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
      signedWith = masterKey._1,
      signature = ByteString.copyFrom(masterKey._2.sign(createEvent.toByteArray)),
      event = Some(createEvent)
    )
    val updateEvent: PrismEvent =
      PrismEvent(
        event = PrismEvent.Event.UpdateDid(
          value = ProtoUpdateDID(
            previousEventHash = createEvent.getEventHash.byteString,
            id = "", // defualt (not in use)
            actions = actions.map(_.toProto)
          )
        )
      )
    val signedPrismUpdateEventDID = SignedPrismEvent(
      signedWith = masterKey._1,
      signature = ByteString.copyFrom(masterKey._2.sign(updateEvent.toByteArray)),
      event = Some(updateEvent)
    )

    val didPrism: DIDPrism = createEvent.didPrism.getOrElse(???) // '???' is ok

    (didPrism, signedPrismCreateEventDID, signedPrismUpdateEventDID)
  }
}
