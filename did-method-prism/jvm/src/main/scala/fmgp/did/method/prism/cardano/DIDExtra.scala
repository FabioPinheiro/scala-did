package fmgp.did.method.prism.cardano

import proto.prism.*
import com.google.protobuf.ByteString
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.proto.didPrism

object DIDExtra {

  def createDID(
      masterKeys: Seq[(String, Secp256k1PrivateKey)],
      vdrKeys: Seq[(String, Secp256k1PrivateKey)]
  ): (DIDPrism, SignedPrismOperation) = {
    def op = PrismOperation(
      operation = PrismOperation.Operation.CreateDid(
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
    def signedPrismCreateEventDID = SignedPrismOperation(
      signedWith = masterKeys.head._1,
      signature = ByteString.copyFrom(masterKeys.head._2.sign(op.toByteArray)),
      operation = Some(op)
    )
    def didPrism: DIDPrism = op.didPrism.getOrElse(???)
    (didPrism, signedPrismCreateEventDID)
  }
}
