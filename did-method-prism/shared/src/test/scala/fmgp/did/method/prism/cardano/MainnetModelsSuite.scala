package fmgp.did.method.prism.cardano

import munit._
import zio.json._

/** didResolverPrismJVM/testOnly fmgp.prism.MainnetModelsSuite */
class MainnetModelsSuite extends FunSuite {
  test("CardanoMetadata 6418") {
    assert(MainnetExamples.metadata_6418.toCardanoPrismEntry.isRight)
    val tmp = MainnetExamples.metadata_6418_cbor.toCardanoPrismEntry
    assert(tmp.isRight)
    assertNoDiff(
      String(
        tmp
          .getOrElse(???)
          .content
          .toProtoString
          .getBytes()
          .filterNot(_ == 127.toByte)
      ),
      """block_content {
        |  operations {
        |    signed_with: "master0"
        |    signature: "0D\002 z\304\027\336kV\017\200\370\227\242\212\315\024\352\206\206\372t\267\241~\271\016\034\273R\274,%\252\261\002 \0321\375\026\315\214T\333\037\022A\316\262X\376\025\034\022:\355\2100\325\316\325W\350{\210\227\312h"
        |    operation {
        |      create_did {
        |        did_data {
        |          public_keys {
        |            id: "key-1"
        |            usage: AUTHENTICATION_KEY
        |            compressed_ec_key_data {
        |              curve: "Ed25519"
        |              data: "\021%\032\270f\242\245\000D\361\203\260\223H\255>\336\000\234\210\017\251{\206\3464\002\333\376\223\367"
        |            }
        |          }
        |          public_keys {
        |            id: "master0"
        |            usage: MASTER_KEY
        |            compressed_ec_key_data {
        |              curve: "secp256k1"
        |              data: "\002\317\241\353Z\225F\321\311\033\255~\224\000k\377K\320Q\243\272m2\a\";\033 :F\a\004|"
        |            }
        |          }
        |          services {
        |            id: "service-1"
        |            type: "LinkedDomains"
        |            service_endpoint: "https://blocktrust.dev/"
        |          }
        |          context: "https://didcomm.org/messaging/contexts/v2"
        |        }
        |      }
        |    }
        |  }
        |}""".stripMargin
    )
  }

  test("CardanoMetadata 6451") {
    assert(MainnetExamples.metadata_6451.toCardanoPrismEntry.isRight)
    val tmp = MainnetExamples.metadata_6451_cbor.toCardanoPrismEntry
    assert(tmp.isRight)
    assertNoDiff(
      tmp.getOrElse(???).content.toProtoString,
      """block_content {
        |  operations {
        |    signed_with: "master0"
        |    signature: "0D\002 ?\202\261\335\"\357\274\ti\225RZ\2368Jn3\223\306_`&`\345\347\257\210e\357}t\275\002 Q\242~\270\033\243\r\243\342(aB\321x\351\333\362>\203\376\032f\334\206X\353\267\251\233n\210U"
        |    operation {
        |      create_did {
        |        did_data {
        |          public_keys {
        |            id: "auth-1"
        |            usage: AUTHENTICATION_KEY
        |            compressed_ec_key_data {
        |              curve: "secp256k1"
        |              data: "\003r\r\025\365\211\266Mu~[\254\241\032j\242FYb\372\371XX\3266\323\343\343\260\351\f<_"
        |            }
        |          }
        |          public_keys {
        |            id: "issue-1"
        |            usage: ISSUING_KEY
        |            compressed_ec_key_data {
        |              curve: "secp256k1"
        |              data: "\003%2\\\220N\271,\215\277r\321\004\323\276\222s\023\261\365\320I\024P0\366\255\027G[\365h\270"
        |            }
        |          }
        |          public_keys {
        |            id: "master0"
        |            usage: MASTER_KEY
        |            compressed_ec_key_data {
        |              curve: "secp256k1"
        |              data: "\002\330v%dS/\213\377\370\223@1~\027\277\333\026W/\026J\203F\223f\372\000\304\360\375eo"
        |            }
        |          }
        |        }
        |      }
        |    }
        |  }
        |}""".stripMargin
    )

  }

  test("CardanoMetadata 6452") {
    assert(MainnetExamples.metadata_6452.toCardanoPrismEntry.isLeft)
    assertEquals(
      MainnetExamples.metadata_6452.toCardanoPrismEntry,
      Left(
        "PRISM Block must the encode in ByteString - https://github.com/input-output-hk/prism-did-method-spec/issues/66"
      )
    )
    assertEquals(
      MainnetExamples.metadata_6452_cbor.toCardanoPrismEntry,
      Left(
        "Expected ByteString or Array of bytes but got Text (input position 8)"
      )
    )
  }
}
