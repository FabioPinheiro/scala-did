package fmgp.did.method.prism

import scala.jdk.CollectionConverters._
import zio._
import zio.stream._
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol
import com.google.protobuf.ByteString
import _root_.proto.prism._
import fmgp.util.bytes2Hex
import fmgp.util.hex2bytes
import fmgp.did.method.prism._
import fmgp.did.method.prism.cardano._
import fmgp.did.method.prism.proto._
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey
import fmgp.crypto.SHA256
import scalapb.UnknownFieldSet

object KeyConstanceUtils {
  val seed = MnemonicHelper.Companion.createSeed(CardanoWalletConfig().mnemonic.asJava, "")
  // FIXME missing the DerivationPath.Companion.fromPath("m/axis1/axis2/")
  val pkMaster = HDKey(seed, 0, 0).getKMMSecp256k1PrivateKey()
  val pk1VDR = HDKey(seed, 0, 1).getKMMSecp256k1PrivateKey()
  val pk2VDR = HDKey(seed, 0, 2).getKMMSecp256k1PrivateKey()
  val pk3VDR = HDKey(seed, 0, 3).getKMMSecp256k1PrivateKey()
}

object VDRUtilsTestExtra {
  import KeyConstanceUtils._

  def createDID(
      masterKeys: Seq[(String, KMMECSecp256k1PrivateKey)],
      vdrKeys: Seq[(String, KMMECSecp256k1PrivateKey)]
  ) = {
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

  def updateDIDAddKey(
      didPrism: DIDPrism,
      previousOperation: SignedPrismOperation,
      masterKeyName: String,
      masterKey: KMMECSecp256k1PrivateKey,
      vdrKeyName: String,
      vdrKey: KMMECSecp256k1PrivateKey,
  ): SignedPrismOperation = {
    val previousEventHash =
      SHA256.digest(previousOperation.operation.get.toByteArray)
    def op = PrismOperation(
      operation = PrismOperation.Operation.UpdateDid(
        value = ProtoUpdateDID(
          previousOperationHash = ByteString.copyFrom(previousEventHash),
          id = didPrism.did,
          actions = Seq(
            UpdateDIDAction(
              action = UpdateDIDAction.Action.AddKey(
                value = AddKeyAction(
                  key = Some(
                    PublicKey(
                      id = vdrKeyName,
                      usage = KeyUsage.VDR_KEY,
                      keyData = PublicKey.KeyData.CompressedEcKeyData(
                        value = CompressedECKeyData(
                          curve = "secp256k1",
                          data = ByteString.copyFrom(vdrKey.getPublicKey().getCompressed())
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
    def signedPrismCreateEventDID = SignedPrismOperation(
      signedWith = masterKeyName,
      signature = ByteString.copyFrom(masterKey.sign(op.toByteArray)),
      operation = Some(op)
    )
    signedPrismCreateEventDID
  }

  /** just for testing purpos */
  def updateVDREntryWithUnknownFields(
      eventRef: RefVDR,
      previousOperation: SignedPrismOperation,
      vdrKey: KMMECSecp256k1PrivateKey,
      keyName: String,
      data: Array[Byte],
      unknownFieldNumber: Int,
  ): SignedPrismOperation = {
    val previousEventHash =
      SHA256.digest(previousOperation.operation.get.toByteArray)
    def op = PrismOperation(
      operation = PrismOperation.Operation.UpdateStorageEntry(
        value = ProtoUpdateStorageEntry(
          previousEventHash = ByteString.copyFrom(previousEventHash),
          data = ProtoUpdateStorageEntry.Data.Bytes(ByteString.copyFrom(data)),
          unknownFields = UnknownFieldSet(
            Map(
              (
                unknownFieldNumber,
                scalapb.UnknownFieldSet.Field(lengthDelimited = Seq(ByteString.EMPTY))
              )
            )
          )
        ),
      ),
    )
    def signedPrismUpdateEventVDR = SignedPrismOperation(
      signedWith = keyName,
      signature = ByteString.copyFrom(KeyConstanceUtils.pk1VDR.sign(op.toByteArray)),
      operation = Some(op)
    )
    signedPrismUpdateEventVDR
  }

}
