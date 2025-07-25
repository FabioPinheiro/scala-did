package fmgp.did.method.prism.vdr

import zio._
import zio.json._
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.*
import fmgp.did.method.prism.vdr.*
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import fmgp.did.method.prism.proto.getEventHash

class GenericVDRDriver(
    bfConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
    workdir: String = "../../prism-vdr/mainnet",
    didPrism: DIDPrism,
    keyName: String,
    vdrKey: Secp256k1PrivateKey,
    maybeMsgCIP20: Option[String],
) {
  var globalState = PrismState.empty
//   def refState = Ref.make(globalState)

  def initState: ZIO[Any, Throwable, Unit] = for {
    // stateRef <- ZIO.service[Ref[PrismState]]
    stateRef <- IndexerUtils.loadPrismStateFromChunkFiles
      .provide(
        ZLayer.succeed(IndexerConfig(mBlockfrostConfig = None, workdir))
      )
    state <- stateRef.get
    _ <- ZIO.log(s"Init GenericVDRDriver Service with PrismState (with ${state.ssiCount} SSI)")
  } yield (globalState = state)

  // HACK
  def updateState = ZIO.succeed(globalState) // FIXME

  def createBytesEntry(data: Array[Byte]): ZIO[Any, Throwable, (RefVDR, Int, String)] =
    for {
      // TODO check is in of the type bytes
      // TODO check key
      state <- updateState
      ssi <- state.getSSI(didPrism)
      _ <- ZIO.log("SSI: " + ssi.toJsonPretty)
      _ <- ZIO.unit
      (refVDR, signedPrismOperation) = VDRUtils.createVDREntryBytes(
        didPrism = didPrism,
        vdrKey = vdrKey,
        keyName = keyName,
        data = data,
      )
      _ <- ZIO.log(s"New signedPrismOperation to create $refVDR: ${bytes2Hex(signedPrismOperation.toByteArray)}")
      tx = CardanoService.makeTrasation(
        bfConfig = bfConfig,
        wallet = wallet,
        prismEvents = Seq(signedPrismOperation),
        maybeMsgCIP20,
      )
      _ <- ZIO.log(s"Transation: ${bytes2Hex(tx.serialize)}")
      ret <- CardanoService
        .submitTransaction(tx)
        .provideEnvironment(ZEnvironment(bfConfig))
    } yield (refVDR, ret._1, ret._2)

  def updateBytesEntry(eventRef: RefVDR, data: Array[Byte]): ZIO[Any, Throwable, (EventHash, Int, String)] = {
    for {
      //   stateRef <- ZIO.service[Ref[PrismState]]
      //   state <- stateRef.get
      state <- updateState
      vdrEntry <- state.getVDR(eventRef)
      previousEventHashStr = vdrEntry.latestVDRHash.get // FIXME fix what?
      // TODO check is in of the type bytes
      // TODO check key
      (eventHash, signedPrismOperation) = VDRUtils.updateVDREntryBytes(
        eventRef = eventRef,
        previousEventHash = previousEventHashStr,
        vdrKey = vdrKey,
        keyName = keyName,
        data = data,
      )
      _ <- ZIO.log(s"New signedPrismOperation to update $eventRef: ${bytes2Hex(signedPrismOperation.toByteArray)}")
      tx = CardanoService.makeTrasation(
        bfConfig = bfConfig,
        wallet = wallet,
        prismEvents = Seq(signedPrismOperation),
        maybeMsgCIP20,
      )
      _ <- ZIO.log(s"Transation: ${bytes2Hex(tx.serialize)}")
      ret <- CardanoService
        .submitTransaction(tx)
        .provideEnvironment(ZEnvironment(bfConfig))
    } yield (eventHash, ret._1, ret._2)
  }

  def fetchEntry(eventRef: RefVDR): ZIO[Any, Throwable, VDR] =
    for {
      _ <- ZIO.log(s"Fetch VDR entry $eventRef")
      state <- updateState
      vdrEntry <- state.getVDR(eventRef)
    } yield vdrEntry

  def deactivateEntry(eventRef: RefVDR): ZIO[Any, Throwable, (EventHash, Int, String)] =
    for {
      _ <- ZIO.log(s"Deactivate VDR entry $eventRef")
      state <- updateState
      vdrEntry <- state.getVDR(eventRef)
      previousEventHashStr = vdrEntry.latestVDRHash.get // FIXME fix what?
      // TODO check is in of the type bytes
      // TODO check key
      (eventHash, signedPrismOperation) = VDRUtils.deactivateVDREntry(
        eventRef = eventRef,
        previousEventHash = previousEventHashStr,
        vdrKey = vdrKey,
        keyName = keyName,
      )
      _ <- ZIO.log(s"New signedPrismOperation to deactivate $eventRef: ${bytes2Hex(signedPrismOperation.toByteArray)}")
      tx = CardanoService.makeTrasation(
        bfConfig = bfConfig,
        wallet = wallet,
        prismEvents = Seq(signedPrismOperation),
        maybeMsgCIP20,
      )
      _ <- ZIO.log(s"Transation: ${bytes2Hex(tx.serialize)}")
      ret <- CardanoService
        .submitTransaction(tx)
        .provideEnvironment(ZEnvironment(bfConfig))
    } yield (eventHash, ret._1, ret._2)

}

object GenericVDRDriver {

  def runProgram[E, A](program: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(program).getOrThrowFiberFailure()
    }

}
