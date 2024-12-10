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
      case Right(did) =>
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
      case Left(error) => fail(error)
      case Right(mDIDDocument) =>
        mDIDDocument.alsoKnownAs match
          case None                       => fail("alsoKnownAs is missing")
          case Some(arr) if arr.size == 2 => // ok
          case Some(arr) => fail("alsoKnownAs MUST Contain the previous elements plus the other did:peer:4 form")
  }

}
