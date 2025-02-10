package fmgp.prism

import munit._
import zio.json._
import fmgp.did.DIDDocument
import fmgp.util.Base64
import fmgp.prism.PrismPublicKey._
// import fmgp.prism.CrytoUtil

class SharedCrytoSuite extends FunSuite {

  test("Metadata 6418 checkECDSASignature".ignore) {
    val cardanoPrismEntry = ModelsExamples.metadata_6418.getOrElse(???)
    val tmp = MaybeOperation.fromProto("tx", -1, cardanoPrismEntry.content)
    val mySignedPrismOperation = tmp.head.asInstanceOf[MySignedPrismOperation[OP]]

    PrismPublicKey
      .filterECKey(mySignedPrismOperation.operation.asInstanceOf[CreateDidOP].publicKeys)
      .find(_.usage == PrismKeyUsage.MasterKeyUsage) match
      case None                                            => fail("Missing MASTER_KEY")
      case Some(UncompressedECKey(id, usage, curve, x, y)) => fail("Expeting CompressedEcKeyData")
      case Some(key @ CompressedECKey(id, usage, curve, data)) =>
        SharedCryto.checkECDSASignature(
          msg = mySignedPrismOperation.protobuf.toByteArray,
          sig = mySignedPrismOperation.signature,
          pubKey = key
        ) match
          case false => fail("invalid")
          case true  => assertEquals(id, "master0") // ok
    // Error: publicKey of length 32 expected, got 33
  }
}
