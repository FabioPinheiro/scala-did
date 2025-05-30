package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto._
import fmgp.util.hex2bytes
import fmgp.prism.SSI

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

  def namespace: String = "prism"
  def applyUnsafe(did: String): DIDPrism = DIDPrism(DIDSubject(did).specificId)

  def fromDID(did: DID): Either[String, DIDPrism] = did.string match {
    case regexPrismShortForm(hash)      => Right(DIDPrism(hash))
    case regexPrismLongForm(hash, data) => Right(DIDPrism(s"$hash:$data"))
    case any if regexPrism.matches(any) => Left(s"Not a did:prism '$any'") // TODO make Error type
    case any                            => Left(s"Not a did:prism '$any'")
    // FIXME what about case any ??? //TODO add test in DIDPeerSuite
  }

  def fromSSI(ssi: SSI): DIDPrism = ssi.didPrism
  // // def fromCreateEvent(op: MySignedPrismOperation[CreateDidOP | UpdateDidOP | DeactivateDidOP]) =
  // def fromCreateEvent(op: MySignedPrismOperation[CreateDidOP]) =
  //   SSI.init(DIDPrism(op.opHash)).append(op).didDocument
}
