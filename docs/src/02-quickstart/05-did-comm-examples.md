# Getting Started with DIDComm Messaging

Here's a basic example of how to send a message using the library:

```scala mdoc
import zio.*
import zio.json.*
import fmgp.did.*
import fmgp.did.comm.*
import fmgp.did.comm.protocol.basicmessage2.*

// Initialize a new message
val message = new BasicMessage(
  to = Set(TO("did:example:123")),
  content = "Hello, World!",
  from = Some(FROM("did:example:456")), 
)


message.toPlaintextMessage.toJsonPretty
```


To receive messages, you can use the following code:

```scala mdoc
// TODO
// didcomm.onMessage { message =>
//   println(s"Received message from ${message.from}: ${message.body}")
// }
```

## Further Resources

The library's documentation 
The DIDComm V2 specification - (https://identity.foundation/didcomm-messaging/spec/)
The Decentralized Identity Foundation (DIF) - https://identity.foundation/
