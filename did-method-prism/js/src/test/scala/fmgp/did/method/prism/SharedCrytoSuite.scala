package fmgp.did.method.prism

import munit.*
import zio.json.*
import fmgp.did.DIDDocument
import fmgp.util.Base64
import fmgp.did.method.prism.proto.*
import fmgp.crypto.SharedCryto

class SharedCrytoSuite extends FunSuite {

  test("Metadata 6418 checkECDSASignature".ignore) {
    val cardanoPrismEntry =
      fmgp.did.method.prism.cardano.MainnetExamples.metadata_6418.toCardanoPrismEntry.getOrElse(???)
    val tmp = MaybeEvent.fromProto(cardanoPrismEntry.content, "tx", -1)
    val mySignedPrismEvent = tmp.head.asInstanceOf[MySignedPrismEvent[OP]]

    PrismPublicKey
      .filterECKey(mySignedPrismEvent.event.asInstanceOf[CreateDidOP].publicKeys)
      .find(_.usage == PrismKeyUsage.MasterKeyUsage) match
      case None                                                               => fail("Missing MASTER_KEY")
      case Some(PrismPublicKey.UncompressedECKey(id, usage, curve, x, y))     => fail("Expeting CompressedEcKeyData")
      case Some(key @ PrismPublicKey.CompressedECKey(id, usage, curve, data)) =>
        SharedCryto.checkECDSASignature(
          msg = mySignedPrismEvent.protobuf.toByteArray,
          sig = mySignedPrismEvent.signature,
          pubKey = key
        ) match
          case false => fail("invalid")
          case true  => assertEquals(id, "master0") // ok
    // Error: publicKey of length 32 expected, got 33
  }
}
