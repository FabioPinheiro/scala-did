package fmgp.did.method.peer

import munit._
import zio._
import zio.json._
import fmgp.crypto._
import fmgp.did._
import fmgp.did.method.peer._
import fmgp.multibase._

import DIDPeerExamples._
import fmgp.did.method.peer.DidPeerResolver
import zio.json.ast.Json
import fmgp.util.Base64
import scala.util.matching.Regex

/** didResolverPeerJVM/testOnly fmgp.did.method.peer.DIDPeer4Suite */
class DIDPeer4Suite extends ZSuite {

  import DIDPeerExamples.{didpeer4initDoc, didpeer4long, didpeer4short}

  test("Regex for did:peer:4") {
    didpeer4long.string match
      case DIDPeer.regexPeer4() =>
      case _                    => fail("didpeer4long fail to match regex ")
    didpeer4short.string match
      case DIDPeer.regexPeer4() =>
      case _                    => fail("didpeer4short fail to match regex")
  }
  test("Regex for did:peer:4 long") {
    didpeer4long.string match
      case DIDPeer.regexPeer4_LONG(hash, doc) => // ok
      case _                                  => fail("didpeer4long fail to match regex for Long")
    didpeer4long.string match
      case DIDPeer.regexPeer4_SHORT(hash) => fail("didpeer4long MUST NOT match regex for Short")
      case _                              => // ok
  }
  test("Regex for did:peer:4 short") {
    didpeer4short.string match
      case DIDPeer.regexPeer4_LONG(hash, doc) => fail("didpeer4short MUST NOT match regex for long")
      case _                                  => // ok
    didpeer4short.string match
      case DIDPeer.regexPeer4_SHORT(hash) => // ok
      case _                              => fail("didpeer4short fail to match regex for Short")
  }

  test("Encode document for did:peer:4") {
    val ret = DIDPeer4.encodeDocument(didpeer4initDoc)
    assertEquals(obtained = ret.value, expected = DIDPeerExamples.didpeer4docEncodedExpected)
  }

  test("Hash of the encoded document for did:peer:4") {
    val multibase = DIDPeer4.encodeHash(DIDPeer4.calculateHash(didpeer4initDoc))
    assertEquals(obtained = multibase.value, expected = DIDPeerExamples.didpeer4docHashExpected)
  }
  test("Create DID fromInitDocument") {
    val did = DIDPeer4.fromInitDocument(didpeer4initDoc)
    assertEquals(did.string, didpeer4long.string)
    assertEquals(did.toShortForm.string, didpeer4short.string)
  }
  test("Create DID DIDPeer4.fromDID") {
    DIDPeer4.fromDID(didpeer4long) match
      case Left(error) => fail(s"Fail to parse DID: $error")
      case Right(did)  => assertEquals(did.string, didpeer4long.string)
    DIDPeer4.fromDID(didpeer4short) match
      case Left(error) => fail(s"Fail to parse DID: $error")
      case Right(did)  => assertEquals(did.string, didpeer4short.string)
  }
  test("Create DID DIDPeer.fromDID") {
    DIDPeer.fromDID(didpeer4long) match
      case Left(error) => fail(s"Fail to parse DID: $error")
      case Right(did)  => assertEquals(did.string, didpeer4long.string)
    DIDPeer.fromDID(didpeer4short) match
      case Left(error) => fail(s"Fail to parse DID: $error")
      case Right(did)  => assertEquals(did.string, didpeer4short.string)
  }
  test("Decode document for did:peer:4") {
    import zio.json.*
    val multibase = DIDPeer4.encodeDocument(didpeer4initDoc)
    val obtainedDoc = DIDPeer4.decodeDocument(multibase).getOrElse(???)
    assertEquals(obtained = obtainedDoc, expected = didpeer4initDoc)
  }

  test("Contextualize Document") {
    DIDPeer4.contextualize(didpeer4initDoc, didpeer4long, didpeer4short) match
      case Left(error)         => fail(error)
      case Right(mDIDDocument) => // ok
  }
  test("Resolve DID into a DIDDocument") {
    DIDPeer4.fromDID(didpeer4long) match
      case Left(error) => fail(s"Fail to parse DID: $error")
      case Right(did)  =>
        assertEquals(
          did.document.toJsonAST.toOption.flatMap(_.asObject).get,
          DIDPeerExamples.didpeer4docAfterResolveLongForm
        )
  }

  val initDoc2 = """{"alsoKnownAs": ["did:x:1"]}""".stripMargin.fromJson[Json.Obj].getOrElse(???)
  val did2long = "did:peer:4zQmeJTUjKekd3C5su6huiCrCwKPS1EMPcScNUSzMA5Tr9Rt:z6p5GJREE8K167ndFihTbDPZG5zMUYCAENmdLVRek"
  val did2short = "did:peer:4zQmeJTUjKekd3C5su6huiCrCwKPS1EMPcScNUSzMA5Tr9Rt"
  test("Contextualize Document initDoc2") {
    val did2 = DIDPeer4.fromInitDocument(initDoc2)
    DIDPeer4.contextualize(initDoc2, DIDSubject(did2short), DIDSubject(did2long)) match
      case Left(error)         => fail(error)
      case Right(mDIDDocument) =>
        mDIDDocument.alsoKnownAs match
          case None                       => fail("alsoKnownAs is missing")
          case Some(arr) if arr.size == 2 => // ok
          case Some(arr) => fail("alsoKnownAs MUST Contain the previous elements plus the other did:peer:4 form")
  }

  test("Make Agent - DIDComm Relay") {
    //  With well knowed alice's keys
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
      ) match
      case Left(error)  => fail(s"Unable to create agent: $error")
      case Right(agent) =>
        assertEquals(agent.id.document.allKeysTypeKeyAgreement.size, 1)
        assertEquals(agent.id.document.keyAgreement.size, 1)
  }

}
