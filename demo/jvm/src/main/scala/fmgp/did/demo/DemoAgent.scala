package fmgp.did.demo

import zio._
import zio.json._

import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import zio.http._
import fmgp.did.framework._
import fmgp.did.comm.protocol.auth.AuthRequest

object DemoAgent {

  case class Table(data: Map[String, String] = Map.empty)
  val tableDataRef = ZLayer.fromZIO(Ref.make(Table()))

  def loginDemo: Routes[Operations & Resolver & Ref[Table], Nothing] = Routes(
    Method.GET / "auth" -> handler { (req: Request) => // wscat -c ws://localhost:8080/auth
      for {
        annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
        operations <- ZIO.service[Operations]
        resolver <- ZIO.service[Resolver]
        agent <- DemoAgent.agent
        table <- ZIO.service[Ref[Table]]
        webSocketApp = {
          def f(transport: TransportWSImp[String]): ZIO[Any, DidException, Unit] =
            for {
              _ <- ZIO.log("AUTH endpoint")
              authRequest = AuthRequest(from = agent.id)
              msg = authRequest.toPlaintextMessage
              _ <- table.update(t => Table(t.data + ((authRequest.id.value, transport.id))))
              sMsg <- operations
                .sign(msg)
                .mapError(cryptoFailed => DidException(cryptoFailed))
                .provideEnvironment(ZEnvironment(agent: fmgp.did.Agent, resolver))
              _ <- ZIO.log(sMsg.toJsonPretty)
              _ <- transport.send(sMsg.toJsonPretty)
            } yield ()
          TransportWSImp.createWebSocketApp(annotationMap, f)
        }
        ret <- webSocketApp.toResponse
      } yield (ret)
    },
    Method.GET / "table" -> handler { (req: Request) => // curl http://localhost:8080/table
      for {
        table <- ZIO.service[Ref[Table]]
        data <- table.get
        _ <- ZIO.log("table: " + data)
      } yield Response.text("table: " + data)

    },
  )

  def agent = ZIO
    .fromEither {

      /** DIDComm relay uses the well knowed alice's keys */
      """{"verificationMethod":[
         |{"id":"#Ed25519","type":"JsonWebKey2020","publicKeyJwk":{"kty":"OKP","crv":"Ed25519","x":"MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA"}},
         |{"id":"#X25519","type":"JsonWebKey2020","publicKeyJwk":{"kty":"OKP","crv":"X25519","x":"Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"}}
         |],
         |"authentication":["#Ed25519"],"assertionMethod":["#Ed25519"],"keyAgreement":["#X25519"],"capabilityInvocation":["#Ed25519"],"capabilityDelegation":["#Ed25519"],
         |"service":[{"id":"#s1","type":"DIDCommMessaging","serviceEndpoint":{"uri":"https://relay.fmgp.app"}},{"id":"#s2","type":"DIDCommMessaging","serviceEndpoint":{"uri":"wss://relay.fmgp.app/ws"}}]
         |}""".stripMargin
        .fromJson[ast.Json.Obj]
        .map(initDoc =>
          fmgp.did.method.peer.DIDPeer4.makeAgentLongForm(
            Seq(
              OKPPrivateKeyWithKid(
                kty = KTY.OKP,
                crv = Curve.X25519,
                d = "Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c",
                x = "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw",
                kid = "#X25519"
              ),
              OKPPrivateKeyWithKid(
                kty = KTY.OKP,
                crv = Curve.Ed25519,
                d = "INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug",
                x = "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
                kid = "#Ed25519"
              )
            ),
            initDoc
          )
        )
    }
    .mapError(error => DidException(FailToParse(error)))
    .catchAll(ex => ZIO.die(ex)) // FIXME
    .tap(mediatorAgent => ZIO.log(s"DIDComm Relay DID: ${mediatorAgent.id.string}"))

}
