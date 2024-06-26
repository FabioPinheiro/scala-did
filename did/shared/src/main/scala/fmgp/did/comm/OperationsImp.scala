package fmgp.did.comm

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto.error._
import fmgp.crypto._
import scala.util.chaining.*

/** TODO Fix all FIXME */
object OperationsImp {
  val layer: URLayer[CryptoOperations, Operations] =
    ZLayer.fromFunction(OperationsImp(_))
}

class OperationsImp(cryptoOperations: CryptoOperations) extends Operations {

  def sign(msg: PlaintextMessage): ZIO[Agent & Resolver, CryptoFailed, SignedMessage] =
    for {
      agent <- ZIO.service[Agent]
      fromDID <- msg.from match
        case Some(did) if (did == agent.id.asFROM) => ZIO.succeed(did)
        case Some(did)                             => ZIO.fail(WrongSigningDID)
        case None                                  => ZIO.fail(MissingFromHeader)
      resolver <- ZIO.service[Resolver]
      didDocument <- resolver.didDocument(fromDID).mapError(CryptoFailedWarpResolverError(_))
      privateKeysToSign = {
        val allKeysTypeAuthentication = didDocument.allKeysTypeAuthentication
        agent.keyStore.keys.filter(k => allKeysTypeAuthentication.map(_.id).contains(k.kid))
      }
      signingKey <- privateKeysToSign.toSeq
        .filter {
          case OKPPrivateKey(kty, Curve.X25519, d, x, kid)  => false // UnsupportedCurve
          case OKPPrivateKey(kty, Curve.Ed25519, d, x, kid) => true
          case _                                            => false // TODO UnsupportedCurve ATM
        }
        .headOption // TODO give the user an API to choose the key
        .pipe {
          case Some(key) => ZIO.succeed(key)
          case None      => ZIO.fail(NoSupportedKey)
        }
      ret <- cryptoOperations.sign(signingKey, msg)
    } yield ret

  def verify(msg: SignedMessage): ZIO[Resolver, CryptoFailed, Boolean] = {
    for {
      resolver <- ZIO.service[Resolver]
      mSignerKids = msg.signatures.map(_.signerKid).map(e => DIDURL.parseString(e))
      ret <- ZIO.forall(mSignerKids) { mSignerKid =>
        for {
          didURL <- mSignerKid match {
            case Left(fail)    => ZIO.fail(FailToExtractKid(fail))
            case Right(didURL) => ZIO.succeed(didURL)
          }
          kid = didURL.toFROMTO
          doc <- resolver
            .didDocument(kid)
            .mapError(CryptoFailedWarpResolverError(_))
          key <- doc.authenticationByKid(didURL).map(_.key) match
            case Some(key) => ZIO.succeed(key)
            case None      => ZIO.fail(FailToExtractKid("Fail to extract kid: " + didURL.string))
          result <- cryptoOperations.verify(key, msg)
        } yield result
      }
    } yield ret
  }

  override def anonEncrypt(msg: PlaintextMessage): ZIO[Resolver, DidFail, EncryptedMessage] = {
    // TODO return EncryptionFailed.type on docs
    for {
      resolver <- ZIO.service[Resolver]
      docs <- ZIO.foreach(msg.to.toSeq.flatten)(
        resolver.didDocument(_).mapError(ResolverErrorWarp(_))
      )
      recipientKidsKeys = docs.flatMap(_.allKeysTypeKeyAgreement).map(_.pair)
      ret <- cryptoOperations.anonEncrypt(recipientKidsKeys, msg.toJson.getBytes)
    } yield ret
  }

  override def authEncrypt(msg: PlaintextMessage): ZIO[Agent & Resolver, DidFail, EncryptedMessage] = {
    // TODO return EncryptionFailed.type on docs
    for {
      agent <- ZIO.service[Agent]
      resolver <- ZIO.service[Resolver]
      fromDID <- msg.from match
        case Some(did) => ZIO.succeed(did)
        case None      => ZIO.fail(MissingFromHeader)
      docsFROM <- resolver
        .didDocument(fromDID)
        .mapError(ResolverErrorWarp(_))
      allKeysTypeKeyAgreement = docsFROM.allKeysTypeKeyAgreement
      secretsFROM = agent.keyStore.keys.toSeq
        .map { key => VerificationMethodReferencedWithKey(key.kid, key) }
        .filter(vmk => allKeysTypeKeyAgreement.exists(_.kid == vmk.kid))
      senderKeys = secretsFROM
        .groupBy(_.key.crv)
        .view
        .mapValues(_.headOption)
        .toMap
      docsTO <- ZIO.foreach(msg.to.toSeq.flatten)(
        resolver.didDocument(_).mapError(ResolverErrorWarp(_))
      )
      recipientKeys = docsTO.flatMap(_.allKeysTypeKeyAgreement).groupBy(_.key.crv)
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
      headFixme <- ret match
        case Seq()         => ZIO.fail(EncryptionFailed) // FIXME HAED
        case eMsg +: Seq() => ZIO.succeed(eMsg)
        case eMsg +: others => // TODO Don't discard other messages
          ZIO.logWarning(s"We are discount other encrypted messages (${others.size})") *>
            ZIO.succeed(eMsg)
    } yield headFixme
  }

  /** decrypt */
  def anonDecryptRaw(msg: EncryptedMessage): ZIO[Agent, DidFail, Array[Byte]] = {
    for {
      agent <- ZIO.service[Agent]
      did = agent.id
      kidsNeeded = msg.recipients.map(_.header.kid)
      // TODO fail if the keys are not keyAgreement
      keys = agent.keyStore.keys.toSeq
        .flatMap { k =>
          val vmr = (VerificationMethodReferenced(k.kid))
          if (kidsNeeded.contains(vmr)) Some(vmr, k) else None
        }
      data <- cryptoOperations.anonDecrypt(keys, msg)
    } yield data
  }

  def authDecryptRaw(msg: EncryptedMessage): ZIO[Agent & Resolver, DidFail, Array[Byte]] =
    for {
      agent <- ZIO.service[Agent]
      did = agent.id
      kidsNeeded = msg.recipients.map(_.header.kid)
      // TODO fail if the keys are not keyAgreement
      keys = agent.keyStore.keys.toSeq
        .flatMap { k =>
          val vmr = (VerificationMethodReferenced(k.kid))
          if (kidsNeeded.contains(vmr)) Some(vmr, k) else None
        }
      resolver <- ZIO.service[Resolver]
      skid = msg.`protected`.obj match
        case AnonProtectedHeader(epk, apv, typ, enc, alg)            => ??? // FIXME
        case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) => skid
      doc <- resolver
        .didDocument(skid.did.asFROMTO)
        .mapError(ResolverErrorWarp(_))
      senderKey = doc.allKeysTypeKeyAgreement.find { e => e.vmr == skid }.get // FIXME get
      data <- cryptoOperations.authDecrypt(senderKey.key, keys, msg)
    } yield data

}
