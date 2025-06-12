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

// https://github.com/hyperledger-identus/apollo/blob/main/apollo/src/commonMain/kotlin/org/hyperledger/identus/apollo/utils/KMMECSecp256k1PublicKey.kt

object KeyConstanceUtils {
  val seed = MnemonicHelper.Companion.createSeed(CardanoWalletConfig().mnemonic.asJava, "")
  // FIXME missing the DerivationPath.Companion.fromPath("m/axis1/axis2/")
  val pkMaster = HDKey(seed, 0, 0).getKMMSecp256k1PrivateKey()
  val pk1VDR = HDKey(seed, 0, 1).getKMMSecp256k1PrivateKey()
  val pk2VDR = HDKey(seed, 0, 2).getKMMSecp256k1PrivateKey()
  val pk3VDR = HDKey(seed, 0, 3).getKMMSecp256k1PrivateKey()
}

object PrismTestUtils {
  import KeyConstanceUtils._

  def didPrism: DIDPrism = createDID.didPrism.getOrElse(???)

  def signedPrismCreateEventDID = SignedPrismOperation(
    signedWith = "master1",
    signature = ByteString.copyFrom(pkMaster.sign(createDID.toByteArray)),
    operation = Some(createDID)
  )

  def signedPrismCreateEventVDR = SignedPrismOperation(
    signedWith = "vdr1",
    signature = ByteString.copyFrom(KeyConstanceUtils.pk1VDR.sign(createVDR.toByteArray)),
    operation = Some(createVDR)
  )

  def signedPrismUpdateEventVDR = SignedPrismOperation(
    signedWith = "vdr1",
    signature = ByteString.copyFrom(KeyConstanceUtils.pk1VDR.sign(updateVDR1.toByteArray)),
    operation = Some(updateVDR1)
  )

  def createDID = PrismOperation(
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
                usage = KeyUsage.VDR_KEY,
                keyData = PublicKey.KeyData.CompressedEcKeyData(
                  value = CompressedECKeyData(
                    curve = "secp256k1",
                    data = ByteString.copyFrom(pk1VDR.getPublicKey().getCompressed())
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

  def createVDR = PrismOperation(
    operation = PrismOperation.Operation.CreateStorageEntry(
      value = ProtoCreateStorageEntry(
        didPrismHash = ByteString.copyFrom(didPrism.hashRef),
        nonce = ByteString.EMPTY,
        data = ProtoCreateStorageEntry.Data.Bytes(
          ByteString.copyFrom(hex2bytes("00ff11"))
        )
      )
    )
  )

  def updateVDR1 = PrismOperation(
    operation = PrismOperation.Operation.UpdateStorageEntry(
      value = ProtoUpdateStorageEntry(
        previousOperationHash = ByteString.copyFrom(createVDR.eventHash),
        data = ProtoUpdateStorageEntry.Data.Bytes(ByteString.copyFrom(hex2bytes("3300ffcc"))),
      )
    ),
  )

  // val aux = MaybeOperation
  //   .fromProto(
  //     tx = "String",
  //     blockIndex = 0,
  //     prismObject = PrismObject(
  //       blockContent = Some(
  //         PrismBlock(
  //           operations = Seq(PrismTestUtils.signedPrismCreateEventDID)
  //         )
  //       )
  //     ),
  //   )
  //   .head
  //   .asInstanceOf[fmgp.prism.MySignedPrismOperation[fmgp.prism.CreateDidOP]]

}

/** didResolverPrismJVM/Test/runMain fmgp.did.method.prism.MainVDR */
@main def MainVDR() = {
  println("*" * 100)
  println("Main VDR")

  val e1 = PrismTestUtils.signedPrismCreateEventDID
  val e2 = PrismTestUtils.signedPrismCreateEventVDR
  val e3 = PrismTestUtils.signedPrismUpdateEventVDR

  // println("CreateDID: " + bytes2Hex(PrismTestUtils.createDID.toByteArray))
  println("signatureCreateDID: " + bytes2Hex(e1.signature.toByteArray))
  println("signedPrismCreateEventDID: " + bytes2Hex(e1.toByteArray))
  println(PrismTestUtils.createDID.didPrism.getOrElse(???).string)
  // println("PrismOperation: " + bytes2Hex(PrismTestUtils.createVDR.toByteArray))
  println("signature CreateEventVDR: " + bytes2Hex(e2.signature.toByteArray))
  println("signedPrismOperation: " + bytes2Hex(e2.toByteArray))

  println("signature UpdateEventVDR: " + bytes2Hex(e3.signature.toByteArray))
  println("signedPrismOperation: " + bytes2Hex(e3.toByteArray))

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
