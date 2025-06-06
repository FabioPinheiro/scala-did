package fmgp.did.method.prism.cardano

import munit._
import zio.json._
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

}
