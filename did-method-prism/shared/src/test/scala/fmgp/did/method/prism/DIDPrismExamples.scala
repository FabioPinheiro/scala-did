package fmgp.did.method.prism

import zio.json.*
import zio.json.ast.Json
import fmgp.did.DIDSubject

object DIDPrismExamples {

  /** missing in mainnet */
  val ex1_prism_specs_long =
    """did:prism
      |:9b5118411248d9663b6ab15128fba8106511230ff654e7514cdcc4ce919bde9b
      |:Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VjcDI1NmsxEiEDHpf-yhIns-LP3tLvA8icC5FJ1ZlBwbllPtIdNZ3q0jU
      |""".stripMargin.replaceAll("\n", "")

  /** missing in mainnet */
  val ex1_prism_specs_short =
    """did:prism:9b5118411248d9663b6ab15128fba8106511230ff654e7514cdcc4ce919bde9b"""

  /** missing in mainnet */
  val ex2_prism_specs_short =
    """did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290"""

  /** missing in mainnet */
  def ex1_prism_specs_short_didDocument =
    """{
      |  "@context": [
      |    "https://www.w3.org/ns/did/v1",
      |    "https://w3id.org/security/suites/jws-2020/v1",
      |    "https://didcomm.org/messaging/contexts/v2",
      |    "https://identity.foundation/.well-known/did-configuration/v1"
      |  ],
      |  "id": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290",
      |  "verificationMethod": [
      |    {
      |      "id": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#authentication0",
      |      "type": "JsonWebKey2020",
      |      "controller": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290",
      |      "publicKeyJwk": {
      |          "crv": "secp256k1",
      |          "kty": "EC",
      |          "x": "KDiRms8trMvdT4aaJbRtNxtDCYTvFJbstRrC3TgQNnc",
      |          "y": "AUwPe-RV4D9c9WbVKHeg7-imv2ZVDX8hDAEkMDphpqU"
      |       }
      |    },
      |    {
      |      "id": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#issuing0",
      |      "type": "JsonWebKey2020",
      |      "controller": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290",
      |      "publicKeyJwk" : {
      |        "kty" : "OKP",
      |        "crv" : "Ed25519",
      |        "x" : "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"
      |      }
      |    },
      |    {
      |      "id": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#key-agreement0",
      |      "type": "JsonWebKey2020",
      |      "controller": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290",
      |      "publicKeyJwk": {
      |        "kty": "OKP",
      |        "crv": "X25519", 
      |        "x": "pE_mG098rdQjY3MKK2D5SUQ6ZOEW3a6Z6T7Z4SgnzCE"
      |      }
      |    }
      |  ],
      |  "authentication": [
      |    "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#authentication0"
      |  ],
      |  "assertionMethod": [
      |    "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#issuing0"
      |  ],
      |  "keyAgreement": [
      |    "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#key-agreement0"
      |  ],
      |  "service": [
      |    {
      |      "id": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#DIDCommMessaging",
      |      "type": "DIDCommMessaging",
      |      "serviceEndpoint": [ 
      |        { 
      |          "uri": "https://example.com/path", 
      |          "accept": [ "didcomm/v2", "didcomm/aip2;env=rfc587" ], 
      |          "routingKeys": ["did:example:somemediator#somekey"] 
      |        }
      |      ]
      |    },
      |    {
      |      "id" : "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#linked-domain-1",
      |      "type": "LinkedDomains",
      |      "serviceEndpoint": {
      |        "origins": ["https://foo.example.com", "https://identity.foundation"]
      |      }
      |    },
      |    {
      |      "id": "did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290#linked-domain-2",
      |      "type": "LinkedDomains",
      |      "serviceEndpoint": "https://bar.example.com"
      |    }
      |  ]
      |}
      |""".stripMargin

  /** preprod DID with DIDCommMessaging service where serviceEndpoint is stored as a JSON object string in protobuf
    *
    * https://github.com/FabioPinheiro/prism-vdr/blob/main/preprod/events/a792dc6872a76bcbf96f6fbb73a06e07c32a042d55e7a8bc9131a542697e1138
    */
  val ex_preprod_a792dc68_event0 =
    """{"tx":"88622b7ac15980524a947e1cd6329103eec7baf1daf879efd6c38b861aebe7be","b":17407,"o":0,"signedWith":"master","signature":"3045022100f1dc5c0b31aa30f6e86a9e7679418e7fb326a4c0f50bfe2bba06091d4bda633c02207340236fb6dee2fa3c400fcc8648969db45ce92aff3ce91d393522955cc6302c","protobuf":"0a3e0a3c123a0a066d617374657210014a2e0a09736563703235366b3112210237e47abc53df0565faeb9ad66a16e7c7cb83ce4decc98f2479dda179771daf4c"}"""

  val ex_preprod_a792dc68_event1 =
    // """{"tx":"88622b7ac15980524a947e1cd6329103eec7baf1daf879efd6c38b861aebe7be","b":17407,"o":1,"signedWith":"master","signature":"304402206d0d77a05644677e311481f97a4e7ce25d912840c419533ea8169f74034ad75c0220681fac6d1a5868cec601891fccf1d5e07755e9c528185b04d7a0785e3b894971","protobuf":"12d0030a20a792dc6872a76bcbf96f6fbb73a06e07c32a042d55e7a8bc9131a542697e11381240613739326463363837326137366263626639366636666262373361303665303763333261303432643535653761386263393133316135343236393765313133381a481a460a440a07646964636f6d6d1210444944436f6d6d4d6573736167696e671a277b22757269223a2268747470733a2f2f6469642e666162696f70696e686569726f2e636f6d227d1a430a410a3f0a0b6b65792d302d5644522d3010084a2e0a09736563703235366b311221023fee2180b6ff5cdb758c436e3e100cf7335fdb0970054d87511d05f59cbd0e631a4b0a490a470a166b65792d302d41757468656e7469636174696f6e2d3010044a2b0a07456432353531391220e54f062f0109773957e88b19745817fc31e2bf36635c9ed19b577ec5fa9c44411a440a420a400a0f6b65792d302d49737375696e672d3010024a2b0a07456432353531391220a4f8e8962c92cd9ee4b489c6ea9df711ab3774aa38a900d70483cf9dd94427a21a480a460a440a144b65792d302d4b657941677265656d656e742d3010034a2a0a065832353531391220c0f02c7bdcde7c64069e30da195c44f8dc520a244e54f4cf6e7d0d5baac99d01"}"""
    """{"tx":"88622b7ac15980524a947e1cd6329103eec7baf1daf879efd6c38b861aebe7be","b":17407,"o":1,"signedWith":"master","signature":"304402206d0d77a05644677e311481f97a4e7ce25d912840c419533ea8169f74034ad75c0220681fac6d1a5868cec601891fccf1d5e07755e9c528185b04d7a0785e3b894971","protobuf":"12d0030a20a792dc6872a76bcbf96f6fbb73a06e07c32a042d55e7a8bc9131a542697e11381240613739326463363837326137366263626639366636666262373361303665303763333261303432643535653761386263393133316135343236393765313133381a481a460a440a07646964636f6d6d1210444944436f6d6d4d6573736167696e671a277b22757269223a2268747470733a2f2f6469642e666162696f70696e686569726f2e636f6d227d1a430a410a3f0a0b6b65792d302d5644522d3010084a2e0a09736563703235366b311221023fee2180b6ff5cdb758c436e3e100cf7335fdb0970054d87511d05f59cbd0e631a4b0a490a470a166b65792d302d41757468656e7469636174696f6e2d3010044a2b0a07456432353531391220e54f062f0109773957e88b19745817fc31e2bf36635c9ed19b577ec5fa9c44411a440a420a400a0f6b65792d302d49737375696e672d3010024a2b0a07456432353531391220a4f8e8962c92cd9ee4b489c6ea9df711ab3774aa38a900d70483cf9dd94427a21a480a460a440a146b65792d302d4b657941677265656d656e742d3010034a2a0a065832353531391220c0f02c7bdcde7c64069e30da195c44f8dc520a244e54f4cf6e7d0d5baac99d01"}"""
}
