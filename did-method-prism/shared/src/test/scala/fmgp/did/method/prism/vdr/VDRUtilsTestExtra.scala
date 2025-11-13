package fmgp.did.method.prism.vdr

import scala.jdk.CollectionConverters._
import zio._
import zio.stream._

import com.google.protobuf.ByteString
import fmgp.util.bytes2Hex
import fmgp.util.hex2bytes
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism._
import fmgp.did.method.prism.cardano._
import fmgp.did.method.prism.proto._
import fmgp.crypto.SHA256
import _root_.proto.prism._

object VDRUtilsTestExtra {
  import KeyConstanceUtils._

  val didPrism = DIDPrism("51d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4")
  val refVDR = RefVDR("2a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7b")

  val createSSI =
    "0a076d61737465723112473045022100fd1f2ea66ea9e7f37861dbe1599fb12b7ca3297e9efa872504bfc54f1daebec502205f0152d45b266b5524d2fe8eb38aaa1d3e78dc053b4f50d98fe4564f50c4c0da1a7b0a790a77123b0a076d61737465723110014a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f12380a047664723110084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f"
  val updateSSI_addKey =
    "0a076d61737465723112463044022053718b0ed7499ea6c39d76f3b88474cc682b4cc0b984047c9f3a6abd690842f502201aaa4ef7f61bdebb6235c810df4129b52cf538077d9b67886768a39710fc093a1aaf0112ac010a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4124a6469643a707269736d3a353164343762313333393361376363356331616663343730393964636265636363663063386137303832386330373261633832663535323235623432643466341a3c0a3a0a380a047664723210084a2e0a09736563703235366b311221028210fd4c42b148df2b908eb6a5c507822f63c440facc283b30c84859fde2e30f"

  val createVDR = // Create Bytes data1
    "0a0476647231124730450221008aa8a6f66a57d28798b24540dbf0a93772ff317f7bd8969a0ca3a98cec7ff9d4022016336c0c8b9fc82198b661f85468f53c1b4dc0cdd23b44dc0d17853aa50944161a283a260a2051d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4a2060101"
  val updateVDR = // Update Bytes data2
    "0a047664723112473045022100d07451415fecbe92f270ecd0371ee181cabd198e781759287e5122d688a9c97102202273b49e43a1f2022940f48012b5e9839f99eaacf7429c417ff67ec64e58b51b1a2a422812202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7ba20603020304"
  val updateVDR_withUnknownField49 = // Update Bytes data2
    "0a04766472311246304402206e6152ba20935eb946e79b4d39cd6cf327b162b25d37b5cc2e4df3ce9e44dd9f0220729a0891b714e6116838cf4dd2dca3e877b6e36a25814e7400fd35cb1da6084a1a2d422b12202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7ba206030203048a0300"
  val updateVDR_withUnknownField99 = // Update Bytes data2
    "0a047664723112463044022008ce25d2a5731b5946df3c57c3d4046a499830603680288b7c3c14d0b6e3d12302205c9f064f0a488e86377b0112448316827a6d356884e218a148db3d3198bf7c051a2d422b12202a0d49ff70f6403cab5bba090478300369ff4875f849dd42c9c59ca4272a9a7ba206030203049a0600"
  val updateVDR_withTheNewKey = // Update Bytes with data3 after frist updateVDR
    "0a047664723212473045022100b33c0b12449eb506c862c98cfbd221d48cbf31ba62e512b4af31170a34a62e2a0220329bbc57107491b62ecfb894cda5757cc221421ecccae61a7404d3d0ae1d01d41a2842261220d7c38f7d8aa4912d0a58ead87b154eed968b949b3bfb54f3f894ab9fc5365f40a2060105"
  val deactivateVDR_afterUpdate = // after frist updateVDR
    "0a047664723112463044022012ef278a68a4a04b72c5972d5e3cd72e8c798b9250cff5c52bee78297c06e19002205d267333f7d91bb99f4282de55ba974d6630776059758d918e13caf8b2f34ab01a244a221220d7c38f7d8aa4912d0a58ead87b154eed968b949b3bfb54f3f894ab9fc5365f40"

  val data1 = hex2bytes("01")
  val data2 = hex2bytes("020304")
  val data3 = hex2bytes("05")

  def makeSignedPrismEvent(
      hex: String,
      opIndex: Int,
      blockIndex: Int = 0,
      tx: String = "tx"
  ): MySignedPrismEvent[OP] = {
    val protoBytes = hex2bytes(hex)
    val signedPrismEvent = SignedPrismEvent.parseFrom(protoBytes)
    MaybeEvent.fromProto(signedPrismEvent, tx, blockIndex = blockIndex, opIndex = opIndex) match
      case InvalidPrismObject(tx, b, reason)                                   => ???
      case InvalidSignedPrismEvent(tx, b, o, reason)                           => ???
      case obj @ MySignedPrismEvent(tx, b, o, signedWith, signature, protobuf) => obj
  }

  def createDID(
      masterKeys: Seq[(String, Secp256k1PrivateKey)],
      vdrKeys: Seq[(String, Secp256k1PrivateKey)]
  ) = DIDExtra.createDID(masterKeys = masterKeys, vdrKeys = vdrKeys)

  def updateDIDAddKey(
      didPrism: DIDPrism,
      previousEvent: SignedPrismEvent,
      masterKeyName: String,
      masterKey: Secp256k1PrivateKey,
      vdrKeyName: String,
      vdrKey: Secp256k1PrivateKey,
  ): SignedPrismEvent = {
    val previousEventHash =
      SHA256.digest(previousEvent.event.get.toByteArray)
    def op = PrismEvent(
      event = PrismEvent.Event.UpdateDid(
        value = ProtoUpdateDID(
          previousEventHash = ByteString.copyFrom(previousEventHash),
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
    def signedPrismCreateEventDID = SignedPrismEvent(
      signedWith = masterKeyName,
      signature = ByteString.copyFrom(masterKey.sign(op.toByteArray)),
      event = Some(op)
    )
    signedPrismCreateEventDID
  }

  /** just for testing purpos */
  def updateVDREntryWithUnknownFields(
      eventRef: RefVDR,
      previousEvent: SignedPrismEvent,
      vdrKey: Secp256k1PrivateKey,
      keyName: String,
      data: Array[Byte],
      unknownFieldNumber: Int,
  ): SignedPrismEvent = {
    val previousEventHash =
      SHA256.digest(previousEvent.event.get.toByteArray)
    def op = PrismEvent(
      event = PrismEvent.Event.UpdateStorageEntry(
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
    def signedPrismUpdateEventVDR = SignedPrismEvent(
      signedWith = keyName,
      signature = ByteString.copyFrom(vdrKey.sign(op.toByteArray)),
      event = Some(op)
    )
    signedPrismUpdateEventVDR
  }

}
