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
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism._
import fmgp.did.method.prism.cardano._
import fmgp.did.method.prism.proto._
import fmgp.crypto.SHA256

object KeyConstanceUtils {
  val wallet = CardanoWalletConfig()
  val pkMaster = wallet.secp256k1PrivateKey(0, 0) // HDKey(seed, 0, 0).getKMMSecp256k1PrivateKey()
  val pk1VDR = wallet.secp256k1PrivateKey(0, 1) // HDKey(seed, 0, 1).getKMMSecp256k1PrivateKey()
  val pk2VDR = wallet.secp256k1PrivateKey(0, 2) // HDKey(seed, 0, 2).getKMMSecp256k1PrivateKey()
  val pk3VDR = wallet.secp256k1PrivateKey(0, 3) // HDKey(seed, 0, 3).getKMMSecp256k1PrivateKey()
}

object VDRUtilsTestExtra {
  import KeyConstanceUtils._

  def createDID(
      masterKeys: Seq[(String, Secp256k1PrivateKey)],
      vdrKeys: Seq[(String, Secp256k1PrivateKey)]
  ) = DIDExtra.createDID(masterKeys = masterKeys, vdrKeys = vdrKeys)

  def updateDIDAddKey(
      didPrism: DIDPrism,
      previousOperation: SignedPrismOperation,
      masterKeyName: String,
      masterKey: Secp256k1PrivateKey,
      vdrKeyName: String,
      vdrKey: Secp256k1PrivateKey,
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
                      keyData = vdrKey.compressedEcKeyData
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
      vdrKey: Secp256k1PrivateKey,
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
          unknownFields = scalapb.UnknownFieldSet(
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
