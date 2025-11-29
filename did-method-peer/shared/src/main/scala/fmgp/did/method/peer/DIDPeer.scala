package fmgp.did.method.peer

import zio.*
import fmgp.did.*
import fmgp.util.*
import fmgp.multibase.*
import fmgp.crypto.*
import zio.json.*
import zio.json.ast.Json
import javax.lang.model.element.Element
import fmgp.multibase.Base.Base58BTC
import fmgp.multiformats.*

/** DID Peer
  *
  * @see
  *   https://identity.foundation/peer-did-method-spec/#method-specific-identifier
  */
sealed trait DIDPeer extends DID {
  def namespace: String = "peer"
  def specificId: String
  def numalgo: 0 | 1 | 2 | 3 | 4

  // FIXME Its not possible to resolve for numalgo 3 and the short form of 4 Just from the DID https://github.com/FabioPinheiro/scala-did/issues/323
  def document: DIDDocument
}

case class DIDPeer0(encnumbasis: DIDPeer.Array46_BASE58BTC) extends DIDPeer {
  override def numalgo: 0 = 0
  override def specificId: String = "" + numalgo + encnumbasis
  override def document: DIDDocument = ??? // FIXME
}
case class DIDPeer1(encnumbasis: DIDPeer.Array46_BASE58BTC) extends DIDPeer {
  override def numalgo: 1 = 1
  override def specificId: String = "" + numalgo + encnumbasis
  override def document: DIDDocument = ??? // FIXME
}
case class DIDPeer2(elements: Seq[DIDPeer2.Element]) extends DIDPeer {
  override def numalgo: 2 = 2
  override def specificId: String = "" + numalgo + elements.map(_.encode).mkString(".", ".", "")

  def indexedElements = elements.zipWithIndex

  override def document: DIDDocument = DIDDocumentClass(
    id = this,
    authentication = Some(
      indexedElements.collect { case (e: DIDPeer2.ElementV, index: Int) =>
        VerificationMethodEmbeddedJWK(
          id = this.string + "#key-" + (index + 1),
          controller = this.did,
          `type` = VerificationMethodType.JsonWebKey2020,
          publicKeyJwk = OKPPublicKey(
            kty = KTY.OKP,
            crv = Curve.Ed25519, // TODO PLZ FIX BLACKMAGIC of did:peer! (zero documentation & unexpected logic)
            x = DIDPeer.decodeKey(e.mb.value),
          )
        )
      }
    ),
    keyAgreement = Some(
      indexedElements.collect { case (e: DIDPeer2.ElementE, index: Int) =>
        VerificationMethodEmbeddedJWK(
          id = this.string + "#key-" + (index + 1),
          controller = this.did,
          `type` = VerificationMethodType.JsonWebKey2020,
          publicKeyJwk = OKPPublicKey(
            kty = KTY.OKP,
            crv = Curve.X25519, // TODO PLZ FIX BLACKMAGIC of did:peer! (zero documentation & unexpected logic)
            x = DIDPeer.decodeKey(e.mb.value),
          )
        )
      }.toSet
    ),
    service = Some(
      elements
        .collect { case e: DIDPeer2.ElementService => e }
        .foldLeft(Set.empty[DIDService])((acc, ele) =>
          acc ++ DIDPeerServiceEncodedNew(Base64(ele.base64))
            .getDIDService(DIDSubject(this.string), previouslyNumberOfService = acc.size)
        )
    ).filterNot(_.isEmpty)
  )
}
object DIDPeer2 {
  // type Purposecode = 'A' | 'E' | 'V' | 'I' | 'D' | 'S'
  sealed trait Element { def encode: String } // TODO

  /** A - keyAgreement */
  case class ElementA(mb: Multibase) extends Element { def encode = "A" + mb }
  case class ElementE(mb: Multibase) extends Element { def encode = "E" + mb }

  /** V - authentication key */
  case class ElementV(mb: Multibase) extends Element { def encode = "V" + mb }
  case class ElementI(mb: Multibase) extends Element { def encode = "I" + mb }
  case class ElementD(mb: Multibase) extends Element { def encode = "D" + mb }

  type C1_B64URL = String
  case class ElementService(base64: C1_B64URL) extends Element { def encode = "S" + base64 }

  object ElementService {
    def apply(obj: DIDPeerServiceEncoded): ElementService =
      obj match
        case DIDPeerServiceEncodedNew(base64)           => new ElementService(base64.urlBase64)
        case obj @ DIDPeerServiceEncodedOld(t, s, r, a) => new ElementService(Base64.encode(obj.toJson).urlBase64)
  }

  // case class Element(code: Purposecode, value: String) { def encode = "" + code + value }

  /** {{{
    * DIDPeer2(
    *   Seq(
    *     fmgp.crypto.KeyGenerator.unsafeX25519,
    *     fmgp.crypto.KeyGenerator.unsafeEd25519
    *   ),
    *   Seq(DIDPeerServiceEncoded.fromEndpoint("http://localhost:8080"))
    * ).did
    * }}}
    */
  def apply(keys: Seq[PrivateKey], service: Seq[DIDPeerServiceEncoded] = Seq.empty): DIDPeer2 =
    DIDPeer2(keys.map(keyToElement(_)) ++ service.map(ElementService(_)))

  def makeAgent(keySeq: Seq[PrivateKey], service: Seq[DIDPeerServiceEncoded] = Seq.empty): DIDPeer.AgentDIDPeer =
    new DIDPeer.AgentDIDPeer {
      override val id: DIDPeer2 = apply(keySeq, service)
      override val keyStore: KeyStore = KeyStore(
        keySeq.zipWithIndex.map { case (key, index) =>
          key match
            case k: OKPPrivateKeyWithKid => k
            case k: OKPPrivateKey        => k.withKid(id.did + "#key-" + (index + 1))
            case k: ECPrivateKeyWithKid  => k
            case k: ECPrivateKey         => k.withKid(id.did + "#key-" + (index + 1))
        }.toSet
      )
    }

  def keyToElement(key: PrivateKey) = key match {
    case key: OKPPrivateKey =>
      key.crv match
        case Curve.X25519 =>
          DIDPeer2.ElementE(
            Multibase.encode(
              Base58BTC,
              Array(-20.toByte, 1.toByte).map(_.toByte) ++ Base64(key.x).decode // TODO refactoring
            )
          )
        case Curve.Ed25519 =>
          DIDPeer2.ElementV(
            Multibase.encode(
              Base58BTC,
              Array(-19.toByte, 1.toByte).map(_.toByte) ++ Base64(key.x).decode // TODO refactoring
            )
          )
    case _: ECPrivateKey => ??? // TODO
  }

  /** This is the old (undefined) format of kid based on the key's encoded */
  @deprecated("The new format of the kid is based on index")
  def keyKidAbsolute(key: PrivateKey, did: DIDPeer) =
    key.withKid(did.did + "#" + keyToElement(key).encode.drop(2)) // FIXME .drop(2) 'Sz'

  def keyKidRelative(key: PrivateKey) = key match
    case k: OKPPrivateKey =>
      k.withKid(keyToElement(k).encode.drop(2)) // FIXME .drop(2) 'Sz'
    case k: ECPrivateKey =>
      k.withKid(keyToElement(k).encode.drop(2)) // FIXME .drop(2) 'Sz'

  def fromDID(did: DID): Either[String, DIDPeer2] = did.string match {
    case DIDPeer.regexPeer2(all, str*) =>
      val elements = all
        .drop(1) // drop peer type number
        .split('.')
        .toSeq
        .map {
          case s if s.startsWith("A") => DIDPeer2.ElementA(Multibase(s.drop(1)))
          case s if s.startsWith("E") => DIDPeer2.ElementE(Multibase(s.drop(1)))
          case s if s.startsWith("V") => DIDPeer2.ElementV(Multibase(s.drop(1)))
          case s if s.startsWith("I") => DIDPeer2.ElementI(Multibase(s.drop(1)))
          case s if s.startsWith("D") => DIDPeer2.ElementD(Multibase(s.drop(1)))
          case s if s.startsWith("S") => DIDPeer2.ElementService(s.drop(1)) // Base64
        }
      Right(DIDPeer2(elements))
    case any if DIDPeer.regexPeer.matches(any) => Left(s"Not a did:peer:2... '$any'") // FIXME make Error type
  }

}

case class DIDPeer3(encnumbasis: DIDPeer.Array46_BASE58BTC) extends DIDPeer {
  override def numalgo: 3 = 3
  override def specificId: String = "" + numalgo + encnumbasis

  // FIXME Its not possible to resolve for numalgo 3 and the short form of 4 Just from the DID https://github.com/FabioPinheiro/scala-did/issues/323
  override def document: DIDDocument = ???
}

/** @see
  *   https://identity.foundation/peer-did-method-spec/#method-4-short-form-and-long-form
  */
sealed trait DIDPeer4 extends DIDPeer {
  override def numalgo: 4 = 4

  def shortPart: Multibase
}

object DIDPeer4 {

  /**   - 1) Take SHA2-256 digest of the encoded document (encode the bytes as utf-8)
    *   - 2) Prefix these bytes with the multihash prefix for SHA2-256 and the hash length (varint 0x12 for prefix,
    *     varint 0x20 for 32 bytes in length)
    *   - 3) Multibase encode the bytes as base58btc (base58 encode the value and prefix with a z)
    *   - 4) Consider this value the hash
    *
    * @param hash
    *   digest of the encoded document (encode the bytes as utf-8)
    */
  def encodeHash(hash: Array[Byte]): Multibase = Base58BTC.encode(Multihash(Codec.sha2_256, hash).bytes)
  // def calculateAndEncodeHash(initDoc: Json.Obj): Multibase = encodeHash(calculateHash(initDoc))
  // def calculateAndEncodeHash(encodeDocument: Multibase): Multibase = encodeHash(calculateHash(encodeDocument))
  def calculateHash(initDoc: Json.Obj): Array[Byte] = calculateHash(encodeDocument(initDoc))
  def calculateHash(encodeDocument: Multibase): Array[Byte] = SHA256.digest(encodeDocument.value)

  /**   - 1) JSON stringify the object without whitespace
    *   - 2) Encode the string as utf-8 bytes
    *   - 3) Prefix the bytes with the multicodec prefix for json (varint 0x0200)
    *   - 4) Consider this value the hash
    *
    * @param json
    */
  def encodeDocument(json: Json.Obj): Multibase = Base58BTC.encode(Multicodec(Codec.json, json.toJson.getBytes).bytes)

  def decodeDocument(multibase: Multibase): Either[String, Json.Obj] = {
    Multicodec.fromBytes(multibase.decode) match
      case Left(error)                              => Left(s"DIDPeer4 fail to decode Document part: $error")
      case Right(Multicodec(Codec.json, dataBytes)) =>
        new String(dataBytes, java.nio.charset.StandardCharsets.UTF_8).fromJson[Json.Obj] match
          case Left(parseError) => Left(s"DIDPeer4 fail to parse Document part as Json: $parseError")
          case Right(jsonObj)   => Right(jsonObj)
      case Right(Multicodec(codec, dataBytes)) =>
        Left(s"DIDPeer4 expected the Document part to be Multicodec of 'json' instade of '$codec'")
  }

  /** To “contextualize” a document:
    *   - Take the decoded document
    *   - Add id at the root of the document and set it to the DID
    *   - Add alsoKnownAs at the root of the document and set it to a list, if not already present, and append the short
    *     form of the DID
    *   - For each verification method (declared in the verificationMethod section or embedded in a verification
    *     relationship like authentication): - If controller is not set, set controller to the DID
    */
  def contextualize(decodedDocument: Json.Obj, did: DID, alsoKnownAs: DID): Either[String, DIDDocument] = {

    def withAbsoluteRef(did: DIDSubject, obj: Json): Json = {
      obj match
        case ref: Json.Str =>
          ref match
            case Json.Str(value) if value.startsWith("#") => Json.Str(did.string + value)
            case str: Json.Str                            => str
        case entry: Json.Obj => // VerificationMethodEmbedded
          entry.get("id") match
            case None                                       => ??? // FIXME
            case Some(Json.Str(ref)) if ref.startsWith("#") => entry.add("id", Json.Str(did.string + ref))
            case Some(Json.Str(ref))                        => entry
            case Some(_)                                    => ??? // FIXME
        case any => ??? // any // TODO Error!
    }

    /** ifMissingAddControllerToVerificationMethodEmbedded */
    def withController(obj: Json): Json = {
      obj match
        case ref: Json.Str   => ref // VerificationMethodReferenced
        case entry: Json.Obj => // VerificationMethodEmbedded
          entry.get("controller") match
            case None    => entry.add("controller", Json.Str(did.string))
            case Some(_) => entry
        case any => any // TODO Error!
    }

    def withAlsoKnownAs(doc: Json.Obj): Json.Obj = {
      val aux = Json.Str(alsoKnownAs.string)
      doc.get("alsoKnownAs") match
        case None                => doc.add("alsoKnownAs", Json.Arr(aux))
        case Some(arr: Json.Arr) => doc.add("alsoKnownAs", Json.Arr(arr.elements.appended(aux)))
        case Some(value)         => doc // TODO ERROR
    }

    inline def fAux(obj: Json) = withAbsoluteRef(did, withController(obj))
    val didDocument: Json.Obj = decodedDocument
      .add("id", Json.Str(did.string))
      .mapObject(withAlsoKnownAs)
      .mapObjectEntries {
        case ("verificationMethod", arr: Json.Arr) => ("verificationMethod", arr.mapArrayValues(fAux(_)))
        case ("authentication", u: Json.Obj)       => ??? // TODO https://github.com/FabioPinheiro/scala-did/issues/322
        case ("authentication", arr: Json.Arr)     => ("authentication", arr.mapArrayValues(fAux(_)))
        case ("assertionMethod", u: Json.Obj)      => ??? // TODO https://github.com/FabioPinheiro/scala-did/issues/322
        case ("assertionMethod", arr: Json.Arr)    => ("assertionMethod", arr.mapArrayValues(fAux(_)))
        case ("keyAgreement", arr: Json.Arr)       => ("keyAgreement", arr.mapArrayValues(fAux(_)))
        case ("capabilityInvocation", u: Json.Obj) => ??? // TODO https://github.com/FabioPinheiro/scala-did/issues/322
        case ("capabilityInvocation", arr: Json.Arr) => ("capabilityInvocation", arr.mapArrayValues(fAux(_)))
        case ("capabilityDelegation", u: Json.Obj) => ??? // TODO https://github.com/FabioPinheiro/scala-did/issues/322
        case ("capabilityDelegation", arr: Json.Arr) => ("capabilityDelegation", arr.mapArrayValues(fAux(_)))
        case entry                                   => entry
      }
    DIDDocument.decoder.fromJsonAST(didDocument)
  }

  /** @param keySeq
    *   MUST have relative references
    * @param initDoc
    *   that will create the DID
    * @return
    *   AgentDIDPeer for the long form of did:peer:4
    */
  def makeAgentLongForm(keySeq: Seq[PrivateKeyWithKid], initDoc: Json.Obj): DIDPeer.AgentDIDPeer =
    new DIDPeer.AgentDIDPeer {
      override val id: DIDPeer4 = DIDPeer4.fromInitDocument(initDoc)
      override val keyStore: KeyStore = KeyStore(
        keySeq.zipWithIndex.map { case (key, index) =>
          key match {
            case k: OKPPrivateKeyWithKid if k.kid.startsWith("#") => k.withKid(id.string + k.kid)
            case k: OKPPrivateKeyWithKid                          => k
            // case k: OKPPrivateKey        => k.withKid(id.did + "#key-" + (index + 1))
            case k: ECPrivateKeyWithKid if k.kid.startsWith("#") => k.withKid(id.string + k.kid)
            case k: ECPrivateKeyWithKid                          => k
            // case k: ECPrivateKey         => k.withKid(id.did + "#key-" + (index + 1))
          }
        }.toSet
      )
    }

  /** @param keySeq
    *   MUST have relative references
    * @param initDoc
    *   that will create the DID
    * @return
    *   AgentDIDPeer for the short form of did:peer:4
    */
  def makeAgentShortForm(keySeq: Seq[PrivateKeyWithKid], initDoc: Json.Obj): DIDPeer.AgentDIDPeer =
    new DIDPeer.AgentDIDPeer {
      override val id: DIDPeer4 = DIDPeer4.fromInitDocument(initDoc).toShortForm
      override val keyStore: KeyStore = KeyStore(
        keySeq.zipWithIndex.map { case (key, index) =>
          key match {
            case k: OKPPrivateKeyWithKid if k.kid.startsWith("#") => k.withKid(id.string + k.kid)
            case k: OKPPrivateKeyWithKid                          => k
            // case k: OKPPrivateKey        => k.withKid(id.did + "#key-" + (index + 1))
            case k: ECPrivateKeyWithKid if k.kid.startsWith("#") => k.withKid(id.string + k.kid)
            case k: ECPrivateKeyWithKid                          => k
            // case k: ECPrivateKey         => k.withKid(id.did + "#key-" + (index + 1))
          }
        }.toSet
      )
    }

  def fromInitDocument(doc: Json.Obj): DIDPeer4LongForm = {
    val eDoc = encodeDocument(doc)
    val hash = calculateHash(eDoc)
    val eHash = encodeHash(hash)
    DIDPeer4LongForm(eHash, eDoc)
  }

  def fromDID(did: DID): Either[String, DIDPeer4] = did.string match {
    case DIDPeer.regexPeer4_LONG(hashMultibase, docMultibase) =>
      Right(DIDPeer4LongForm(Multibase(hashMultibase), Multibase(docMultibase)))
    case DIDPeer.regexPeer4_SHORT(hashMultibase) => Right(DIDPeer4ShortForm(Multibase(hashMultibase)))
    case any                                     => Left(s"Not a did:peer:4 '$any'")
  }

}
case class DIDPeer4LongForm(
    override val shortPart: Multibase, // TODO BASE58BTC
    longPart: Multibase // TODO BASE58BTC
) extends DIDPeer4 {

  override def specificId: String = "" + numalgo + shortPart.value + ":" + longPart.value
  override def document: DIDDocument = {
    val initDoc = DIDPeer4.decodeDocument(longPart).getOrElse(???) // FIXME
    DIDPeer4.contextualize(initDoc, this, this.toShortForm).getOrElse(???) // FIXME
  }

  def toShortForm: DIDPeer4ShortForm = DIDPeer4ShortForm(shortPart)

  def longPartAsJson = Multicodec.fromBytes(longPart.decode) match
    case Left(value)                              => Left(value)
    case Right(Multicodec(Codec.json, dataBytes)) =>
      println(dataBytes.mkString)
      dataBytes.mkString("_").fromJson[Json]
    case Right(Multicodec(codec, dataBytes)) => Left(s"Expected Multicodec to be json instade of $codec")

}
case class DIDPeer4ShortForm(
    override val shortPart: Multibase, // TODO BASE58BTC
) extends DIDPeer4 {
  override def specificId: String = "" + numalgo + shortPart.value

  // FIXME Its not possible to resolve for numalgo 3 and the short form of 4 Just from the DID https://github.com/FabioPinheiro/scala-did/issues/323
  override def document: DIDDocument = ???
}

object DIDPeer {
  type Array46_BASE58BTC = String // TODO  "46*BASE58BTC"

  trait AgentDIDPeer extends Agent {
    override def id: DIDPeer
  }

  def regexPeer =
    ("^did:peer:(" +
      "([01](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))" +
      "|" +
      "(2((\\.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)[0-9a-zA-Z=]*)*))" +
      "|" +
      "(4zQm[" + BASE58_ALPHABET + "]{44}(?::z[" + BASE58_ALPHABET + "]{6,})?)"
      + ")$").r // TODO for 3
  def regexPeer0 = "^did:peer:(0(z)([1-9a-km-zA-HJ-NP-Z]{46,47}))$".r
  def regexPeer1 = "^did:peer:(1(z)([1-9a-km-zA-HJ-NP-Z]{46,47}))$".r
  // #regexPeer2 = "^did:peer:(2((\\.[AEVID](z)([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)[0-9a-zA-Z=]*)*))$".r
  def regexPeer2 = "^did:peer:2((\\.([AEVID])z([1-9a-km-zA-HJ-NP-Z]{46,47}))+(\\.(S)([0-9a-zA-Z=]*))*)$".r
  // def regexPeer3 = "^did:peer:3.*$".r
  private def BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
  def regexPeer4 = ("^did:peer:4zQm[" + BASE58_ALPHABET + "]{44}(?::z[" + BASE58_ALPHABET + "]{6,})?$").r
  def regexPeer4_LONG = ("^did:peer:4(zQm[" + BASE58_ALPHABET + "]{44}):(z[" + BASE58_ALPHABET + "]{6,})$").r
  def regexPeer4_SHORT = ("^did:peer:4(zQm[" + BASE58_ALPHABET + "]{44})$").r

  def apply(didSubject: DIDSubject): DIDPeer = fromDID(didSubject.toDID).toOption.get // FIXME!

  def fromDID(did: DID): Either[String, DIDPeer] = did.string match {
    case regexPeer0(all, z: "z", data) => Right(DIDPeer0(all.drop(1)))
    case regexPeer1(all, z: "z", data) => Right(DIDPeer1(all.drop(1)))
    case regexPeer2(all, str*)         => DIDPeer2.fromDID(did)
    // case regexPeer3(all, str*)         => ??? // FIXME
    case regexPeer4() => DIDPeer4.fromDID(did)
    // case any if regexPeer.matches(any) => Left(s"Not a did:peer '$any'") // TODO make Error type
    case any => Left(s"Not a did:peer '$any'")
    // FIXME what about case any ??? //TODO add test in DIDPeerSuite
  }

  def decodeKey(data: String): String = {
    val tmp = Multibase(data).decode
      .drop(1) // FIXME drop 1 for the multicodec type (0x12) and another becuase the type the multihash size
      .drop(1) // FIXME drop 1 another becuase the type is multihash and this byte (0x20) is for the size
    Base64.encode(tmp).urlBase64
  }

}
