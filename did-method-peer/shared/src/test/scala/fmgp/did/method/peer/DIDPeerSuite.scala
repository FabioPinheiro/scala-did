package fmgp.did.method.peer

import munit.*
import zio.*
import zio.json.*
import fmgp.crypto.*
import fmgp.did.*
import fmgp.did.method.peer.*
import fmgp.multibase.*

import DIDPeerExamples.*
import fmgp.did.method.peer.DidPeerResolver
import zio.json.ast.Json
import fmgp.util.Base64
import scala.util.matching.Regex

/** didResolverPeerJVM/testOnly fmgp.did.method.peer.DIDPeerSuite */
class DIDPeerSuite extends ZSuite {

  test("Check regex for peer (method 1)") {
    val d = ex5_peer1
    assert(DIDPeer.regexPeer.matches(d))
    assert(!DIDPeer.regexPeer0.matches(d))
    assert(DIDPeer.regexPeer1.matches(d))
    assert(!DIDPeer.regexPeer2.matches(d))
    // TODO numalgo 3
    assert(!DIDPeer.regexPeer4.matches(d))
    assert(!DIDPeer.regexPeer4_LONG.matches(d))
    assert(!DIDPeer.regexPeer4_SHORT.matches(d))
  }

  test("Check regex for peer (method 2)") {
    val d = ex4_peer2_did
    assert(DIDPeer.regexPeer.matches(d))
    assert(!DIDPeer.regexPeer0.matches(d))
    assert(!DIDPeer.regexPeer1.matches(d))
    assert(DIDPeer.regexPeer2.matches(d))
    // TODO numalgo 3
    assert(!DIDPeer.regexPeer4.matches(d))
    assert(!DIDPeer.regexPeer4_LONG.matches(d))
    assert(!DIDPeer.regexPeer4_SHORT.matches(d))
  }

  test("Check regex for peer (method 2) - aliceWithMultiService") {
    val d = aliceWithMultiService
    assert(DIDPeer.regexPeer.matches(d))
    assert(!DIDPeer.regexPeer0.matches(d))
    assert(!DIDPeer.regexPeer1.matches(d))
    assert(DIDPeer.regexPeer2.matches(d))
    // TODO numalgo 3
    assert(!DIDPeer.regexPeer4.matches(d))
    assert(!DIDPeer.regexPeer4_LONG.matches(d))
    assert(!DIDPeer.regexPeer4_SHORT.matches(d))
  }

  test("Check regex for peer (method 4 long form)") {
    val d = DIDPeerExamples.didpeer4long.string
    assert(DIDPeer.regexPeer.matches(d))
    assert(!DIDPeer.regexPeer0.matches(d))
    assert(!DIDPeer.regexPeer1.matches(d))
    assert(!DIDPeer.regexPeer2.matches(d))
    // TODO numalgo 3
    assert(DIDPeer.regexPeer4.matches(d))
    assert(DIDPeer.regexPeer4_LONG.matches(d))
    assert(!DIDPeer.regexPeer4_SHORT.matches(d))
  }

  test("Check regex for peer (method 2 short form)") {
    val d = DIDPeerExamples.didpeer4short.string
    assert(DIDPeer.regexPeer.matches(d))
    assert(!DIDPeer.regexPeer0.matches(d))
    assert(!DIDPeer.regexPeer1.matches(d))
    assert(!DIDPeer.regexPeer2.matches(d))
    // TODO numalgo 3
    assert(DIDPeer.regexPeer4.matches(d))
    assert(!DIDPeer.regexPeer4_LONG.matches(d))
    assert(DIDPeer.regexPeer4_SHORT.matches(d))
  }

  test("Create DIDPeer apply ex5_peer1") {
    val s = DIDSubject(ex5_peer1)
    DIDPeer.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did)  =>
        assertEquals(did, DIDPeer1("zQmZMygzYqNwU6Uhmewx5Xepf2VLp5S4HLSwwgf2aiKZuwa"))
        assertEquals(did.string, s.string)
  }

  test("Create DIDPeer apply ex4_peer2_did") {
    val s = DIDSubject(ex4_peer2_did)
    DIDPeer.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did)  =>
        assertEquals(
          did,
          DIDPeer2(
            Seq(
              DIDPeer2.ElementE(Multibase("z6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc")),
              DIDPeer2.ElementV(Multibase("z6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V")),
              DIDPeer2.ElementV(Multibase("z6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg")),
              DIDPeer2.ElementService(
                "eyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0"
              ),
            )
          )
        )
        assertEquals(did.string, s.string)
  }

  test("Create DIDPeer apply myExampleDID") {
    val s = DIDSubject(myExampleDID)
    DIDPeer.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did)  =>
        assertEquals(
          did,
          DIDPeer2(
            Seq(
              DIDPeer2.ElementE(Multibase("z6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE")),
              DIDPeer2.ElementV(Multibase("z6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY")),
              DIDPeer2.ElementService(
                "eyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19"
              ),
            )
          )
        )

        assertEquals(did.string, s.string)
        assertEquals(
          Some(did.document.toJsonPretty),
          myExampleDIDDocument.fromJson[Json].map(_.toJsonPretty).toOption
        )
  }

  test("Parse DIDPeer with missing optional fields of the service endpoint") {
    val s = DIDSubject(rootsid_ex_peer2_did)
    DIDPeer.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did)  =>
        assertEquals(did.string, s.string)
        assertEquals(
          Some(did.document.toJsonPretty),
          rootsid_ex_peer2_didDocument.fromJson[Json].map(_.toJsonPretty).toOption
        )
  }

  test("Create DIDPeer apply keys") {
    val keyAgreement = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.X25519,
      d = "9yAs1ddRaUq4d7_HfLw2VSj1oW2kirb2wALmPXrRuZA",
      x = "xfvZlkAnuNpssHOR2As4kUJ8zEPbowOIU5VbhBsYoGo",
    )
    val keyAuthentication = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = "-yjzvLY5dhFEuIsQcebEejbLbl3b8ICR7b2y2_HqFns",
      x = "vfzzx6IIWdBI7J4eEPHuxaXGErhH3QXnRSQd0d_yn0Y",
    )
    val obj =
      DIDPeer2(Seq(keyAgreement, keyAuthentication), Seq(DIDPeerServiceEncodedOld("http://localhost:8080")))

    assertEquals(
      obj,
      DIDPeer(
        DIDSubject(
          "did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.Vz6MksEtp5uusk11aUuwRHzdwfTxJBUaKaUVVXwFSVsmUkxKF.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
        )
      )
    )
  }

  test("Check service's Base64 - decode and encode (did must not change)") {
    def testDid(s: String) = s"did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.S$s"

    /** {"r":[],"s":"http://localhost:8080","a":["didcomm\/v2"],"t":"dm"} */
    val service = "eyJyIjpbXSwicyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImEiOlsiZGlkY29tbVwvdjIiXSwidCI6ImRtIn0="

    /** Default {"t":"dm","s":"http://localhost:8080","r":[],"a":["didcomm/v2"]} */
    val defaultService = "eyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"

    DIDPeer(DIDSubject(testDid(service))) match
      case DIDPeer0(encnumbasis)          => fail("Wrong DIDPeer type")
      case DIDPeer1(encnumbasis)          => fail("Wrong DIDPeer type")
      case DIDPeer3(encnumbasis)          => fail("Wrong DIDPeer type")
      case DIDPeer4LongForm(hash, json)   => fail("Wrong DIDPeer type")
      case DIDPeer4ShortForm(encnumbasis) => fail("Wrong DIDPeer type")
      case obj @ DIDPeer2(elements)       =>
        assertEquals(obj.did, testDid(service))
        assertNotEquals(obj.did, testDid(defaultService))
  }

  test("Support did:peer:2 with multi services (DIDCommMessaging and others types)") {
    val s = DIDSubject(aliceWithMultiService)
    DIDPeer2.fromDID(s) match
      case Left(value) => fail(value)
      case Right(did)  =>
        assertEquals(did.string, s.string)
        assertEquals(did.document.service.toSeq.flatten.size, 4)
        did.document.service.map(_.toSeq) match
          case None                      => fail("Must have two servies instated of none")
          case Some(Seq(s1, s2, s3, s4)) =>
            assertEquals(s1.`type`, "DIDCommMessaging")
            assertEquals(s2.`type`, "DIDCommMessaging")
            assertEquals(s3.`type`, "DIDCommMessaging")
            assertEquals(s4.`type`, "SeriveType123")
            assert(s1.id.endsWith("#service")) // before was "#didcommmessaging-0"
            assert(s2.id.endsWith("#service-1")) // before was "#didcommmessaging-1"
            assert(s3.id.endsWith("#service-2")) // before was "#didcommmessaging-2"
            assert(s4.id.endsWith("#serivetype123-3"))
          case Some(services) => fail(s"Must have only two servies instated of ${services.size}")
  }

  // ##############################################################################################
  // New DIDPeerServiceEncoded -> because of  https://github.com/Indicio-tech/didcomm-demo/issues/2

  /** Old example of the DID Peer's services
    *
    * This example will eventually be removed. Since 'serviceEndpoint' is a string instead of object with 'uri',
    * 'routingKeys' and 'accept' properties inside.
    */
  @deprecated("serviceEndpoint with old structure")
  val ex1Str =
    """[{"t":"dm","s":"https://did.fmgp.app","r":[],"a":["didcomm/v2"]},{"t":"dm","s":"ws://did.fmgp.app","r":[],"a":["didcomm/v2"]}]"""
  val ex1AfterRemoveAbbreviation =
    """[
      |  {
      |    "type" : "DIDCommMessaging",
      |    "serviceEndpoint" : "https://did.fmgp.app",
      |    "routingKeys" : [],
      |    "accept" : ["didcomm/v2"]
      |  },
      |  {
      |    "type" : "DIDCommMessaging",
      |    "serviceEndpoint" : "ws://did.fmgp.app",
      |    "routingKeys" : [],
      |    "accept" : ["didcomm/v2"]
      |  }
      |]""".stripMargin.replaceAll("\n", "").replaceAll(" ", "")

  val ex1Services =
    """[
      |  {
      |    "id" : "did:test:s1#service",
      |    "type" : "DIDCommMessaging",
      |    "serviceEndpoint" : {
      |      "uri" : "https://did.fmgp.app",
      |      "accept" : ["didcomm/v2"],
      |      "routingKeys" : []
      |    }
      |  },
      |  {
      |    "id" : "did:test:s1#service-1",
      |    "type" : "DIDCommMessaging",
      |    "serviceEndpoint" : {
      |      "uri" : "ws://did.fmgp.app",
      |      "accept" : [ "didcomm/v2"],
      |      "routingKeys" : []
      |    }
      |  }
      |]""".stripMargin.replaceAll("\n", "").replaceAll(" ", "")

  val ex2Str =
    """{"t":"dm","s":{"uri":"did:peer:2.SW3sidCI6ImRtIiwicyI6Imh0dHBzOi8vZGlkLmZtZ3AuYXBwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfSx7InQiOiJkbSIsInMiOiJ3czovL2RpZC5mbWdwLmFwcCIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX1d","accept":["didcomm/v2"]}}"""
  val ex2AfterRemoveAbbreviation =
    """{
      |  "type" : "DIDCommMessaging",
      |  "serviceEndpoint" : {
      |    "uri" : "did:peer:2.SW3sidCI6ImRtIiwicyI6Imh0dHBzOi8vZGlkLmZtZ3AuYXBwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfSx7InQiOiJkbSIsInMiOiJ3czovL2RpZC5mbWdwLmFwcCIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX1d",
      |    "accept" : ["didcomm/v2"]
      |  }
      |}""".stripMargin.replaceAll("\n", "").replaceAll(" ", "")
  val ex2Services =
    """[
      |  {
      |    "id" : "did:test:s2#service",
      |    "type" : "DIDCommMessaging",
      |    "serviceEndpoint" : {
      |      "uri" : "did:peer:2.SW3sidCI6ImRtIiwicyI6Imh0dHBzOi8vZGlkLmZtZ3AuYXBwIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfSx7InQiOiJkbSIsInMiOiJ3czovL2RpZC5mbWdwLmFwcCIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX1d",
      |      "accept" : ["didcomm/v2"]
      |    }
      |  }
      |]""".stripMargin.replaceAll("\n", "").replaceAll(" ", "")

  val ex3OldEndpoint = "https://did.fmgp.app"
  val ex3OldStr = """{"t":"dm","s":"https://did.fmgp.app","r":[],"a":["didcomm/v2"]}"""
  val ex3NewStr = """{"t":"dm","s":{"uri":"https://did.fmgp.app","a":["didcomm/v2"]}}"""

  test("test DIDPeerServiceEncoded abbreviation ex1") {
    @scala.annotation.nowarn("cat=deprecation")
    val ret = DIDPeerServiceEncoded.abbreviation(ex1Str.fromJson[Json].getOrElse(???))
    assertEquals(
      ret.toJson,
      ex1AfterRemoveAbbreviation // .fromJson[Json].getOrElse(???).toJson
    )
  }

  test("test DIDPeerServiceEncoded abbreviation ex2") {
    val ret = DIDPeerServiceEncoded.abbreviation(ex2Str.fromJson[Json].getOrElse(???))
    assertEquals(
      ret.toJson,
      ex2AfterRemoveAbbreviation.fromJson[Json].getOrElse(???).toJson
    )
  }

  test("test DIDPeerServiceEncoded get services ex1") {
    @scala.annotation.nowarn("cat=deprecation")
    val service = DIDPeerServiceEncodedNew(Base64.encode(ex1Str))
      .getDIDService(didSubject = DIDSubject("did:test:s1"), previouslyNumberOfService = 0)
    assertEquals(service.size, 2)
    assertEquals(service.toJson, ex1Services)
  }

  test("test DIDPeerServiceEncoded get services ex2") {
    val service = DIDPeerServiceEncodedNew(Base64.encode(ex2Str))
      .getDIDService(didSubject = DIDSubject("did:test:s2"), previouslyNumberOfService = 0)
    assertEquals(service.size, 1)
    assertEquals(service.toJson, ex2Services)
  }

  test("test ElementService with DIDPeerServiceEncodedNew ex3") {
    val serviceNew = DIDPeerServiceEncodedNew(Base64.encode(ex3NewStr))

    val elementServiceNew = DIDPeer2.ElementService(serviceNew)
    assertEquals(elementServiceNew.base64, Base64.encode(ex3NewStr).urlBase64)

    val serviceOld = DIDPeerServiceEncodedOld(ex3OldEndpoint)
    val elementServiceOld = DIDPeer2.ElementService(serviceOld)
    assertEquals(elementServiceOld.base64, Base64.encode(ex3OldStr).urlBase64)
  }

  test("method makeAgent must fill the 'kid' based on index") {
    val alice = DIDPeer2.makeAgent(
      Seq(
        OKPPrivateKey( // keyAgreement
          kty = KTY.OKP,
          crv = Curve.X25519,
          d = "Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c",
          x = "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw",
        ),
        OKPPrivateKey( // keyAuthentication
          kty = KTY.OKP,
          crv = Curve.Ed25519,
          d = "INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug",
          x = "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
        )
      )
    )
    val kidPattern: Regex = "^.*#key-[0-9]+$".r
    alice.keyStore.keys.foreach { key => assert(kidPattern.matches(key.kid), "'kid' must follow the kid pattern") }
  }
}
