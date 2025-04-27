package fmgp.did.method.prism

import zio.json._
import zio.json.ast.Json
import fmgp.did.DIDSubject

object DIDPrismExamples {

  val ex1_prism_specs_long =
    """did:prism
      |:9b5118411248d9663b6ab15128fba8106511230ff654e7514cdcc4ce919bde9b
      |:Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VjcDI1NmsxEiEDHpf-yhIns-LP3tLvA8icC5FJ1ZlBwbllPtIdNZ3q0jU
      |""".stripMargin.replaceAll("\n", "")
  val ex1_prism_specs_short =
    """did:prism:9b5118411248d9663b6ab15128fba8106511230ff654e7514cdcc4ce919bde9b"""
  val ex2_prism_specs_short =
    """did:prism:db47e78dd57d2043a7a704fbd9d186a586682110a2097ac06dbc83b35602f290"""

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

}
