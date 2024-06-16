package fmgp.crypto

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import scala.util.chaining._

object JWTOperationsImp {
  val layer: URLayer[CryptoJWTOperations, JWTOperations] =
    ZLayer.fromFunction(JWTOperationsImp(_))
}

class JWTOperationsImp(ops: CryptoJWTOperations) extends JWTOperations {

  def signJWT(payload: JWTPayload): ZIO[Agent & Resolver, CryptoFailed, JWT] =
    for {
      agent <- ZIO.service[Agent]
      issDID <- payload.getISS match
        case Some(iss) if (iss == agent.id.did) => ZIO.succeed(agent.id)
        case Some(iss)                          => ZIO.fail(WrongSigningDID)
        case None                               => ZIO.fail(MissingFromHeader) // FIXME MissingFromHeader
      resolver <- ZIO.service[Resolver]
      didDocument <- resolver
        .didDocument(issDID.asFROM)
        .mapError(CryptoFailedWarpResolverError(_))
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
      ret <- ops.signJWT(signingKey, payload.toJson.getBytes())
    } yield ret

  def verifyJWT(jwt: JWT): ZIO[Resolver, CryptoFailed, Boolean] =
    ZIO.fail(CryptoNotImplementedError) // FIXME
}
