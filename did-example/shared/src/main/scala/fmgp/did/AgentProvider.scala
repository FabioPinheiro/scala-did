package fmgp.did

import zio._
import fmgp.crypto._
import fmgp.did.Agent
import fmgp.did.resolver.peer.DIDPeer2
import fmgp.did.resolver.peer.DIDPeerServiceEncoded

object AgentProvider {

  /** https://mermaid.live/edit#pako:eNpVkMFqwzAMhl_F6Ny-gA-FbVmht8LKLnEPaqwsooltFDswSt-9cpvDdpD8S__3G-MbdNETWPgRTIM5NS4Y8zZyR2a73Zkj5lbLuZAkLiTnar_Hy9NscKG_82HB0NamOM9zWfGPAWXk132fC7VaCpCGZy8xpRUz1XxCe8Fw_b_65i5HaV-HpvUp3POa3OOFYxWePWxgIplQlYVbXTrIA03kwKr01GMZswMX7opiyfHrN3RgsxTaQEkeMzWM-hsT2B7Hme4PPpxgwQ
    */
  val usersGraph = """
  |graph TD
  |  Alice --> Pat[Pat\nprover]
  |  Bob --> Dave
  |  Bob --> Ivan[Ivan\nissuer]
  |  Charlie --> Eve[Eve\neavesdropper]
  |    Eve --> Frank
  |    Eve --> Victor[Victor\nverifier]
  |  Fabio
  |  did
  |""".stripMargin

  val allAgents = Map(
    "alice" -> alice,
    "bob" -> bob,
    "charlie" -> charlie,
    "dave" -> dave,
    "eve" -> eve,
    "frank" -> frank,
    "ivan" -> ivan,
    "pat" -> pat,
    "victor" -> victor,
  )

  private def aliceURL = s"https://alice.did.fmgp.app/"
  private def bobURL = s"https://bob.did.fmgp.app/"
  private def charlieURL = s"https://charlie.did.fmgp.app/"

  private def keyAgreement(d: String, x: String) =
    OKPPrivateKey(kty = KTY.OKP, crv = Curve.X25519, d = d, x = x, kid = None)
  private def keyAuthentication(d: String, x: String) =
    OKPPrivateKey(kty = KTY.OKP, crv = Curve.Ed25519, d = d, x = x, kid = None)

  // did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd
  val alice = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(DIDPeerServiceEncoded(s = aliceURL))
  )
  // did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z
  val bob = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("H5wHQcecUqobAMT3RiNsAaYaFXIfTLCNhWAYXgTYv7E", "f8ce_zxdhIEy76JE21XpVDviRtR2amXaZ6NjYyIPjg4"),
      keyAuthentication("LyMSyr_usdn3pHZc00IbJaS2RcvF4OcJTJIB2Vw6dLQ", "TQdV8Wduyz3OylN3YbyHR0R-aynF3C1tmvHAgl6b34I")
    ),
    Seq(DIDPeerServiceEncoded(s = bobURL))
  )
  // did:peer:2.Ez6LSbj1AHPALSc6v6jMzozr4HfE3siavLgjZ8XrriNTSWdkW.Vz6MkmL4nXx4qze1UU3hAcAVghn5WVZj2vbt2w9vpAWN85ZAS
  val charlie = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("J7aAYF3EDfKpfK392bgPV-bC2vaceq0jpMnfjM8agoQ", "ALluQw8hX9lN3G-vKEsGHJdteEMx7IN8qrNu3Z-o_wM"),
      keyAuthentication("dJ-k49IrQ3CQmrkrpHZvWYIdlMkcc6-hnQvrCa8hB1Q", "ZioC9PEG6LJA_Yf1sDTwQgTkKpK-LMWYYff0KUfeVZM")
    ),
    Seq(DIDPeerServiceEncoded(s = charlieURL))
  )
  // did:peer:2.Ez6LSoJXKbJuq1FvfMdCm3jnbTV4PLAN4qAs8sEEnZn6gNiBD.Vz6MkmAG1iP6YaTHzmU2n6wfkCuMep8swWEUdjwzmVH5EewjW)
  val pat = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z8CsybA4UuLWeSPdteHvTbpx1QVOhX-pMXsEsM1c74Y", "rMBBlYU8xMpzuwGp8Jff9z-bkepa2AwIdmZydrzKqCw"),
      keyAuthentication("6VvWEGxOv15e9EPgb1zXq7pnrbSMN9ediBulijUxvPA", "Y6bl7Gs7LqYLlt6bAjPzE8ywpc2xtKtiv8irsFS5f1E")
    ),
    Seq(DIDPeerServiceEncoded(s = alice.id.did))
  )
  // did:peer:2.Ez6LShTw9dYiKEixwH3vAmSufL7gb1Rvp2YGoy4VHYUWxpjmh.Vz6MknK57qz1oyQFdsDm9L29CNBJzqESqe5eTPYXTsg1yVJKw
  val dave = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("p2dYLzyj__m5gn6Fk4SQnmhbOar1QSff-IO6MBZlf7w", "VgNN-ldBGnukky38qz2G0lMLPF9UWH9NzEEcZM84ozA"),
      keyAuthentication("o7ZwF-S12lUtXrZl6B9GsrYo-sDpDdrTWz8MPsc-OXE", "dMR-f2jlNpk9IHJpEqbz_3GwU1Iby4ZCun-2s4Z3bk4")
    ),
    Seq(DIDPeerServiceEncoded(s = bob.id.did))
  )
  // did:peer:2.Ez6LSpFPZ4YmSHjEvSb2DsDdNJAPNhPWP48RowvpwvyKZRkjp.Vz6MkrpKnrHBvLREgP9VT4KZkEzdtjXxT9w8FMkZq2tuUjLDr
  val ivan = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("_yfk3UXYu6Wt1LxqbqI5ST0JdDC6aQS5TV-8CLb6u4s", "us5q6uu-DUC_49Bh04cZMOScKewx-Xg8kVlF7KSHpD8"),
      keyAuthentication("f31O53ZdLyF73RmuJTKqe1TiI0mXzQHiqdBquBj1wVg", "t7Gs8OdVn3eyZ21QB32qYPxK_vqAJ1DfaWAoYwEKUZU")
    ),
    Seq(DIDPeerServiceEncoded(s = bob.id.did))
  )
  // did:peer:2.Ez6LSi65Z5Xxq3Qyksu3Fy6gawopQM9hN5yqvcsKQQh8aqAZH.Vz6MkfoXdwNKyAVfz6QtmLkow4J5v9es21FsbocZwWbNaJzHs
  val eve = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("T0ra1wQ4FizXQtchN03lt3_DvKdd-_xVTtM39Z9Yl7I", "X0W3atVLGH3XWf6X9nRtmM0NYWzf7joyi-YEnKMkUgQ"),
      keyAuthentication("MIbKvsxw658nBM2SAuQBq3iRdbfTlqXuPLdQ23XtPVs", "FAzu6ZMsBrwGklCFBO3fAHNpDNa-IpOd_zuedLDfWa4")
    ),
    Seq(DIDPeerServiceEncoded(s = charlie.id.did))
  )
  // did:peer:2.Ez6LSdmPx5SxWD6XGmZk5EcYZD5EjB1yCgZAXzvuyabmNqYVz.Vz6MkhE8eyVZsZJjhk8rNXox5yiNywkNmzcMQcfAn5acg5Wcm
  val frank = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("_9NL5wwqOyFKGv4ikD-hMQYzqoEchXx5LXtGnM_F6Zo", "Hw28ApUI4byjBpyjTr2ALBPT4u0dBG1v7mlcGO7C2j0"),
      keyAuthentication("RMFyv5f_IoT5ikNewuzDUU1CBrdoaqfkvsRA2ywa2ys", "KTYniMnFo_8KLG3BP3Jg8ISEaUzKXtBgf9eZXKrfT14")
    ),
    Seq(DIDPeerServiceEncoded(s = eve.id.did))
  )

  // did:peer:2.Ez6LSi4HpG1Y3MQqRVJvMBbJBFKP329g6k8t1qC4GVxNsWNJB.Vz6MksAbrQeW9NLum9QTHMu2JHRYVM6LpLxoE8y39f8fYJNLr
  val victor = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("t0PNwt5UC3g-sblXAaJFKlhwxM3U-cqvFLvSxvRZqZM", "XtBrU4fKSFun-U2AkYJ3zs_X57bZq2vlGV7ZBXzDCGA"),
      keyAuthentication("g0wE5tKUba3wVvtbHwOtpG-XPfX14t2rXvclDw1uCuc", "vONzzXnYK4KC_j_d4FMflqhRjFsI7giAeCGq8j-1vos")
    ),
    Seq(DIDPeerServiceEncoded(s = eve.id.did))
  )

  // // did:peer:2.Ez6LSm4qrZKu2svvVCMNGUwXG9FZRdRqrfU2AqyABFdgJ8xzV.Vz6Mks42Y68Na7B8xgV4RKedZGqXrKNp7ryymv3nDEnyZXBFY
  // val x = DIDPeer2.makeAgent(
  //   Seq(
  //     keyAgreement("d5NNzHVrh13uEYd_IiEpW9xJPdjb60j-06AHcCdXT6c", "i4e1FMEyCC3uqBZvRH_yT2_soyvrhYxp6Mi33D4i-Eo"),
  //     keyAuthentication("mnMNIGXhYLiIEzk4KjCewN5QTtEHgSd_V5dAvpy1-_M", "uzRMEeJho3oGJUiJWi_YvVwTlUrDpJ4Z2LZcqmseZCM")
  //   ),
  //   Seq(DIDPeerServiceEncoded(s = eve.id.did))
  // )

}
