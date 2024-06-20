package fmgp.did

import fmgp.did.*
import fmgp.crypto.*

object DIDWebExamples {

  private def makeKeyAgreement(d: String, x: String) =
    OKPPrivateKey(kty = KTY.OKP, crv = Curve.X25519, d = d, x = x)
  private def makeKeyAuthentication(d: String, x: String) =
    OKPPrivateKey(kty = KTY.OKP, crv = Curve.Ed25519, d = d, x = x)

  private def makeFabioDoc(did: String): DIDDocument = {
    DIDDocumentClass(
      id = DIDSubject(did),
      authentication = Some(
        Seq(
          VerificationMethodEmbeddedJWK(
            id = did + "#k2",
            controller = did,
            `type` = "JsonWebKey2020",
            publicKeyJwk = OKPPublicKey(
              kty = KTY.OKP,
              crv = Curve.Ed25519,
              x = "bgxO3hAHDS970HeQLx7-01xoWT6GabkR9whaWP2DqR8",
              kid = did + "#k2",
            )
          )
        )
      ),
      keyAgreement = Some(
        Set(
          VerificationMethodEmbeddedJWK(
            id = did + "#k1",
            controller = did,
            `type` = "JsonWebKey2020",
            publicKeyJwk = OKPPublicKey(
              kty = KTY.OKP,
              crv = Curve.X25519,
              x = "-w-E7bboOZQn8e5BXLJ-lbVfMPvvxJo0Ajjl1rlm3k4",
              kid = did + "#k1",
            )
          )
        )
      )
    )
  }

  val fabioWellKnown = makeFabioDoc("did:web:did.fmgp.app")
  val fabioWithPath = makeFabioDoc("did:web:did.fmgp.app:fabio")

  val (clioDoc, clioAgent): (DIDDocument, Agent) = {
    val did = "did:web:did.fmgp.app:clio"
    val keyAgreement = makeKeyAgreement(
      d = "V_iMJa7YL12wLMRpH36DLXZgDUpPIdnt09-a9cZ_fZ4",
      x = "v_Xx37c36tZDkr5aGQ_ZHP1OITa4btKua4UlbFfYR3A",
    ).withKid(did + "#k1")
    val keyAuthentication = makeKeyAuthentication(
      d = "sWQsOPOHkRxDuV9gF_cycvS2lA69tgiRv07xRY5etWU",
      x = "Elm1_e264C_dj9C_-iTaqUDYVbjxa8_TJ93GHc6yPyU",
    ).withKid(did + "#k2")

    val doc = DIDDocumentClass(
      id = DIDSubject(did),
      authentication = Some(
        Seq(
          VerificationMethodEmbeddedJWK(
            id = keyAuthentication.kid,
            controller = did,
            `type` = "JsonWebKey2020",
            publicKeyJwk = keyAuthentication.toPublicKey
          )
        )
      ),
      keyAgreement = Some(
        Set(
          VerificationMethodEmbeddedJWK(
            id = keyAgreement.kid,
            controller = did,
            `type` = "JsonWebKey2020",
            publicKeyJwk = keyAgreement.toPublicKey
          )
        )
      )
    )

    val agent = new Agent {
      override def id: DID = doc.id
      override def keyStore: KeyStore = KeyStore(Set(keyAgreement, keyAuthentication))
    }

    (doc, agent)
  }

  val (thaliaDoc, thaliaAgent): (DIDDocument, Agent) = {
    val did = "did:web:did.fmgp.app:thalia"
    val keyAgreement = makeKeyAgreement(
      d = "B8WDuCbt6NIjHHtjTssOzSJYNHOVLNZ4ekG5tcpolqU",
      x = "mpv_KLuwOMn6pDfw-Zknbazb1mAPSG-tumisCyqYwQg",
    ).withKid("#KeyAgreement")
    val keyAuthentication = makeKeyAuthentication(
      d = "y2c0JdZDyFokxomhZLyFE_NHLRy_1gF0-p2hYSSn2Tg",
      x = "DzKDGu9OxHff_RT-5dH4-fQbX9KTRLx5Xc1PM8LWfUU",
    ).withKid("#Authentication")

    val doc = DIDDocumentClass(
      id = DIDSubject(did),
      authentication = Some(
        Seq(
          VerificationMethodEmbeddedJWK(
            id = keyAuthentication.kid,
            controller = did,
            `type` = "JsonWebKey2020",
            publicKeyJwk = keyAuthentication.toPublicKey
          )
        )
      ),
      keyAgreement = Some(
        Set(
          VerificationMethodEmbeddedJWK(
            id = keyAgreement.kid,
            controller = did,
            `type` = "JsonWebKey2020",
            publicKeyJwk = keyAgreement.toPublicKey
          )
        )
      )
    )

    val agent = new Agent {
      override def id: DID = doc.id
      override def keyStore: KeyStore = KeyStore(Set(keyAgreement, keyAuthentication))
    }

    (doc, agent)
  }
}
