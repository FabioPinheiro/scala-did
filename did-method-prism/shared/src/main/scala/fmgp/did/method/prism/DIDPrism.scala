package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto._
import fmgp.util._
import fmgp.did.method.prism.proto._

/** DID Prism (only short form)
  *
  * @see
  *   https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md
  *
  * TODO Long form DID
  */
case class DIDPrism(specificId: String) extends DID {
  final def namespace: String = DIDPrism.namespace

  def domainName: String = specificId.split(":").head // NOTE split always have head

  def hashRef: Array[Byte] = hex2bytes(specificId)

}

object DIDPrism {

  given decoder: JsonDecoder[DIDPrism] = DIDSubject.decoder.mapOrFail(did => DIDPrism.fromDID(did))
  // JsonDecoder.string.map(DIDPrism(_))
  given encoder: JsonEncoder[DIDPrism] = DIDSubject.encoder.contramap(did => did.asDIDSubject)
  // JsonEncoder.string.contramap[DIDPrism](_.specificId)

  /** Syntax
    * {{{
    *   prism-did          = "did:prism:" initial-hash [encoded-state]
    *   initial-hash       = 64HEXDIGIT
    *   encoded-state      = ":" 1*id-char
    *   id-char            = ALPHA / DIGIT / "-" / "_"
    * }}}
    */
  def regexPrism = "^did:prism:([0-9a-f]{64})(?::([A-Za-z0-9_-]+))?$".r
  def regexPrismShortForm = "^did:prism:([0-9a-f]{64})$".r
  def regexPrismLongForm = "^did:prism:([0-9a-f]{64}):([A-Za-z0-9_-]+)$".r

  val namespace: String = "prism"
  def applyUnsafe(did: String): DIDPrism = DIDPrism(DIDSubject(did).specificId)

  def fromEventHash(hash: Array[Byte]): DIDPrism = DIDPrism(bytes2Hex(hash))
  def fromEvent(event: MySignedPrismOperation[OP]): DIDPrism = // Either[String, DIDPrism]
    event.operation match
      case CreateDidOP(publicKeys, services, context) => applyUnsafe(event.eventHash.hex)
      case _                                          => ??? // FIXME

  def fromString(string: String): Either[String, DIDPrism] = string match {
    case regexPrismShortForm(hash)      => Right(DIDPrism(hash))
    case regexPrismLongForm(hash, data) => Right(DIDPrism(s"$hash:$data"))
    case any if regexPrism.matches(any) => Left(s"Not a did:prism '$any' - (But regexPrism for did PRISM ?)")
    case DID.regex(namespace, specificId) =>
      if (namespace == DIDPrism.namespace) Left(s"Invalid specificId for a did:prism '$specificId'")
      else Left(s"Expected the did method 'prism' instead of '$namespace'")
    case any => Left(s"Not a did '$any'")
    // TODO REGEX for a DID but not a DID PRISM
    // FIXME what about case any ??? //TODO add test in DIDPeerSuite
  }

  def fromDID(did: DID): Either[String, DIDPrism] = fromString(did.string)
  def fromSSI(ssi: SSI): DIDPrism = ssi.didPrism
  // // def fromCreateEvent(op: MySignedPrismOperation[CreateDidOP | UpdateDidOP | DeactivateDidOP]) =
  // def fromCreateEvent(op: MySignedPrismOperation[CreateDidOP]) =
  //   SSI.init(DIDPrism(op.opHash)).append(op).didDocument
}
