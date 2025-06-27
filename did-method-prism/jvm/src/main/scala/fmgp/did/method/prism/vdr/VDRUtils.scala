package fmgp.did.method.prism.vdr

import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey
import fmgp.did.method.prism.*
// import _root_.proto.prism.*
import scala.util.Random
import com.google.protobuf.ByteString
import fmgp.did.method.prism.proto.eventHash
import fmgp.crypto.SHA256
import _root_.proto.prism.*

//FIXME rename, move to shared src
object VDRUtils {

  def createVDREntryBytes(
      didPrism: DIDPrism,
      vdrKey: KMMECSecp256k1PrivateKey,
      keyName: String,
      data: Array[Byte],
      nonce: Array[Byte] = Random.nextBytes(16),
  ): (RefVDR, SignedPrismOperation) = {
    def op = PrismOperation(
      operation = PrismOperation.Operation.CreateStorageEntry(
        value = ProtoCreateStorageEntry(
          didPrismHash = ByteString.copyFrom(didPrism.hashRef),
          nonce = ByteString.copyFrom(nonce),
          data = ProtoCreateStorageEntry.Data.Bytes(ByteString.copyFrom(data))
        )
      )
    )
    def signedPrismCreateEventVDR = SignedPrismOperation(
      signedWith = keyName,
      signature = ByteString.copyFrom(vdrKey.sign(op.toByteArray)),
      operation = Some(op)
    )
    (RefVDR.fromEventHash(signedPrismCreateEventVDR.operation.get.eventHash), signedPrismCreateEventVDR)
  }

  def updateVDREntryBytes(
      eventRef: RefVDR,
      previousOperation: SignedPrismOperation,
      vdrKey: KMMECSecp256k1PrivateKey,
      keyName: String,
      data: Array[Byte],
  ): SignedPrismOperation =
    updateVDREntryBytes(
      eventRef = eventRef,
      previousEventHash = SHA256.digest(previousOperation.operation.get.toByteArray),
      vdrKey = vdrKey,
      keyName = keyName,
      data = data,
    )

  def updateVDREntryBytes(
      eventRef: RefVDR,
      previousEventHash: Array[Byte],
      vdrKey: KMMECSecp256k1PrivateKey,
      keyName: String,
      data: Array[Byte],
  ): SignedPrismOperation = {
    def op = PrismOperation(
      operation = PrismOperation.Operation.UpdateStorageEntry(
        value = ProtoUpdateStorageEntry(
          previousEventHash = ByteString.copyFrom(previousEventHash),
          data = ProtoUpdateStorageEntry.Data.Bytes(ByteString.copyFrom(data)),
        )
      ),
    )
    def signedPrismUpdateEventVDR = SignedPrismOperation(
      signedWith = keyName,
      signature = ByteString.copyFrom(vdrKey.sign(op.toByteArray)),
      operation = Some(op)
    )
    signedPrismUpdateEventVDR
  }
}
