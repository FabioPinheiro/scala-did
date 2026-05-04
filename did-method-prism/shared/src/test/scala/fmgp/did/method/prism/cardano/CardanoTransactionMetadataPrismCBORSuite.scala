package fmgp.did.method.prism.cardano

import munit.*
import zio.json.*
import fmgp.did.DIDDocument
import fmgp.util.Base64

import io.bullet.borer.Cbor
import io.bullet.borer.{Decoder, Encoder}

class CardanoTransactionMetadataPrismCBORSuite extends FunSuite {

  test("Metadata 1 CBOR") {
    val metadata =
      "a119534da26176016163855840100222b0021295010a076d617374657230124730450220622b4fdaa18f835856378a58bb04697afab180963a1e38c3a163743f809ba0b4022100a6124d55ab2958403b83f99be3012e2c0001ab72f13b726532831d43f2739a395b851a410a3f0a3d123b0a076d61737465723010014a2e0a09736563703235366b311221026841f958407c1a4c46859496f9c1267772793107b0e3f8f65102fe6e983253ed54e71295010a076d61737465723012473045022100e898f57100ed8aa015ba352d861a9aa15840bcf1e3d5a6548badd0665b9f30a4181202206f53548d7d3923d024ae8283aadeb8e602266b19f622f54672d22038781dbb261a410a3f0a3d123b0a076d617374583565723010014a2e0a09736563703235366b3112210305ba6022fccbce1f5152ab91b39fc5187b2ecca87a413371dd16242ab348a15b"

    // https://cbor.me/
    val bytes = fmgp.util.hex2bytes(metadata)
    val out = Cbor
      .decode(bytes)
      // .withPrintLogging()
      .to[CardanoTransactionMetadataPrismCBOR]
      .valueEither

    assert(out.isRight)
  }

  /** Reproduces the SimulateView "Simulation failed: an implementation is missing" error reported for a real preprod
    * metadata blob. The CBOR decode + PrismObject parse must succeed; running the events through PrismStateInMemory
    * must also succeed without hitting any `???`.
    *
    * "index":6819,"tx":"a74cf531e0dc8ccf00ff71636486e169dad8f73a252ea6e201d062aa789d2980",
    */
  test("Metadata Simulate full pipeline (Mainnet 6819)") {
    import _root_.proto.prism.{PrismBlock, PrismObject, SignedPrismEvent}
    import fmgp.did.method.prism.PrismStateInMemory
    import fmgp.did.method.prism.proto.MaybeEvent
    import zio.{Runtime, Unsafe, ZIO}

    val metadata =
      "a119534da2617601616386584022be0212bb020a086d61737465722d3012473045022100db162a95479f8242a89da535165d4618f8fcaa2217f07a42b86a1e4128afa2c3022030c7fe8773274f584009bfbcb2c76fc69c1994f2890d496ceee34167b070735e879c1ae5010ae2010adf01125d0a086d61737465722d301001424f0a09736563703235366b311220245840c728a14302496fd7209b62278b1d1163119e91294b4adc3f2bcd8e1f1bc85a1a205b9f1582144fd381d726bf7ac0c34f8b62bc4f2ac78d0d6d3b8dfd1ce9642b58401412410a1061757468656e7469636174696f6e2d3010044a2b0a07456432353531391220bbe81cd28849dd702afb00aeb6046e17e2ed6352de6740f74fb435495840c95971ac123b0a0b61677265656d656e742d3010034a2a0a065832353531391220e04ad4cc6be75ceadb9208c65e32dbb47eb5250174eef270e4036fe33052e64138"

    val bytes = fmgp.util.hex2bytes(metadata)
    val decoded = Cbor.decode(bytes).to[CardanoTransactionMetadataPrismCBOR].valueEither
    assert(decoded.isRight, s"CBOR decode must succeed, got: $decoded")
    val prismObject = decoded.toOption.get.toPrismObject
    val events: Seq[SignedPrismEvent] = prismObject.blockContent.toSeq.flatMap(_.events)
    assert(events.nonEmpty, "Expected at least one event in the PrismObject")

    val maybeEvents = MaybeEvent.fromProto(prismObject, "sim-tx", 0)
    val program = for {
      state <- PrismStateInMemory.empty
      _ <- ZIO.foreach(maybeEvents)(state.addMaybeEvent(_))
      ssis <- state.makeSSI
    } yield ssis

    val ran =
      try Right(Unsafe.unsafe { implicit u => Runtime.default.unsafe.run(program).getOrThrow() })
      catch case t: Throwable => Left(t)

    ran match
      case Left(t)     => fail(s"Simulation pipeline threw: ${t.getClass.getName}: ${t.getMessage}", t)
      case Right(ssis) => assert(ssis.nonEmpty, "Expected at least one SSI from the events")
  }

}
