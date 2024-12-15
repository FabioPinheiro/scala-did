package fmgp.did

import zio._
import zio.json.*
import fmgp.crypto._
import fmgp.did.Agent
import fmgp.did.comm.*
import fmgp.did.method.peer.DIDPeer
import fmgp.did.method.peer.DIDPeer2
import fmgp.did.method.peer.DIDPeer4
import fmgp.did.method.peer.DIDPeerServiceEncoded
import fmgp.did.method.hardcode.HardcodeResolver

import fmgp.did.AgentProvider._

case class AgentProvider(agents: Seq[AgentWithShortName], identities: Seq[DIDWithShortName]) {
  def getAgentByDID(subject: DIDSubject): Option[Agent] = agents.find(_.id.did == subject.did).map(_.value)
  def getAgentByName(name: String): Option[Agent] = agents.find(_.name == name).map(_.value)
  def getDIDByName(name: String): Option[DID] =
    getAgentByName(name)
      .map(_.id)
      .orElse(
        identities.find(_.name == name).map(_.value)
      )

  def nameOfAgent(id: DID): Option[String] = agents.find(_.value.id.string == id.string).map(_.name)
  def nameOfIdentity(id: DID): Option[String] =
    nameOfAgent(id)
      .orElse(
        identities.find(_.value.string == id.string).map(_.name)
      )

  def agetnsNames = agents.map(_.name)
  def identitiesNames = agents.map(_.name) ++ identities.map(_.name)

  def agentsAndIdentities = agents ++ identities

  def withAgent(newAgent: AgentWithShortName) = this.copy(agents = agents :+ newAgent)
}

object AgentProvider {

  given decoder: JsonDecoder[AgentProvider] = DeriveJsonDecoder.gen[AgentProvider]
  given encoder: JsonEncoder[AgentProvider] = DeriveJsonEncoder.gen[AgentProvider]

  case class AgentWithShortName(name: String, value: AgentSimple) extends Agent {
    override def id = value.id
    override def keyStore = value.keyStore

    def toDIDWithShortName = DIDWithShortName(name, value.id)
  }

  object AgentWithShortName {
    given decoder: JsonDecoder[AgentWithShortName] = DeriveJsonDecoder.gen[AgentWithShortName]
    given encoder: JsonEncoder[AgentWithShortName] = DeriveJsonEncoder.gen[AgentWithShortName]
  }

  case class DIDWithShortName(name: String, value: DIDSubject) extends DID {
    override def namespace: String = value.namespace
    override def specificId: String = value.specificId
  }

  object DIDWithShortName {
    given decoder: JsonDecoder[DIDWithShortName] = DeriveJsonDecoder.gen[DIDWithShortName]
    given encoder: JsonEncoder[DIDWithShortName] = DeriveJsonEncoder.gen[DIDWithShortName]
  }

  /** https://mermaid.live/edit#pako:eNpVkMFqwzAMhl_F6Ny-gA-FbVmht8LKLnEPaqwsooltFDswSt-9cpvDdpD8S__3G-MbdNETWPgRTIM5NS4Y8zZyR2a73Zkj5lbLuZAkLiTnar_Hy9NscKG_82HB0NamOM9zWfGPAWXk132fC7VaCpCGZy8xpRUz1XxCe8Fw_b_65i5HaV-HpvUp3POa3OOFYxWePWxgIplQlYVbXTrIA03kwKr01GMZswMX7opiyfHrN3RgsxTaQEkeMzWM-hsT2B7Hme4PPpxgwQ
    */
  def usersGraph = s"""
  |graph LR
  |  subgraph did.fmgp.app
  |
  |    subgraph alice.did.fmgp.app
  |      Alice
  |      AliceWithMultiService
  |    end
  |
  |    subgraph bob.did.fmgp.app
  |      Bob
  |    end
  |
  |    subgraph relay.fmgp.app
  |      DIDComm_Relay
  |    end
  |
  |    Web_Well_Known
  |    Web_Fabio
  |  end
  |
  |  subgraph localhost:8080
  |    localhost8080AliceHttp
  |    localhost8080AliceWs
  |    localhost8080AliceHttp&Ws
  |  end
  |
  |  click Alice "#/resolver/${alice.id.did}" "Link to Alice DID Document"
  |  click AliceWithMultiService "#/resolver/${alice.id.did}" "Link to Alice DID Document with multiple ServiceEndpoints"
  |  click Bob "#/resolver/${bob.id.did}" "Link to Bob DID Document"
  |  click Bob "#/resolver/${didCommRelay.id.did}" "Link to Bob DID Document"
  |  click localhost8080AliceHttp "#/resolver/${localhost8080AliceHttp.id.did}" "Link to local DID Document with a HTTP ServiceEndpoint"
  |  click localhost8080AliceWs "#/resolver/${localhost8080AliceWs.id.did}" "Link to local DID Document with a WS ServiceEndpoint"
  |  click localhost8080AliceHttp&Ws "#/resolver/${localhost8080AliceHttpWs.id.did}" "Link to local DID Document with both HTTP & WS ServiceEndpoints"
  |  click Web_Well_Known "#/resolver/${DIDWebExamples.fabioWellKnown.id.did}" "Link to Fabio DID (web method) Document"
  |  click Web_Fabio "#/resolver/${DIDWebExamples.fabioWithPath.id.did}" "Link to Alice DID (web method) Document"
  |""".stripMargin

  def provider = AgentProvider(
    Seq(
      AgentWithShortName("alice", alice),
      AgentWithShortName("bob", bob),
      AgentWithShortName("fmgpMediator", fmgpMediator),
      AgentWithShortName("iohkMediatorSitHttpWs", iohkMediatorSitHttpWs),
      AgentWithShortName("iohkMediatorSandboxHttpWs", iohkMediatorSandboxHttpWs),
      AgentWithShortName("exampleAlice", exampleAlice),
      AgentWithShortName("exampleBob", exampleBob),
      AgentWithShortName("exampleSicpaAlice", exampleSicpaAlice),
      AgentWithShortName("exampleSicpaBob", exampleSicpaBob),
      AgentWithShortName("exampleSicpaCharlie", exampleSicpaCharlie),
      AgentWithShortName("exampleSicpaMediator1", exampleSicpaMediator1),
      AgentWithShortName("exampleSicpaMediator2", exampleSicpaMediator2),
      AgentWithShortName("exampleSicpaMediator3", exampleSicpaMediator3),
      AgentWithShortName("localhost8080AliceHttp", localhost8080AliceHttp),
      AgentWithShortName("localhost8080AliceWs", localhost8080AliceWs),
      AgentWithShortName("localhost8080AliceHttp&Ws", localhost8080AliceHttpWs),
      AgentWithShortName("Web_Clio", DIDWebExamples.clioAgent),
      AgentWithShortName("Web_Thalia", DIDWebExamples.thaliaAgent),
      AgentWithShortName("DIDCommRelay", didCommRelay),
    ),
    Seq(
      DIDWithShortName(
        "rootsid",
        DIDPeer
          .fromDID(
            DIDSubject( // https://mediator.rootsid.cloud/oob_url // latest check 2024-11-05
              "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0"
            ).toDID
          )
          .toOption
          .get
      ),
      DIDWithShortName(
        "blocktrust",
        DIDPeer
          .fromDID(
            DIDSubject( // https://mediator.blocktrust.dev/ // latest check 2024-11-05
              "did:peer:2.Ez6LSeUYyDHMTbWoMGCKyqntPR95TB3N6ic2A27YLmwZHchxY.Vz6MkgRyq89zDCmXEcg8LmdqKjoaanxK4MUVbbtembDa4fLpK.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLmJsb2NrdHJ1c3QuZGV2LyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0"
            ).toDID
          )
          .toOption
          .get
      ),
      DIDWithShortName(
        "aliceWithMultiService",
        DIDPeer.fromDID(DIDSubject(aliceWithMultiService).toDID).toOption.get
      ),
      DIDWithShortName("DecentriQube_Mediator", decentriQubeMediatorDID),
      DIDWithShortName("DecentriQube_Verifier", decentriQubeVerifierDID),
      DIDWithShortName("DecentriQube_Register", decentriQubeRegisterDID),
      DIDWithShortName("Web_Well_Known", DIDWebExamples.fabioWellKnown),
      DIDWithShortName("Web_Fabio", DIDWebExamples.fabioWithPath),
      DIDWithShortName("Indiciotech_Mediator_web", DIDExampleIndiciotech.mediatorWeb),
      DIDWithShortName("Indiciotech_Mediator_peer2", DIDExampleIndiciotech.mediatorPeer2),
      DIDWithShortName("Indiciotech_Mediator_peer3", DIDExampleIndiciotech.mediatorPeer3),
    )
  )

  val decentriQubeMediator =
    "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHBzOi8vbS5mbWdwLmFwcCIsImEiOlsiZGlkY29tbS92MiJdfX0.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6IndzczovL20uZm1ncC5hcHAvd3MiLCJhIjpbImRpZGNvbW0vdjIiXX19"
  val decentriQubeVerifier =
    "did:peer:2.Ez6LShJTpyEhR986PyBgo7E1j44n1fwgpst3vbNUEh7jdn1Yx.Vz6MkuDVDb1dVpNzUqGsHy41r3fnnAUJTaVcfva8sZxb7yJJV.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHBzOi8vdmVyaWZpZXIuZGVjZW50cmlxdWJlLmNvbSIsImEiOlsiZGlkY29tbS92MiJdfX0.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6IndzczovL3ZlcmlmaWVyLmRlY2VudHJpcXViZS5jb20vd3MiLCJhIjpbImRpZGNvbW0vdjIiXX19"
  val decentriQubeRegister =
    "did:peer:2.Ez6LSfyM9dRiumBJbmH4zMd4uAjmpiWdB7xvm5cAkoPvg4PGq.Vz6MkjNcmVzRGLrCmeji9exN6e32doNGrz1Np3owF8GvRvnZY.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHBzOi8vcmVnaXN0ZXIuZGVjZW50cmlxdWJlLmNvbSIsImEiOlsiZGlkY29tbS92MiJdfX0.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6IndzczovL3JlZ2lzdGVyLmRlY2VudHJpcXViZS5jb20vd3MiLCJhIjpbImRpZGNvbW0vdjIiXX19"

  val decentriQubeMediatorDID = DIDPeer.fromDID(DIDSubject(decentriQubeMediator).toDID).toOption.get
  val decentriQubeVerifierDID = DIDPeer.fromDID(DIDSubject(decentriQubeVerifier).toDID).toOption.get
  val decentriQubeRegisterDID = DIDPeer.fromDID(DIDSubject(decentriQubeRegister).toDID).toOption.get

  private def aliceURL = s"https://alice.did.fmgp.app/"
  private def aliceWsURL = s"wss://alice.did.fmgp.app/ws"
  private def bobURL = s"https://bob.did.fmgp.app/"

  private def keyAgreement(d: String, x: String) =
    OKPPrivateKey(kty = KTY.OKP, crv = Curve.X25519, d = d, x = x)
  private def keyAuthentication(d: String, x: String) =
    OKPPrivateKey(kty = KTY.OKP, crv = Curve.Ed25519, d = d, x = x)

  // did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd
  val alice = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(DIDPeerServiceEncoded.fromEndpoint(aliceURL), DIDPeerServiceEncoded.fromEndpoint(aliceWsURL))
  )

  // did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z
  val bob = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("H5wHQcecUqobAMT3RiNsAaYaFXIfTLCNhWAYXgTYv7E", "f8ce_zxdhIEy76JE21XpVDviRtR2amXaZ6NjYyIPjg4"),
      keyAuthentication("LyMSyr_usdn3pHZc00IbJaS2RcvF4OcJTJIB2Vw6dLQ", "TQdV8Wduyz3OylN3YbyHR0R-aynF3C1tmvHAgl6b34I")
    ),
    Seq(DIDPeerServiceEncoded.fromEndpoint(bobURL))
  )

  val fmgpMediator = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(
      DIDPeerServiceEncoded.fromEndpoint("https://mediator.fmgp.app"),
      DIDPeerServiceEncoded.fromEndpoint("wss://mediator.fmgp.app/ws")
    )
  )

  /** did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHBzOi8vc2FuZGJveC1tZWRpYXRvci5hdGFsYXByaXNtLmlvIiwiYSI6WyJkaWRjb21tL3YyIl19fQ.SeyJ0IjoiZG0iLCJzIjp7InVyaSI6IndzczovL3NhbmRib3gtbWVkaWF0b3IuYXRhbGFwcmlzbS5pby93cyIsImEiOlsiZGlkY29tbS92MiJdfX0
    */
  val iohkMediatorSandboxHttpWs = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(
      DIDPeerServiceEncoded.fromEndpoint("https://sandbox-mediator.atalaprism.io"),
      DIDPeerServiceEncoded.fromEndpoint("wss://sandbox-mediator.atalaprism.io/ws")
    )
  )

  val iohkMediatorSitHttpWs = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(
      DIDPeerServiceEncoded.fromEndpoint("https://sit-prism-mediator.atalaprism.io"),
      DIDPeerServiceEncoded.fromEndpoint("wss://sit-prism-mediator.atalaprism.io/ws")
    )
  )

  val exampleAlice = new Agent {
    override def id: DID = DidExample.senderDIDDocument.id
    override def keyStore: KeyStore = KeyStore(DidExample.senderSecrets.keys)
  }

  val exampleBob = new Agent {
    override def id: DID = DidExample.recipientDIDDocument.id
    override def keyStore: KeyStore = KeyStore(DidExample.recipientSecrets.keys)
  }

  val exampleSicpaAlice = new Agent {
    override def id: DID = DidExampleSicpaRustAlice.aliceDIDDocument.id
    override def keyStore: KeyStore = KeyStore(DidExampleSicpaRustAlice.aliceSecrets.keys)
  }
  val exampleSicpaBob = new Agent {
    override def id: DID = DidExampleSicpaRustBob.bobDIDDocument.id
    override def keyStore: KeyStore = KeyStore(DidExampleSicpaRustBob.bobSecrets.keys)

  }
  val exampleSicpaCharlie = new Agent {
    override def id: DID = DidExampleSicpaRustCharlie.charlieDIDDocument.id
    override def keyStore: KeyStore = KeyStore(DidExampleSicpaRustCharlie.charlieSecrets.keys)

  }

  val exampleSicpaMediator1 = new Agent {
    override def id: DID = DidExampleSicpaRustMediator1.mediator1DIDDocument.id
    override def keyStore: KeyStore = KeyStore(DidExampleSicpaRustMediator1.mediator1Secrets.keys)
  }
  val exampleSicpaMediator2 = new Agent {
    override def id: DID = DidExampleSicpaRustMediator2.mediator2DIDDocument.id
    override def keyStore: KeyStore = KeyStore(DidExampleSicpaRustMediator2.mediator2Secrets.keys)

  }
  val exampleSicpaMediator3 = new Agent {
    override def id: DID = DidExampleSicpaRustMediator3.mediator3DIDDocument.id
    override def keyStore: KeyStore = KeyStore(DidExampleSicpaRustMediator3.mediator3Secrets.keys)

  }

  val localhost8080AliceHttp = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(DIDPeerServiceEncoded.fromEndpoint("http://localhost:8080"))
  )
  val localhost8080AliceWs = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(DIDPeerServiceEncoded.fromEndpoint("ws://localhost:8080/ws"))
  )

  val localhost8080AliceHttpWs = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(
      DIDPeerServiceEncoded.fromEndpoint("http://localhost:8080"),
      DIDPeerServiceEncoded.fromEndpoint("ws://localhost:8080/ws")
    )
  )

  /** Alice (peer2) with the folloing Services:
    *   - {"t":"dm","s":"https://alice.did.fmgp.app/","r":[],"a":["didcomm/v2"]}
    *   - {"t":"dm","s":"https://alice.did.fmgp.app/","r":[],"a":["didcomm/v2"]}
    *   - {"t":"DIDCommMessaging","s":"https://3.server.endpoint","r":[],"a":["didcomm/v2"]}
    *   - {"t":"SeriveType123","s":"https://new.server.type"}
    */
  val aliceWithMultiService =
    """did:peer:2
        |.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y
        |.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd
        |.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ
        |.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ
        |.SeyJ0IjoiRElEQ29tbU1lc3NhZ2luZyIsInMiOiJodHRwczovLzMuc2VydmVyLmVuZHBvaW50IiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ
        |.SeyJ0IjoiU2VyaXZlVHlwZTEyMyIsInMiOiJodHRwczovL25ldy5zZXJ2ZXIudHlwZSJ9
        |""".stripMargin.replaceAll("\n", "")

  /** DIDComm relay uses the well knowed alice's keys */
  val didCommRelay = {
    """{"verificationMethod":[
         |{"id":"#Ed25519","type":"JsonWebKey2020","publicKeyJwk":{"kty":"OKP","crv":"Ed25519","x":"MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA"}},
         |{"id":"#X25519","type":"JsonWebKey2020","publicKeyJwk":{"kty":"OKP","crv":"X25519","x":"Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"}}
         |],
         |"authentication":["#Ed25519"],"assertionMethod":["#Ed25519"],"keyAgreement":["#X25519"],"capabilityInvocation":["#Ed25519"],"capabilityDelegation":["#Ed25519"],
         |"service":[{"id":"#s1","type":"DIDCommMessaging","serviceEndpoint":{"uri":"https://relay.fmgp.app"}},{"id":"#s2","type":"DIDCommMessaging","serviceEndpoint":{"uri":"wss://relay.fmgp.app/ws"}}]
         |}""".stripMargin
      .fromJson[ast.Json.Obj]
      .map(initDoc =>
        DIDPeer4.makeAgentLongForm(
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
      .toOption
      .get // TODO would be nice to run at cimpile time
  }

}
