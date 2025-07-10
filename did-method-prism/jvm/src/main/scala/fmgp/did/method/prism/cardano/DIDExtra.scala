package fmgp.did.method.prism.cardano

import proto.prism.*
import com.google.protobuf.ByteString
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.proto.didPrism
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey

object DIDExtra {

  def createDID(
      masterKeys: Seq[(String, KMMECSecp256k1PrivateKey)],
      vdrKeys: Seq[(String, KMMECSecp256k1PrivateKey)]
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
                    keyData = PublicKey.KeyData.CompressedEcKeyData(
                      value = CompressedECKeyData(
                        curve = "secp256k1",
                        data = ByteString.copyFrom(pk.getPublicKey().getCompressed())
                      )
                    )
                  )
                } ++
                vdrKeys.map { (keyName, pk) =>
                  PublicKey(
                    id = keyName,
                    usage = KeyUsage.VDR_KEY,
                    keyData = PublicKey.KeyData.CompressedEcKeyData(
                      value = CompressedECKeyData(
                        curve = "secp256k1",
                        data = ByteString.copyFrom(pk.getPublicKey().getCompressed())
                      )
                    )
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
