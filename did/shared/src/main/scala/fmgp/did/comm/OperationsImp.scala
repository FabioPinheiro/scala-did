package fmgp.did.comm

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto.error._
import fmgp.crypto._

/** TODO Fix all FIXME */
object OperationsImp {
  val layer: URLayer[CryptoOperations, Operations] =
    ZLayer.fromFunction(OperationsImp(_))
}

class OperationsImp(cryptoOperations: CryptoOperations) extends Operations {

  def sign(msg: PlaintextMessage): ZIO[Agent, CryptoFailed, SignedMessage] =
    for {
      agent <- ZIO.service[Agent]
      key = agent.keyStore.keys.toSeq.filter {
        case OKPPrivateKey(kty, Curve.X25519, d, x, kid) => false // TODO UnsupportedCurve
        case _                                           => true
      }.head // FIXME head
      ret <- cryptoOperations.sign(key, msg)
    } yield ret

  def verify(msg: SignedMessage): ZIO[Resolver, CryptoFailed, Boolean] = {
    for {
      resolver <- ZIO.service[Resolver]
      doc <- resolver
        .didDocument(FROMTO.force(msg.signatures.head.header.get.kid))
        .catchAll(error => ???) // FIXME
      key = {
        // FIXME get the correct key
        doc.keyAgreement.get.head match {
          case e: VerificationMethodReferenced        => ??? : PublicKey // FIXME
          case e: VerificationMethodEmbeddedJWK       => e.publicKeyJwk
          case e: VerificationMethodEmbeddedMultibase => ??? : PublicKey // FIXME
        }
      }
      ret <- cryptoOperations.verify(key, msg)
    } yield ret
  }

  override def anonEncrypt(msg: PlaintextMessage): ZIO[Resolver, DidFail, EncryptedMessage] = {
    // TODO return EncryptionFailed.type on docs
    for {
      resolver <- ZIO.service[Resolver]
      docs <- ZIO.foreach(msg.to.toSeq.flatten)(resolver.didDocument(_))
      recipientKidsKeys = docs.flatMap(_.keyAgreementAll).map(_.pair)
      ret <- cryptoOperations.anonEncrypt(recipientKidsKeys, msg.toJson.getBytes)
    } yield ret
  }

  def authEncrypt(msg: PlaintextMessage): ZIO[Agent & Resolver, DidFail, EncryptedMessage] = {
    // TODO return EncryptionFailed.type on docs
    for {
      agent <- ZIO.service[Agent]
      resolver <- ZIO.service[Resolver]
      fromDID <- msg.from match
        case Some(did) => ZIO.succeed(did)
        case None      => ZIO.fail(MissingFromHeader)
      docsFROM <- resolver.didDocument(fromDID)
      keyAgreementAll = docsFROM.keyAgreementAll
      secretsFROM = agent.keyStore.keys.toSeq
        .flatMap { key => key.kid.map(kid => VerificationMethodReferencedWithKey(kid, key)) }
        .filter(vmk => keyAgreementAll.exists(_.kid == vmk.kid))
      senderKeys = secretsFROM
        .groupBy(_.key.crv)
        .view
        .mapValues(_.headOption)
        .toMap
      docsTO <- ZIO.foreach(msg.to.toSeq.flatten)(resolver.didDocument(_))
      recipientKeys = docsTO.flatMap(_.keyAgreementAll).groupBy(_.key.crv)
      curve2SenderRecipientKeys = senderKeys
        .map(e => e._1 -> (e._2, recipientKeys.get(e._1).getOrElse(Seq.empty)))
        .toMap
      data = msg.toJson.getBytes
      msgSeq = curve2SenderRecipientKeys.values.toSeq.map {
        case (None, b)                      => ZIO.none
        case (Some(sender), b) if b.isEmpty => ZIO.none
        case (Some(VerificationMethodReferencedWithKey(senderKid, senderKey)), recipient) =>
          cryptoOperations
            .authEncrypt((VerificationMethodReferenced(senderKid), senderKey), recipient.map(_.pair), data)
            .map(Some(_))
        // case (Some(VerificationMethodReferencedWithKey(senderKid, senderKey: ECPrivateKey)), recipientAux) =>
        //   val recipient = recipientAux.asInstanceOf[Seq[VerificationMethodReferencedWithKey[ECPublicKey]]] // FIXME
        //   RawOperations
        //     .authcryptEC((VerificationMethodReferenced(senderKid), senderKey), recipient, data)
        //     .map(Some(_))
        // case (Some(VerificationMethodReferencedWithKey(senderKid, senderKey: OKPPrivateKey)), recipientAux) =>
        //   val recipient = recipientAux.asInstanceOf[Seq[VerificationMethodReferencedWithKey[OKPPublicKey]]] // FIXME
        //   RawOperations // FIXME cryptoOperations
        //     .authcryptOKP((VerificationMethodReferenced(senderKid), senderKey), recipient, data)
        //     .map(Some(_))
      }: Seq[ZIO[Any, CryptoFailed, Option[EncryptedMessage]]]
      ret <- ZIO.foreach(msgSeq)(e => e).map(_.flatten)
    } yield ret.head // FIXME HAED
  }

  /** decrypt */
  def anonDecryptRaw(msg: EncryptedMessage): ZIO[Agent, DidFail, Array[Byte]] = {
    for {
      agent <- ZIO.service[Agent]
      did = agent.id
      kidsNeeded = msg.recipients.map(_.header.kid)
      keys = agent.keyStore.keys.toSeq
        .flatMap(k =>
          k.kid.flatMap { kid =>
            val vmr = (VerificationMethodReferenced(kid))
            if (kidsNeeded.contains(vmr)) Some(vmr, k) else None
          }
        )
      data <- cryptoOperations.anonDecrypt(keys, msg)
    } yield data
  }

  def authDecryptRaw(msg: EncryptedMessage): ZIO[Agent & Resolver, DidFail, Array[Byte]] =
    for {
      agent <- ZIO.service[Agent]
      did = agent.id
      kidsNeeded = msg.recipients.map(_.header.kid)
      keys = agent.keyStore.keys.toSeq
        .flatMap(k =>
          k.kid.flatMap { kid =>
            val vmr = (VerificationMethodReferenced(kid))
            if (kidsNeeded.contains(vmr)) Some(vmr, k) else None
          }
        )
      resolver <- ZIO.service[Resolver]
      skid = msg.`protected`.obj match
        case AnonProtectedHeader(epk, apv, typ, enc, alg)            => ??? // FIXME
        case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) => skid
      doc <- resolver.didDocument(skid.did.asFROMTO)
      senderKey = doc.keyAgreementAll.find { e => e.vmr == skid }.get // FIXME get
      data <- cryptoOperations.authDecrypt(senderKey.key, keys, msg)
    } yield data

}
