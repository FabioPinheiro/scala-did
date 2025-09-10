/*
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

// https://github.com/hyperledger-identus/apollo/blob/main/apollo/src/commonMain/kotlin/org/hyperledger/identus/apollo/utils/KMMECSecp256k1PublicKey.kt

/** didResolverPrismJVM/Test/runMain fmgp.did.method.prism.MainVDR */
@main def MainVDR() = {
  println("*" * 100)
  println("Main VDR")

  import KeyConstanceUtils._
  import PrismTestUtils._

  val (didPrism, e1) = createDID(Seq(("master1", pkMaster)), Seq(("vdr1", pk1VDR)))
  val (refVDR, e2) = createVDREntry(didPrism, pk1VDR, "vdr1", hex2bytes("00ff11"))
  val e3 = updateVDREntry(refVDR, e2, pk1VDR, "vdr1", hex2bytes("3300ffcc"))
  val e4 = updateDIDAddKey(
    didPrism = didPrism,
    previousEvent = e1,
    masterKeyName = "master1",
    masterKey = pkMaster,
    vdrKeyName = "vdr2",
    vdrKey = pk2VDR,
  )
  val e5 = updateVDREntry(refVDR, e3, pk1VDR, "vdr1", hex2bytes("aa0a"))

  // println("CreateDID: " + bytes2Hex(PrismTestUtils.createDID.toByteArray))
  // println("signatureCreateDID: " + bytes2Hex(e1.signature.toByteArray))
  println("signedPrismCreateEventDID (create DID):\n" + bytes2Hex(e1.toByteArray))
  println(didPrism.string)
  // println("PrismEvent: " + bytes2Hex(PrismTestUtils.createVDR.toByteArray))
  // println("signature CreateEventVDR: " + bytes2Hex(e2.signature.toByteArray))
  println("signedPrismEvent (create VDR):\n" + bytes2Hex(e2.toByteArray))

  // println("signature UpdateEventVDR: " + bytes2Hex(e3.signature.toByteArray))
  println("signedPrismEvent (update VDR):\n" + bytes2Hex(e3.toByteArray))
  println("signedPrismEvent (add key to DID):\n" + bytes2Hex(e4.toByteArray))
  println("signedPrismEvent (update VDR with the new key):\n" + bytes2Hex(e5.toByteArray))

  val program = for {
    _ <- ZIO.log("""### MainVDR program ###""".stripMargin)
    sink = {
      import java.nio.file.StandardOpenOption.*
      ZSink
        .fromFileName(name = "vdr_data_example", options = Set(WRITE, APPEND, CREATE, SYNC))
        .contramapChunks[String](_.flatMap { e => (e + "\n").getBytes })
        .ignoreLeftover // TODO review
        .mapZIO(bytes => ZIO.log(s"ZSink PRISM Events into 'vdr_data_example' (write $bytes bytes)"))
    }
    stream = ZStream.fromIterable(
      Seq(e1, e2, e3).map(event => bytes2Hex(event.toByteArray))
    )
    _ <- stream.run(sink)
  } yield ()

  Unsafe.unsafe { implicit unsafe => // Run side effect
    Runtime.default.unsafe
      .run(program) // .provide(Operations.layerOperations ++ DidPeerResolver.layer))
      .getOrThrowFiberFailure()
  }

}
 */
