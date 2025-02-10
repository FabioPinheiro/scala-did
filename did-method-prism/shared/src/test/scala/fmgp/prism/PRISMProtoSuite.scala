package fmgp.prism

import munit._
import zio.json._
import fmgp.did.DIDDocument
import fmgp.util.Base64

import proto.prism.KeyUsage
import proto.prism.PublicKey.KeyData
import fmgp.prism.PrismPublicKey.VoidKey
import fmgp.prism.PrismPublicKey.UncompressedECKey
import fmgp.prism.PrismPublicKey.CompressedECKey
import fmgp.util.hex2bytes

class PRISMProto extends FunSuite {

  test("Metadata 1") {
    val cardanoPrismEntry = ModelsExamples.metadata_1.getOrElse(???)
    val tmp = MaybeOperation.fromProto("tx", -1, cardanoPrismEntry.content)
    val mySignedPrismOperation = tmp.head.isInstanceOf[MySignedPrismOperation[OP]]

  }

}
