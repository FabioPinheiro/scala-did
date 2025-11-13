package fmgp.did.method.prism.vdr

import proto.prism.*
import com.google.protobuf.ByteString
import fmgp.did.method.prism.*
import scala.util.Random
import fmgp.did.method.prism.proto.getEventHash
import fmgp.crypto.SHA256
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.crypto.Secp256k1PrivateKey

protected[vdr] object VDRUtils {

  def createVDREntryBytes(
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      keyName: String,
      data: Array[Byte],
      nonce: Array[Byte] = Random.nextBytes(16),
  ): (RefVDR, SignedPrismEvent) = {
    def op = PrismEvent(
      event = PrismEvent.Event.CreateStorageEntry(
        value = ProtoCreateStorageEntry(
          didPrismHash = ByteString.copyFrom(didPrism.hashRef),
          nonce = ByteString.copyFrom(nonce),
          data = ProtoCreateStorageEntry.Data.Bytes(ByteString.copyFrom(data))
        )
      )
    )
    def signedPrismCreateEventVDR = SignedPrismEvent(
      signedWith = keyName,
      signature = ByteString.copyFrom(vdrKey.sign(op.toByteArray)),
      event = Some(op)
    )
    (RefVDR.fromEventHash(op.getEventHash), signedPrismCreateEventVDR)
  }

  def updateVDREntryBytes(
      eventRef: RefVDR,
      previousEvent: SignedPrismEvent,
      vdrKey: Secp256k1PrivateKey,
      keyName: String,
      data: Array[Byte],
  ): (EventHash, SignedPrismEvent) = {
    updateVDREntryBytes(
      eventRef = eventRef,
      previousEventHash = previousEvent.event.get.getEventHash,
      vdrKey = vdrKey,
      keyName = keyName,
      data = data,
    )
  }

  def updateVDREntryBytes(
      eventRef: RefVDR,
      previousEventHash: EventHash,
      vdrKey: Secp256k1PrivateKey,
      keyName: String,
      data: Array[Byte],
  ): (EventHash, SignedPrismEvent) = {
    def op = PrismEvent(
      event = PrismEvent.Event.UpdateStorageEntry(
        value = ProtoUpdateStorageEntry(
          previousEventHash = ByteString.copyFrom(previousEventHash.byteArray),
          data = ProtoUpdateStorageEntry.Data.Bytes(ByteString.copyFrom(data)),
        )
      ),
    )
    def signedPrismUpdateEventVDR = SignedPrismEvent(
      signedWith = keyName,
      signature = ByteString.copyFrom(vdrKey.sign(op.toByteArray)),
      event = Some(op)
    )
    (op.getEventHash, signedPrismUpdateEventVDR)
  }

  def deactivateVDREntry(
      eventRef: RefVDR,
      previousEvent: SignedPrismEvent,
      vdrKey: Secp256k1PrivateKey,
      keyName: String,
  ): (EventHash, SignedPrismEvent) = deactivateVDREntry(
    eventRef = eventRef,
    previousEventHash = previousEvent.event.get.getEventHash,
    vdrKey = vdrKey,
    keyName = keyName,
  )

  def deactivateVDREntry(
      eventRef: RefVDR,
      previousEventHash: EventHash,
      vdrKey: Secp256k1PrivateKey,
      keyName: String,
  ): (EventHash, SignedPrismEvent) = {
    def op = PrismEvent(
      event = PrismEvent.Event.DeactivateStorageEntry(
        value = ProtoDeactivateStorageEntry(
          previousEventHash = ByteString.copyFrom(previousEventHash.byteArray),
        )
      ),
    )
    def signedPrismUpdateEventVDR = SignedPrismEvent(
      signedWith = keyName,
      signature = ByteString.copyFrom(vdrKey.sign(op.toByteArray)),
      event = Some(op)
    )
    (op.getEventHash, signedPrismUpdateEventVDR)
  }
}
