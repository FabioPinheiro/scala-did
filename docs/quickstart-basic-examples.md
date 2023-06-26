# Basic Example


## Common Imports

Here are the usfull imports for most of the cases:

```scala mdoc
import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm._
```


## Parse a DID Document

```scala mdoc:silent
val documentString = """{
  "@context": "https://www.w3.org/ns/did/v1",
  "id": "did:example:123456789abcdefghi",
  "keyAgreement": [
    "did:example:123456789abcdefghi#keys-1",
    {
      "id": "did:example:123#zC9ByQ8aJs8vrNXyDhPHHNNMSHPcaSgNpjjsBYpMMjsTdS",
      "type": "X25519KeyAgreementKey2019",
      "controller": "did:example:123",
      "publicKeyMultibase": "z9hFgmPVfmBZwRvFEyniQDBkz9LmV7gDEqytWyGZLmDXE"
    }
  ]
}"""
```
```scala mdoc
documentString.fromJson[DIDDocument]
```

**NOTE** the return type is of type `Either[String, DIDDocument]`
<br> Since the `documentString` is a valid json and which is also a valid DID Document.
The value is `Right` of that class that implemente trait `DIDDocument`.
<br> If the `documentString` was a invalid json or did not represented a DID Document.
The return value would have been Left, containing the information why failed to parse.

```scala mdoc
"not a json".fromJson[DIDDocument]
```

```scala mdoc
"""{"some_json": "but_not_a_valid_document"}"""
  .fromJson[DIDDocument]
```

Another **Important Point** is that there is no failed here everywhere on this library! We work with values.
___(Any error/exception will be considered a bug and reports are appreciated)___

This this allowed us to this allowed us to build programs build (ZIO) programs. That can are executed at any point in time.

## Make DID Peer identities

```scala
import fmgp.crypto._
import fmgp.did.method.peer._

val sender = DIDPeer2.makeAgent(
  Seq(
    OKPPrivateKey(// keyAgreement
      kty = KTY.OKP,
      crv = Curve.X25519,
      d = "Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c",
      x = "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw",
      kid = None
    ),
    OKPPrivateKey(//keyAuthentication
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = "INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug",
      x = "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
      kid = None
    )
  ),
  Seq(DIDPeerServiceEncoded(s = "https://alice.did.fmgp.app/"))
)
```

## Trust Ping example

```scala
import fmgp.did.comm.protocol.trustping2._ // For the protocol specific stuff

val ping = TrustPingWithRequestedResponse(
  from = yeah exactly.id,
  to = TO("did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ")
)

ping.toPlaintextMessage.toJsonPretty
// val res0: String = {
//   "id" : "3dda13f7-daa3-4ccf-a3f0-9f5f5a8fad47",
//   "type" : "https://didcomm.org/trust-ping/2.0/ping",
//   "to" : [
//     "did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9ib2IuZGlkLmZtZ3AuYXBwLyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0"
//   ],
//   "from" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ",
//   "body" : {
//     "response_requested" : true
//   }
// }
```


## Encrypt

```scala
import Operations._
val program = for {
  msg <- authEncrypt(ping.toPlaintextMessage).provideSomeLayer(ZLayer.succeed(alice))
  _ <- Console.printLine(msg.toJsonPretty)
} yield ()

Unsafe.unsafe { implicit unsafe => // Run side efect
  Runtime.default.unsafe
    .run(program.provide(Operations.layerDefault ++ DidPeerResolver.layer))
    .getOrThrowFiberFailure()
}
```