package fmgp.did.framework

import munit.*

import zio.*
import zio.json.*
import zio.stream.*
import zio.http.*

import fmgp.did.*
import fmgp.did.comm.*

/** didFrameworkJVM/testOnly fmgp.did.framework.TransportDIDCommOverHTTPSuite */
class TransportDIDCommOverHTTPSuite extends ZSuite {

  // A valid SignedMessage JSON (from the DIDComm spec examples)
  val signedMessageJson = """{
   "payload":"eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19",
   "signatures":[
      {
         "protected":"eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRWREU0EifQ",
         "signature":"FW33NnvOHV0Ted9-F7GZbkia-vYAfBKtH4oBxbrttWAhBZ6UFJMxcGjL3lwOl4YohI3kyyd08LHPWNMgP2EVCQ",
         "header":{
            "kid":"did:example:alice#key-1"
         }
      }
   ]
}"""

  val signedMessage: SignedMessage = signedMessageJson.fromJson[SignedMessage].toOption.get

  // R5: TransportID must use "http"
  testZ("R5 - TransportID should start with transport:http") {
    for {
      transport <- TransportDIDCommOverHTTP.make("http://localhost:0")
    } yield assert(
      transport.id.startsWith("transport:http:"),
      s"Expected transport:http: prefix but got: ${transport.id}"
    )
  }

  // R4: SingleTransmission semantics
  testZ("R4 - transmissionType should be SingleTransmission") {
    for {
      transport <- TransportDIDCommOverHTTP.make("http://localhost:0")
    } yield {
      assertEquals(transport.transmissionFlow, Transport.TransmissionFlow.BothWays)
      assertEquals(transport.transmissionType, Transport.TransmissionType.SingleTransmission)
    }
  }

  // R2: Hub-based inbound supports multiple subscribers (broadcast)
  testZ("R2 - inbound Hub delivers messages to multiple subscribers") {
    ZIO.scoped {
      for {
        hub <- Hub.bounded[SignedMessage | EncryptedMessage](3)

        // Two subscribers BEFORE publishing (using Hub directly, no HTTP needed)
        sub1 <- hub.subscribe
        sub2 <- hub.subscribe

        // Publish a message to the hub
        _ <- hub.publish(signedMessage)

        result1 <- sub1.take
        result2 <- sub2.take
      } yield {
        assert(result1 == signedMessage, "Subscriber 1 should receive the message")
        assert(result2 == signedMessage, "Subscriber 2 should receive the message")
      }
    }
  }

  // R3: Late subscribers do NOT receive past messages (Hub semantics, by design)
  testZ("R3 - late subscribers do not receive messages published before subscription") {
    ZIO.scoped {
      for {
        hub <- Hub.bounded[SignedMessage | EncryptedMessage](3)

        // Publish BEFORE any subscriber exists
        _ <- hub.publish(signedMessage)

        // Subscribe AFTER publish
        sub <- hub.subscribe
        result <- sub.poll
      } yield assert(
        result.isEmpty,
        "Late subscriber should NOT receive messages published before subscription (Hub semantics)"
      )
    }
  }

  // R1: Outbound sends HTTP POST to destination
  testZ("R1 - outbound sends HTTP POST to destination") {

    val echoRoutes = Routes(
      Method.POST / trailing -> handler { (_: Request) =>
        Response(Status.Ok, body = Body.fromCharSequence("ok"))
      }
    )

    ZIO.scoped {
      for {
        server <- Server.serve(echoRoutes).fork
        port <- ZIO.serviceWithZIO[Server](_.port)
        client <- ZIO.service[Client]
        scope <- ZIO.service[Scope]
        destination = s"http://localhost:$port"
        transport <- TransportDIDCommOverHTTP
          .makeWithEnvironment(destination, env = ZEnvironment(client, scope))

        // Send should not fail — the server accepts the POST
        _ <- transport.send(signedMessage)
        _ <- server.interrupt
      } yield ()
    }.provide(Server.defaultWithPort(0), Client.default)
  }

  // Issue #596: HTTP response containing a DIDComm message should be published to inbound
  testZ("Issue #596 - HTTP response with DIDComm message is published to inbound") {

    val echoRoutes = Routes(
      Method.POST / trailing -> handler { (_: Request) =>
        Response(
          Status.Ok,
          Headers(Header.ContentType(MediaType("application", "didcomm-signed+json"))),
          Body.fromCharSequence(signedMessageJson)
        )
      }
    )

    ZIO.scoped {
      for {
        server <- Server.serve(echoRoutes).fork
        port <- ZIO.serviceWithZIO[Server](_.port)
        client <- ZIO.service[Client]
        scope <- ZIO.service[Scope]
        destination = s"http://localhost:$port"
        transport <- TransportDIDCommOverHTTP
          .makeWithEnvironment(destination, env = ZEnvironment(client, scope))

        // Subscribe BEFORE sending (Hub requires subscriber before publish)
        inboundFiber <- transport.inbound.take(1).runHead.timeout(5.seconds).fork
        _ <- ZIO.sleep(50.millis)

        // Send — the server responds with a SignedMessage JSON body
        _ <- transport.send(signedMessage)

        // The response should be parsed and published to inbound
        result <- inboundFiber.join
        _ <- server.interrupt
      } yield assert(
        result.flatten.isDefined,
        "HTTP response containing a DIDComm message should be published to inbound"
      )
    }.provide(Server.defaultWithPort(0), Client.default)
  }

}
