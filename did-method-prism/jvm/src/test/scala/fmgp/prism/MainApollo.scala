package fmgp.prism

import scala.jdk.CollectionConverters._

import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey
import fmgp.util.bytes2Hex
import fmgp.util.hex2bytes
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol
import com.google.protobuf.ByteString
import proto.prism._

// https://github.com/hyperledger-identus/apollo/blob/main/apollo/src/commonMain/kotlin/org/hyperledger/identus/apollo/utils/KMMECSecp256k1PublicKey.kt

/** didResolverPrismJVM/Test/runMain fmgp.prism.MainApollo */
@main def MainApollo() = {
  println("*" * 100)
  println("Main VDR Apollo")

  val seed = MnemonicHelper.Companion.createSeed(CardanoWalletConfig().mnemonic.asJava, "")
  // FIXME missing the DerivationPath.Companion.fromPath("m/axis1/axis2/")
  val pkMaster = HDKey(seed, 0, 0).getKMMSecp256k1PrivateKey()
  val pkVDR = HDKey(seed, 0, 1).getKMMSecp256k1PrivateKey()
  //   println(bytes2Hex(pubK.getCurvePoint().getX()))
  //   println(bytes2Hex(pubK.getCurvePoint().getY()))
  //   println(bytes2Hex(pubK.getCompressed()))

  val createDID = PrismOperation(
    operation = PrismOperation.Operation.CreateDid(
      value = ProtoCreateDID(
        didData = Some(
          ProtoCreateDID.DIDCreationData(
            publicKeys = Seq(
              PublicKey(
                id = "master1",
                usage = KeyUsage.MASTER_KEY,
                keyData = PublicKey.KeyData.CompressedEcKeyData(
                  value = CompressedECKeyData(
                    curve = "secp256k1",
                    data = ByteString.copyFrom(pkMaster.getPublicKey().getCompressed())
                  )
                )
              ),
              PublicKey(
                id = "vdr1",
                usage = KeyUsage.MASTER_KEY,
                keyData = PublicKey.KeyData.CompressedEcKeyData(
                  value = CompressedECKeyData(
                    curve = "secp256k1",
                    data = ByteString.copyFrom(pkVDR.getPublicKey().getCompressed())
                  )
                )
              )
            ),
            services = Seq.empty, // Seq[proto.prism.Service],
            context = Seq.empty // Seq[String]
          )
        )
      )
    )
  )

  println("CreateDID: " + bytes2Hex(createDID.toByteArray))
  val signatureCreateDID = pkMaster.sign(createDID.toByteArray)
  println("signatureCreateDID: " + bytes2Hex(signatureCreateDID))
  val signedPrismOperationCreateDID = SignedPrismOperation(
    signedWith = "master1",
    signature = ByteString.copyFrom(signatureCreateDID),
    operation = Some(createDID)
  )
  println("signedPrismOperationCreateDID: " + bytes2Hex(signedPrismOperationCreateDID.toByteArray))
  val aux = MaybeOperation
    .fromProto(
      tx = "String",
      blockIndex = 0,
      prismObject = PrismObject(
        blockContent = Some(
          PrismBlock(
            operations = Seq(signedPrismOperationCreateDID)
          )
        )
      ),
    )
    .head
    .asInstanceOf[fmgp.prism.MySignedPrismOperation[fmgp.prism.CreateDidOP]]
  val ssi = SSI.fromCreateEvent(aux)
  println(ssi.didPrism.string)

  val createVDR = PrismOperation(
    operation = PrismOperation.Operation.CreateStorageEntry(
      value = ProtoCreateStorageEntry(
        didPrismHash = ByteString.copyFrom(ssi.didPrism.hashRef),
        nonce = ByteString.EMPTY,
        data = ProtoCreateStorageEntry.Data.Bytes(
          ByteString.copyFrom(hex2bytes("00ff11"))
        )
      )
    )
  )

  println("PrismOperation: " + bytes2Hex(createVDR.toByteArray))

  val signatureCreateVDR = pkVDR.sign(createVDR.toByteArray)
  println("signature: " + bytes2Hex(signatureCreateVDR))

  val signedPrismOperationCreateVDR = SignedPrismOperation(
    signedWith = "vdr1",
    signature = ByteString.copyFrom(signatureCreateVDR),
    operation = Some(createVDR)
  )
  println("signedPrismOperation: " + bytes2Hex(signedPrismOperationCreateVDR.toByteArray))

}
