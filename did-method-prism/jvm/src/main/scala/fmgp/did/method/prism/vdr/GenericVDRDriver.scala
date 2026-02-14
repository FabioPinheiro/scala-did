package fmgp.did.method.prism.vdr

import zio.*
import zio.json.*
import proto.prism.PrismBlock
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.*
import fmgp.did.method.prism.vdr.*
import fmgp.did.method.prism.cardano.{CardanoWalletConfig, TxHash}
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import fmgp.did.method.prism.proto.getEventHash

//FIXME @deprecated("deprecated in favor of VDRPassiveService VDRService", "0.1.0-M28")
class GenericVDRDriver(
    bfConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
    workdir: String, // "../../prism-vdr/mainnet",
    didPrism: DIDPrism,
    keyName: String,
    vdrKey: Secp256k1PrivateKey,
    maybeMsgCIP20: Option[String],
) {
  var globalState: PrismStateInMemory = throw new RuntimeException(
    "DRIVER was not initially. You need to run the '.initState' ZIo program first"
  )

  def initState: ZIO[Any, Throwable, Unit] = for {
    // stateRef <- ZIO.service[Ref[PrismState]]
    state <- PrismStateInMemory.empty
    _ <- IndexerUtils.loadPrismStateFromChunkFiles
      .provide(ZLayer.succeed(IndexerConfig(mBlockfrostConfig = None, workdir)) ++ ZLayer.succeed(state))
    _ <- ZIO.log(s"Init GenericVDRDriver Service with PrismState (with ${state.ssiCount} SSI)")
  } yield (globalState = state)

  // HACK
  def updateState = ZIO.succeed(globalState) // FIXME

  def createBytesEntry(data: Array[Byte]): ZIO[Any, Throwable, (RefVDR, TxHash)] = {
    for {
      // TODO check is in of the type bytes
      // TODO check key
      state <- updateState
      ssi <- state.getSSI(didPrism)
      _ <- ZIO.log("SSI: " + ssi.toJsonPretty)
      (refVDR, signedPrismEvent) =
        VDRUtils.createVDREntryBytes(
          didPrism = didPrism,
          vdrKey = vdrKey,
          keyName = keyName,
          data = data,
        )
      _ <- ZIO.log(s"New signedPrismEvent to create $refVDR: ${bytes2Hex(signedPrismEvent.toByteArray)}")
      tx <- ScalusService.makeTrasation(prismEvents = Seq(signedPrismEvent), maybeMsgCIP20)
      _ <- ZIO.log(s"Transation: ${bytes2Hex(tx.toCbor)}")
      txHash <- ScalusService.submitTransaction(tx)
    } yield (refVDR, txHash)
  }.provideEnvironment(ZEnvironment(bfConfig) ++ ZEnvironment(wallet))

  def updateBytesEntry(eventRef: RefVDR, data: Array[Byte]): ZIO[Any, Throwable, (EventHash, TxHash)] = {
    for {
      //   stateRef <- ZIO.service[Ref[PrismState]]
      //   state <- stateRef.get
      state <- updateState
      vdrEntry <- state.getVDR(eventRef)
      previousEventHashStr = vdrEntry.latestVDRHash.get // FIXME fix what?
      // TODO check is in of the type bytes
      // TODO check key
      (eventHash, signedPrismEvent) = VDRUtils.updateVDREntryBytes(
        eventRef = eventRef,
        previousEventHash = previousEventHashStr,
        vdrKey = vdrKey,
        keyName = keyName,
        data = data,
      )
      _ <- ZIO.log(s"New signedPrismEvent to update $eventRef: ${bytes2Hex(signedPrismEvent.toByteArray)}")
      tx <- ScalusService.makeTrasation(prismEvents = Seq(signedPrismEvent), maybeMsgCIP20)
      _ <- ZIO.log(s"Transation: ${bytes2Hex(tx.toCbor)}")
      txHash <- ScalusService.submitTransaction(tx)
    } yield (eventHash, txHash)
  }.provideEnvironment(ZEnvironment(bfConfig) ++ ZEnvironment(wallet))

  def fetchEntry(eventRef: RefVDR): ZIO[Any, Throwable, VDR] =
    for {
      _ <- ZIO.log(s"Fetch VDR entry $eventRef")
      state <- updateState
      vdrEntry <- state.getVDR(eventRef)
    } yield vdrEntry

  def deactivateEntry(eventRef: RefVDR): ZIO[Any, Throwable, (EventHash, TxHash)] = {
    for {
      _ <- ZIO.log(s"Deactivate VDR entry $eventRef")
      state <- updateState
      vdrEntry <- state.getVDR(eventRef)
      previousEventHashStr = vdrEntry.latestVDRHash.get // FIXME fix what?
      // TODO check is in of the type bytes
      // TODO check key
      (eventHash, signedPrismEvent) = VDRUtils.deactivateVDREntry(
        eventRef = eventRef,
        previousEventHash = previousEventHashStr,
        vdrKey = vdrKey,
        keyName = keyName,
      )
      _ <- ZIO.log(s"New signedPrismEvent to deactivate $eventRef: ${bytes2Hex(signedPrismEvent.toByteArray)}")
      tx <- ScalusService.makeTrasation(
        prismEvents = Seq(signedPrismEvent),
        maybeMsgCIP20,
      )
      _ <- ZIO.log(s"Transation: ${bytes2Hex(tx.toCbor)}")
      txHash <- ScalusService
        .submitTransaction(tx)
        .provideEnvironment(ZEnvironment(bfConfig))
    } yield (eventHash, txHash)
  }.provideEnvironment(ZEnvironment(bfConfig) ++ ZEnvironment(wallet))

}
