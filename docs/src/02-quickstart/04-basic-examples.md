# Basic Example


```scala mdoc:invisible
import zio.*
import zio.json.*
import fmgp.did.*
import fmgp.did.comm.*
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
"""{"some_json": "but_not_a_valid_document"}""".fromJson[DIDDocument]
```

Another **Important Point** is that there is no failed here everywhere on this library! We work with values.
___(Any error/exception will be considered a bug and reports are appreciated)___

This this allowed us to this allowed us to build programs build (ZIO) programs. That can are executed at any point in time.

## Make DID Peer identities

```scala mdoc
import fmgp.crypto.*
import fmgp.did.method.peer.*

val alice = DIDPeer2.makeAgent(
  Seq(
    OKPPrivateKey( // keyAgreement
      kty = KTY.OKP,
      crv = Curve.X25519,
      d = "Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c",
      x = "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw",
      kid = None
    ),
    OKPPrivateKey( // keyAuthentication
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = "INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug",
      x = "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
      kid = None
    )
  ),
  Seq(DIDPeerServiceEncoded.fromEndpoint("https://alice.did.fmgp.app/"))
)

alice.id.asDIDSubject

alice.id.document.toJsonPretty
```

### Resolve did:peer identities 

```scala mdoc:height=5
import fmgp.did.method.peer.*

val program1 = DidPeerResolver.didDocument(DIDPeer(alice.id.asDIDSubject))

Unsafe.unsafe { implicit unsafe => // Run side effect
  Runtime.default.unsafe
    .run(program1)
    .getOrThrowFiberFailure()
}
```

## Trust Ping example

```scala mdoc
import fmgp.did.comm.protocol.trustping2.* // For the protocol

val ping = TrustPingWithRequestedResponse(
  from = alice.id,
  to = TO("did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ")
)

ping.toPlaintextMessage.toJsonPretty
```


## Encrypt

```scala mdoc:silent
import Operations.*

val program2 = for {
  msg <- authEncrypt(ping.toPlaintextMessage).provideSomeLayer(ZLayer.succeed(alice))
  _ <- Console.printLine(msg.toJsonPretty)
} yield ()
```
```scala mdoc
Unsafe.unsafe { implicit unsafe => // Run side effect
  Runtime.default.unsafe
    .run(program2.provide(Operations.layerOperations ++ DidPeerResolver.layer))
    .getOrThrowFiberFailure()
}
```