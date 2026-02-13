package fmgp.crypto

import zio.json.*
import zio.json.ast.Json
import zio.json.ast.JsonCursor
import fmgp.util.{Base64, safeValueOf}

/** Header Parameter Values for JWS https://datatracker.ietf.org/doc/html/rfc7518#section-3.1 */
enum JWAAlgorithm {

  /** ECDSA using secp256k1 and SHA-256 */
  case ES256K extends JWAAlgorithm

  /** ECDSA using P-256 (secp256r1) and SHA-256 */
  case ES256 extends JWAAlgorithm

  /** ECDSA using P-384 (secp384r1) and SHA-384 */
  case ES384 extends JWAAlgorithm // TODO check https://identity.foundation/didcomm-messaging/spec/#algorithms

  /** ECDSA using P-512 (secp512r1) and SHA-512 */
  case ES512 extends JWAAlgorithm // TODO check https://identity.foundation/didcomm-messaging/spec/#algorithms

  /** EdDSA (with crv=Ed25519)
    * @see
    *   https://identity.foundation/didcomm-messaging/spec/#algorithms
    */
  case EdDSA extends JWAAlgorithm
}
object JWAAlgorithm {
  given decoder: JsonDecoder[JWAAlgorithm] = JsonDecoder.string.mapOrFail(e => safeValueOf(JWAAlgorithm.valueOf(e)))
  given encoder: JsonEncoder[JWAAlgorithm] = JsonEncoder.string.contramap((e: JWAAlgorithm) => e.toString)
}

enum KTY:
  // case RSA extends KTY
  case EC extends KTY // Elliptic Curve
  case OKP extends KTY // Edwards-curve Octet Key Pair

object KTY {
  given decoder: JsonDecoder[KTY] = JsonDecoder.string.mapOrFail(e => safeValueOf(KTY.valueOf(e)))
  given encoder: JsonEncoder[KTY] = JsonEncoder.string.contramap((e: KTY) => e.toString)

  given decoderEC: JsonDecoder[KTY.EC.type] = JsonDecoder.string.mapOrFail(str =>
    if (str == KTY.EC.toString) Right(KTY.EC) else Left(s"'$str' is not a type KTY.EC")
  )
  given encoderEC: JsonEncoder[KTY.EC.type] = JsonEncoder.string.contramap((e: KTY.EC.type) => e.toString)

  given decoderOKP: JsonDecoder[KTY.OKP.type] = JsonDecoder.string.mapOrFail(str =>
    if (str == KTY.OKP.toString) Right(KTY.OKP) else Left(s"'$str' is not a type KTY.OKP")
  )
  given encoderOKP: JsonEncoder[KTY.OKP.type] = JsonEncoder.string.contramap((e: KTY.OKP.type) => e.toString)

}

enum Curve: // TODO make it type safe!
  def name = this.toString
  case `P-256` extends Curve
  case `P-384` extends Curve
  case `P-521` extends Curve
  case secp256k1 extends Curve
  // case Curve25519 extends Curve
  /** Elliptic-Curve Diffie-Hellman (ECDH) protocol using the x coordinate of the curve Curve25519. */
  case X25519 extends Curve //  used for key exchange
  /** Edwards Digital Signature Algorithm using a curve which is birationally equivalent to Curve25519. */
  case Ed25519 extends Curve //  used for digital signatures

// sealed trait ECCurve // Elliptic Curve
type ECCurve = Curve.`P-256`.type | Curve.`P-384`.type | Curve.`P-521`.type | Curve.secp256k1.type
// sealed trait OKPCurve // Edwards-curve Octet Key Pair
type OKPCurve = Curve.X25519.type | Curve.Ed25519.type // | Curve.Curve25519.type
object ECCurve {
  def valueOf(c: String): Either[String, ECCurve] = Curve.valueOf(c) match
    case o: ECCurve  => Right(o)
    case o: OKPCurve => Left(s"$o is a OKPCruve but is expecting a ECCruve")
}
object OKPCurve {
  def valueOf(c: String): Either[String, OKPCurve] = Curve.valueOf(c) match
    case o: OKPCurve => Right(o)
    case o: ECCurve  => Left(s"$o is a ECCruve but is expecting a OKPCruve")
}

opaque type ECCurveOpaque = Curve.`P-256`.type | Curve.`P-384`.type | Curve.`P-521`.type | Curve.secp256k1.type
object ECCurveOpaque:
  def apply(value: ECCurve): ECCurveOpaque = value
  extension (curveEnum: ECCurveOpaque) def curve: ECCurve = curveEnum
  given decoder: JsonDecoder[ECCurveOpaque] = JsonDecoder.string.mapOrFail(ECCurve.valueOf)

opaque type OKPCurveOpaque = OKPCurve
object OKPCurveOpaque:
  def apply(value: OKPCurve): OKPCurveOpaque = value
  extension (curveEnum: OKPCurveOpaque) def curve: OKPCurve = curveEnum
  given decoder: JsonDecoder[OKPCurveOpaque] = JsonDecoder.string.mapOrFail(OKPCurve.valueOf)

object Curve {
  extension (curve: Curve) {

    /** asECCurve is a Unsafe methods!
      *
      * @throws ClassCastException
      *   is the curve is not a EC Curve
      */
    def asECCurve: ECCurve = curve match {
      case c: ECCurve  => c
      case c: OKPCurve => throw ClassCastException(s"Type $c is not a EC Curve")
    }

    /** asOKPCurve is a Unsafe methods!
      *
      * @throws ClassCastException
      *   is the curve is not a OKP Curve
      */
    def asOKPCurve: OKPCurve = curve match {
      case c: ECCurve  => throw ClassCastException(s"Type $c is not a OKP Curve")
      case c: OKPCurve => c
    }
  }

  extension (curve: ECCurve) {
    inline def isPointOnCurve(x: BigInt, y: BigInt) = curve match
      case Curve.`P-256`   => PointOnCurve.isPointOnCurveP_256(x, y)
      case Curve.`P-384`   => PointOnCurve.isPointOnCurveP_384(x, y)
      case Curve.`P-521`   => PointOnCurve.isPointOnCurveP_521(x, y)
      case Curve.secp256k1 => PointOnCurve.isPointOnCurveSecp256k1(x, y)
  }

  given decoder: JsonDecoder[Curve] = JsonDecoder.string.mapOrFail(e => safeValueOf(Curve.valueOf(e)))
  given encoder: JsonEncoder[Curve] = JsonEncoder.string.contramap((e: Curve) => e.toString)

  val ecCurveSet = Set(Curve.`P-256`, Curve.`P-384`, Curve.`P-521`, Curve.secp256k1)
  val okpCurveSet = Set(Curve.X25519, Curve.Ed25519)
}

/** https://www.rfc-editor.org/rfc/rfc7638 */
sealed trait JWKObj extends MaybeKid {
  def kty: KTY
  def alg: JWAAlgorithm
}
sealed trait MaybeKid {
  def withKid(kid: String): WithKid
  def withoutKid: WithoutKid
  def maybeKid: Option[String] = this match
    case o: WithKid => Some(o.kid)
    case o          => None
}
sealed trait WithKid extends MaybeKid {
  def kid: String
  override final def maybeKid: Some[String] = Some(kid)
}
sealed trait WithoutKid extends MaybeKid { override final def maybeKid: Option[String] = None }

// @jsonDiscriminator("kty")
sealed trait OKP_EC_Key extends JWKObj {
  def kty: KTY // EC.type = KTY.EC
  def crv: Curve
  // def kid: Option[String]

  /** x is a Base64url (RFC 4648) without padding. */
  def x: String
  def xBase64Url = Base64.fromBase64url(x)
  def xNumbre = xBase64Url.decodeToBigInt

  /** https://identity.foundation/didcomm-messaging/spec/#algorithms */
  def jwaAlgorithmtoSign: JWAAlgorithm = crv match {
    case Curve.secp256k1 => JWAAlgorithm.ES256K
    case Curve.`P-256`   => JWAAlgorithm.ES256
    case Curve.`P-384`   => JWAAlgorithm.ES256 // (deprecated?) // TODO CHECK ES256
    case Curve.`P-521`   => JWAAlgorithm.ES256 // (deprecated?) // TODO CHECK ES256
    case Curve.X25519    => JWAAlgorithm.EdDSA // FIXME MUST NOT be used for signing
    case Curve.Ed25519   => JWAAlgorithm.EdDSA
  }

  def alg = crv match {
    case Curve.secp256k1 => JWAAlgorithm.ES256K
    case Curve.`P-256`   => JWAAlgorithm.ES256
    case Curve.`P-384`   => JWAAlgorithm.ES384
    case Curve.`P-521`   => JWAAlgorithm.ES512
    case Curve.X25519    => JWAAlgorithm.EdDSA
    case Curve.Ed25519   => JWAAlgorithm.EdDSA
  }

}

sealed abstract class OKPKey extends OKP_EC_Key {
  override def kty: KTY.OKP.type
  override def crv: OKPCurve

  def getCurve: OKPCurve = crv.asOKPCurve
}
sealed trait OKPKeyWithKid extends OKPKey with WithKid
sealed trait OKPKeyWithoutKid extends OKPKey with WithoutKid
sealed abstract class ECKey extends OKP_EC_Key {
  override def kty: KTY.EC.type
  override def crv: ECCurve

  /** y is a Base64url (RFC 4648) without padding. */
  def y: String
  def yBase64Url = Base64.fromBase64url(y)
  def yNumbre = yBase64Url.decodeToBigInt
  def getCurve: ECCurve = crv.asECCurve
  def isPointOnCurve = getCurve.isPointOnCurve(xNumbre, yNumbre)
}
sealed trait ECKeyWithKid extends ECKey with WithKid
sealed trait ECKeyWithoutKid extends ECKey with WithoutKid

sealed trait PublicKey extends OKP_EC_Key {
  override def withKid(kid: String): PublicKeyWithKid
  override def withoutKid: PublicKeyWithoutKid
}
sealed trait PublicKeyWithKid extends PublicKey with WithKid
sealed trait PublicKeyWithoutKid extends OKP_EC_Key with WithoutKid {
  override def withKid(kid: String): PublicKeyWithKid
  override def withoutKid: PublicKeyWithoutKid
}
sealed trait PrivateKey extends OKP_EC_Key {
  def d: String
  def toPublicKey: PublicKey
  override def withKid(kid: String): PrivateKeyWithKid
  override def withoutKid: PrivateKeyWithoutKid
}
sealed trait PrivateKeyWithKid extends PrivateKey with WithKid
sealed trait PrivateKeyWithoutKid extends PrivateKey with WithoutKid

// ##########
// ## OKP ###
// ##########

sealed trait OKPPublicKey extends OKPKey with PublicKey {
  final override def withKid(kid: String): OKPPublicKeyWithKid = this match
    case k: OKPPublicKeyWithKid    => k.copy(kid = kid)
    case k: OKPPublicKeyWithoutKid => OKPPublicKeyWithKid(kty = k.kty, crv = k.crv, x = k.x, kid)
  final override def withoutKid: OKPPublicKeyWithoutKid = this match
    case k: OKPPublicKeyWithKid    => OKPPublicKeyWithoutKid(kty = k.kty, crv = k.crv, x = k.x)
    case k: OKPPublicKeyWithoutKid => k
}
sealed trait OKPPrivateKey extends OKPKey with PrivateKey {
  override def toPublicKey: OKPPublicKey
  final override def withKid(kid: String): OKPPrivateKeyWithKid = this match
    case k: OKPPrivateKeyWithKid    => k.copy(kid = kid)
    case k: OKPPrivateKeyWithoutKid => OKPPrivateKeyWithKid(kty = k.kty, crv = k.crv, d = k.d, x = k.x, kid)
  final override def withoutKid: OKPPrivateKeyWithoutKid = this match
    case k: OKPPrivateKeyWithKid    => OKPPrivateKeyWithoutKid(kty = k.kty, crv = k.crv, d = k.d, x = k.x)
    case k: OKPPrivateKeyWithoutKid => k
}

case class OKPPublicKeyWithKid(kty: KTY.OKP.type, crv: OKPCurve, x: String, kid: String)
    extends OKPPublicKey
    with OKPKeyWithKid
    with PublicKeyWithKid
case class OKPPublicKeyWithoutKid(kty: KTY.OKP.type, crv: OKPCurve, x: String)
    extends OKPPublicKey
    with OKPKeyWithoutKid
    with PublicKeyWithoutKid {}
case class OKPPrivateKeyWithKid(kty: KTY.OKP.type, crv: OKPCurve, d: String, x: String, kid: String)
    extends OKPPrivateKey
    with OKPKeyWithKid
    with PrivateKeyWithKid {
  override def toPublicKey: OKPPublicKeyWithKid = OKPPublicKeyWithKid(kty = kty, crv = crv, x = x, kid = kid)
}
case class OKPPrivateKeyWithoutKid(kty: KTY.OKP.type, crv: OKPCurve, d: String, x: String)
    extends OKPPrivateKey
    with OKPKeyWithoutKid
    with PrivateKeyWithoutKid {
  override def toPublicKey: OKPPublicKeyWithoutKid = OKPPublicKeyWithoutKid(kty = kty, crv = crv, x = x)
}

// ##########
// ### EC ###
// ##########

sealed trait ECPublicKey extends ECKey with PublicKey {
  final override def withKid(kid: String): ECPublicKeyWithKid = this match
    case k: ECPublicKeyWithKid    => k.copy(kid = kid)
    case k: ECPublicKeyWithoutKid => ECPublicKeyWithKid(kty = k.kty, crv = k.crv, x = k.x, y = k.y, kid)
  final override def withoutKid: ECPublicKeyWithoutKid = this match
    case k: ECPublicKeyWithKid    => ECPublicKeyWithoutKid(kty = k.kty, crv = k.crv, x = k.x, y = k.y)
    case k: ECPublicKeyWithoutKid => k

}
sealed trait ECPrivateKey extends ECKey with PrivateKey {
  override def toPublicKey: ECPublicKey
  final override def withKid(kid: String): ECPrivateKeyWithKid = this match
    case k: ECPrivateKeyWithKid    => k.copy(kid = kid)
    case k: ECPrivateKeyWithoutKid => ECPrivateKeyWithKid(kty = k.kty, crv = k.crv, d = k.d, x = k.x, y = k.y, kid)
  final override def withoutKid: ECPrivateKeyWithoutKid = this match
    case k: ECPrivateKeyWithKid    => ECPrivateKeyWithoutKid(kty = k.kty, crv = k.crv, d = k.d, x = k.x, y = k.y)
    case k: ECPrivateKeyWithoutKid => k
}

case class ECPublicKeyWithKid(kty: KTY.EC.type, crv: ECCurve, x: String, y: String, kid: String)
    extends ECPublicKey
    with ECKeyWithKid
    with PublicKeyWithKid

case class ECPublicKeyWithoutKid(kty: KTY.EC.type, crv: ECCurve, x: String, y: String)
    extends ECPublicKey
    with ECKeyWithoutKid
    with PublicKeyWithoutKid

case class ECPrivateKeyWithKid(kty: KTY.EC.type, crv: ECCurve, d: String, x: String, y: String, kid: String)
    extends ECPrivateKey
    with ECKeyWithKid
    with PrivateKeyWithKid {
  override def toPublicKey: ECPublicKeyWithKid = ECPublicKeyWithKid(kty = kty, crv = crv, x = x, y = y, kid = kid)
}

case class ECPrivateKeyWithoutKid(kty: KTY.EC.type, crv: ECCurve, d: String, x: String, y: String)
    extends ECPrivateKey
    with ECKeyWithoutKid
    with PrivateKeyWithoutKid {
  override def toPublicKey: ECPublicKeyWithoutKid = ECPublicKeyWithoutKid(kty = kty, crv = crv, x = x, y = y)
}

// ###############
// ###############
// ###############

object OKP_EC_Key {
  private val ktyCursor = JsonCursor.field("kty") >>> JsonCursor.isString
  private val crvCursor = JsonCursor.field("crv") >>> JsonCursor.isString
  // private val dCursor = JsonCursor.field("d") >>> JsonCursor.isString
  private val xCursor = JsonCursor.field("x") >>> JsonCursor.isString
  private val yCursor = JsonCursor.field("y") >>> JsonCursor.isString
  // private val kidCursor = JsonCursor.field("kid") >>> JsonCursor.isString

  given encoder: JsonEncoder[OKP_EC_Key] =
    JsonEncoder[Json.Obj].contramap { k =>
      Json.Obj.apply(
        Seq(
          Some(("kty", Json.Str(k.kty.toString))),
          Some(("crv", Json.Str(k.crv.toString()))),
          k match { case ecKey: PrivateKey => Some(("d", Json.Str(ecKey.d))); case _ => None },
          Some(("x", Json.Str(k.x))),
          k match { case ecKey: ECKey => Some(("y", Json.Str(ecKey.y))); case _ => None },
          k.maybeKid.map(kid => ("kid", Json.Str(kid)))
        ).flatten*
      )
    }
  given decoder: JsonDecoder[OKP_EC_Key] = JsonDecoder[Json.Obj]
    .mapOrFail { obj =>
      for {
        kty <- obj.get(ktyCursor).flatMap(_.as[KTY])
        maybeD <- obj.get("d") match
          case None       => Right(None)
          case Some(data) => data.as[Json.Str].map(s => Some(s.value))
        maybeKid <- obj.get("kid") match
          case None       => Right(None)
          case Some(data) => data.as[Json.Str].map(s => Some(s.value))
        x <- obj.get(xCursor).map(_.value)
        key <- kty match
          case ktyEC: KTY.EC.type =>
            for {
              y <- obj.get(yCursor).map(_.value)
              crv <- obj.get(crvCursor).flatMap(_.as[ECCurveOpaque])
              ret = (maybeD, maybeKid) match
                case (None, None)         => ECPublicKeyWithoutKid(kty = ktyEC, crv = crv.curve, x = x, y = y)
                case (None, Some(kid))    => ECPublicKeyWithKid(kty = ktyEC, crv = crv.curve, x = x, y = y, kid = kid)
                case (Some(d), None)      => ECPrivateKeyWithoutKid(kty = ktyEC, crv = crv.curve, d = d, x = x, y = y)
                case (Some(d), Some(kid)) =>
                  ECPrivateKeyWithKid(kty = ktyEC, crv = crv.curve, d = d, x = x, y = y, kid = kid)
            } yield ret
          case ktyOKP: KTY.OKP.type =>
            for {
              crv <- obj.get(crvCursor).flatMap(_.as[OKPCurveOpaque])
              ret = (maybeD, maybeKid) match
                case (None, None)         => OKPPublicKeyWithoutKid(kty = ktyOKP, crv = crv.curve, x = x)
                case (None, Some(kid))    => OKPPublicKeyWithKid(kty = ktyOKP, crv = crv.curve, x = x, kid = kid)
                case (Some(d), None)      => OKPPrivateKeyWithoutKid(kty = ktyOKP, crv = crv.curve, d = d, x = x)
                case (Some(d), Some(kid)) =>
                  OKPPrivateKeyWithKid(kty = ktyOKP, crv = crv.curve, d = d, x = x, kid = kid)
            } yield ret
      } yield key
    }
}

object PublicKey {
  def apply(kty: KTY.EC.type, crv: ECCurve, x: String, y: String, kid: String): ECPublicKeyWithKid =
    ECPublicKeyWithKid(kty = kty, crv = crv, x = x, y = y, kid = kid)
  def apply(kty: KTY.EC.type, crv: ECCurve, x: String, y: String): ECPublicKeyWithoutKid =
    new ECPublicKeyWithoutKid(kty = kty, crv = crv, x = x, y = y)
  def apply(kty: KTY.OKP.type, crv: OKPCurve, x: String, kid: String): OKPPublicKeyWithKid =
    OKPPublicKeyWithKid(kty = kty, crv = crv, x = x, kid = kid)
  def apply(kty: KTY.OKP.type, crv: OKPCurve, x: String): OKPPublicKeyWithoutKid =
    OKPPublicKeyWithoutKid(kty = kty, crv = crv, x = x)
  def unapply(key: PublicKey): Option[(KTY, Curve, String, Option[String], Option[String])] = key match
    case key: ECPublicKey  => Some((key.kty, key.crv, key.x, Some(key.y), key.maybeKid))
    case key: OKPPublicKey => Some((key.kty, key.crv, key.x, None, key.maybeKid))

  given encoder: JsonEncoder[PublicKey] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[PublicKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Right(k)
      case k: OKPPublicKeyWithoutKid  => Right(k)
      case k: OKPPrivateKeyWithKid    => Right(k.toPublicKey)
      case k: OKPPrivateKeyWithoutKid => Right(k.toPublicKey)
      case k: ECPublicKeyWithKid      => Right(k)
      case k: ECPublicKeyWithoutKid   => Right(k)
      case k: ECPrivateKeyWithKid     => Right(k.toPublicKey)
      case k: ECPrivateKeyWithoutKid  => Right(k.toPublicKey)
    }
}

object PublicKeyWithKid {
  given encoder: JsonEncoder[PublicKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[PublicKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Right(k)
      case k: OKPPublicKeyWithoutKid  => Left("Expected PublicKeyWithKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Right(k.toPublicKey)
      case k: OKPPrivateKeyWithoutKid => Left("Expected PublicKeyWithKid but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Right(k)
      case k: ECPublicKeyWithoutKid   => Left("Expected PublicKeyWithKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Right(k.toPublicKey)
      case k: ECPrivateKeyWithoutKid  => Left("Expected PublicKeyWithKid but got ECPrivateKeyWithoutKid")
    }
}

object PublicKeyWithoutKid {
  given encoder: JsonEncoder[PublicKeyWithoutKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[PublicKeyWithoutKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected PublicKeyWithoutKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Right(k)
      case k: OKPPrivateKeyWithKid    => Left("Expected PublicKeyWithoutKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Right(k.toPublicKey)
      case k: ECPublicKeyWithKid      => Left("Expected PublicKeyWithoutKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Right(k)
      case k: ECPrivateKeyWithKid     => Left("Expected PublicKeyWithoutKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Right(k.toPublicKey)
    }
}

object PrivateKey {
  def apply(kty: KTY.EC.type, crv: ECCurve, d: String, x: String, y: String, kid: String): ECPrivateKeyWithKid =
    ECPrivateKeyWithKid(kty = kty, crv = crv, d = d, x = x, y = y, kid = kid)
  def apply(kty: KTY.EC.type, crv: ECCurve, d: String, x: String, y: String): ECPrivateKeyWithoutKid =
    ECPrivateKeyWithoutKid(kty = kty, crv = crv, d = d, x = x, y = y)
  def apply(kty: KTY.OKP.type, crv: OKPCurve, d: String, x: String, kid: String): OKPPrivateKeyWithKid =
    OKPPrivateKeyWithKid(kty = kty, crv = crv, d = d, x = x, kid = kid)
  def apply(kty: KTY.OKP.type, crv: OKPCurve, d: String, x: String): OKPPrivateKeyWithoutKid =
    OKPPrivateKeyWithoutKid(kty = kty, crv = crv, d = d, x = x)
  def unapply(key: PrivateKey): Option[(KTY, Curve, String, String, Option[String], Option[String])] = key match
    case key: ECPrivateKey  => Some((key.kty, key.crv, key.d, key.x, Some(key.y), key.maybeKid))
    case key: OKPPrivateKey => Some((key.kty, key.crv, key.d, key.x, None, key.maybeKid))

  given encoder: JsonEncoder[PrivateKey] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[PrivateKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected PrivateKey but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected PrivateKey but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Right(k)
      case k: OKPPrivateKeyWithoutKid => Right(k)
      case k: ECPublicKeyWithKid      => Left("Expected PrivateKey but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected PrivateKey but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Right(k)
      case k: ECPrivateKeyWithoutKid  => Right(k)
    }
  // given Conversion[PrivateKey, PublicKey] with
  //   def apply(key: PrivateKey) = key.toPublicKey
}

object PrivateKeyWithKid {
  given encoder: JsonEncoder[PrivateKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[PrivateKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected PrivateKeyWithKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected PrivateKeyWithKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Right(k)
      case k: OKPPrivateKeyWithoutKid => Left("Expected PrivateKeyWithKid but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Left("Expected PrivateKeyWithKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected PrivateKeyWithKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Right(k)
      case k: ECPrivateKeyWithoutKid  => Left("Expected PrivateKeyWithKid but got ECPrivateKeyWithoutKid")
    }
}

object PrivateKeyWithoutKid {
  given encoder: JsonEncoder[PrivateKeyWithoutKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[PrivateKeyWithoutKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected PrivateKeyWithoutKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected PrivateKeyWithoutKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Left("Expected PrivateKeyWithoutKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Right(k)
      case k: ECPublicKeyWithKid      => Left("Expected PrivateKeyWithoutKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected PrivateKeyWithoutKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Left("Expected PrivateKeyWithoutKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Right(k)
    }
}

object ECPublicKey {
  def apply(kty: KTY.EC.type, crv: ECCurve, x: String, y: String, kid: String): ECPublicKeyWithKid =
    ECPublicKeyWithKid(kty = kty, crv = crv, x = x, y = y, kid = kid)
  def apply(kty: KTY.EC.type, crv: ECCurve, x: String, y: String): ECPublicKeyWithoutKid =
    new ECPublicKeyWithoutKid(kty = kty, crv = crv, x = x, y = y)
  def unapply(key: ECPublicKey): Option[(KTY, ECCurve, String, String, Option[String])] =
    Some((key.kty, key.crv, key.x, key.y, key.maybeKid))

  given encoder: JsonEncoder[ECPublicKey] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[ECPublicKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected ECPublicKey but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected ECPublicKey but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Left("Expected ECPublicKey but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Left("Expected ECPublicKey but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Right(k)
      case k: ECPublicKeyWithoutKid   => Right(k)
      case k: ECPrivateKeyWithKid     => Right(k.toPublicKey)
      case k: ECPrivateKeyWithoutKid  => Right(k.toPublicKey)
    }
}

object ECPublicKeyWithKid {
  given encoder: JsonEncoder[ECPublicKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[ECPublicKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected ECPublicKeyWithKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected ECPublicKeyWithKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Left("Expected ECPublicKeyWithKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Left("Expected ECPublicKeyWithKid but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Right(k)
      case k: ECPublicKeyWithoutKid   => Left("Expected ECPublicKeyWithKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Right(k.toPublicKey)
      case k: ECPrivateKeyWithoutKid  => Left("Expected ECPublicKeyWithKid but got ECPrivateKeyWithoutKid")
    }
}

object ECPublicKeyWithoutKid {
  given encoder: JsonEncoder[ECPublicKeyWithoutKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[ECPublicKeyWithoutKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected ECPublicKeyWithoutKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected ECPublicKeyWithoutKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Left("Expected ECPublicKeyWithoutKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Left("Expected ECPublicKeyWithoutKid but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Left("Expected ECPublicKeyWithoutKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Right(k)
      case k: ECPrivateKeyWithKid     => Left("Expected ECPublicKeyWithoutKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Right(k.toPublicKey)
    }
}

object ECPrivateKey {
  def apply(kty: KTY.EC.type, crv: ECCurve, d: String, x: String, y: String, kid: String): ECPrivateKeyWithKid =
    ECPrivateKeyWithKid(kty = kty, crv = crv, d = d, x = x, y = y, kid = kid)
  def apply(kty: KTY.EC.type, crv: ECCurve, d: String, x: String, y: String): ECPrivateKeyWithoutKid =
    ECPrivateKeyWithoutKid(kty = kty, crv = crv, d = d, x = x, y = y)
  def unapply(key: ECPrivateKey): Option[(KTY, ECCurve, String, String, String, Option[String])] =
    Some((key.kty, key.crv, key.d, key.x, key.y, key.maybeKid))

  given encoder: JsonEncoder[ECPrivateKey] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[ECPrivateKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected ECPrivateKey but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected ECPrivateKey but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Left("Expected ECPrivateKey but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Left("Expected ECPrivateKey but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Left("Expected ECPrivateKey but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected ECPrivateKey but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Right(k)
      case k: ECPrivateKeyWithoutKid  => Right(k)
    }
}

object ECPrivateKeyWithKid {
  given encoder: JsonEncoder[ECPrivateKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[ECPrivateKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected ECPrivateKeyWithKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected ECPrivateKeyWithKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Left("Expected ECPrivateKeyWithKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Left("Expected ECPrivateKeyWithKid but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Left("Expected ECPrivateKeyWithKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected ECPrivateKeyWithKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Right(k)
      case k: ECPrivateKeyWithoutKid  => Left("Expected ECPrivateKeyWithKid but got ECPrivateKeyWithoutKid")
    }
}

object ECPrivateKeyWithoutKid {
  given encoder: JsonEncoder[ECPrivateKeyWithoutKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[ECPrivateKeyWithoutKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected ECPrivateKeyWithoutKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected ECPrivateKeyWithoutKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Left("Expected ECPrivateKeyWithoutKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Left("Expected ECPrivateKeyWithoutKid but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Left("Expected ECPrivateKeyWithoutKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected ECPrivateKeyWithoutKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Left("Expected ECPrivateKeyWithoutKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Right(k)
    }
}

object OKPPublicKey {
  def apply(kty: KTY.OKP.type, crv: OKPCurve, x: String, kid: String): OKPPublicKeyWithKid =
    OKPPublicKeyWithKid(kty = kty, crv = crv, x = x, kid = kid)
  def apply(kty: KTY.OKP.type, crv: OKPCurve, x: String): OKPPublicKeyWithoutKid =
    OKPPublicKeyWithoutKid(kty = kty, crv = crv, x = x)
  def makeEd25519(x: Base64) = OKPPublicKey(kty = KTY.OKP, crv = Curve.Ed25519, x = x.urlBase64WithoutPadding)
  def unapply(key: OKPPublicKey): Option[(KTY, OKPCurve, String, Option[String])] =
    Some((key.kty, key.crv, key.x, key.maybeKid))

  given encoder: JsonEncoder[OKPPublicKey] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[OKPPublicKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Right(k)
      case k: OKPPublicKeyWithoutKid  => Right(k)
      case k: OKPPrivateKeyWithKid    => Right(k.toPublicKey)
      case k: OKPPrivateKeyWithoutKid => Right(k.toPublicKey)
      case k: ECPublicKeyWithKid      => Left("Expected OKPPublicKey but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected OKPPublicKey but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Left("Expected OKPPublicKey but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Left("Expected OKPPublicKey but got ECPrivateKeyWithoutKid")
    }
}

object OKPPublicKeyWithKid {
  given encoder: JsonEncoder[OKPPublicKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[OKPPublicKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Right(k)
      case k: OKPPublicKeyWithoutKid  => Left("Expected OKPPublicKeyWithKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Right(k.toPublicKey)
      case k: OKPPrivateKeyWithoutKid => Left("Expected OKPPublicKeyWithKid but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Left("Expected OKPPublicKeyWithKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected OKPPublicKeyWithKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Left("Expected OKPPublicKeyWithKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Left("Expected OKPPublicKeyWithKid but got ECPrivateKeyWithoutKid")
    }
}

object OKPPublicKeyWithoutKid {
  given encoder: JsonEncoder[OKPPublicKeyWithoutKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[OKPPublicKeyWithoutKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected OKPPublicKeyWithoutKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Right(k)
      case k: OKPPrivateKeyWithKid    => Left("Expected OKPPublicKeyWithoutKid but got OKPPrivateKeyWitKid")
      case k: OKPPrivateKeyWithoutKid => Right(k.toPublicKey)
      case k: ECPublicKeyWithKid      => Left("Expected OKPPublicKeyWithoutKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected OKPPublicKeyWithoutKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Left("Expected OKPPublicKeyWithoutKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Left("Expected OKPPublicKeyWithoutKid but got ECPrivateKeyWithoutKid")
    }
}

object OKPPrivateKey {
  def apply(kty: KTY.OKP.type, crv: OKPCurve, d: String, x: String, kid: String): OKPPrivateKeyWithKid =
    OKPPrivateKeyWithKid(kty = kty, crv = crv, d = d, x = x, kid = kid)
  def apply(kty: KTY.OKP.type, crv: OKPCurve, d: String, x: String): OKPPrivateKeyWithoutKid =
    OKPPrivateKeyWithoutKid(kty = kty, crv = crv, d = d, x = x)
  def unapply(key: OKPPrivateKey): Option[(KTY, OKPCurve, String, String, Option[String])] =
    Some((key.kty, key.crv, key.d, key.x, key.maybeKid))

  given encoder: JsonEncoder[OKPPrivateKey] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[OKPPrivateKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected OKPPrivateKey but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected OKPPrivateKey but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Right(k)
      case k: OKPPrivateKeyWithoutKid => Right(k)
      case k: ECPublicKeyWithKid      => Left("Expected OKPPrivateKey but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected OKPPrivateKey but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Left("Expected OKPPrivateKey but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Left("Expected OKPPrivateKey but got ECPrivateKeyWithoutKid")
    }
}

object OKPPrivateKeyWithKid {
  given encoder: JsonEncoder[OKPPrivateKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[OKPPrivateKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected OKPPrivateKeyWithKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected OKPPrivateKeyWithKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Right(k)
      case k: OKPPrivateKeyWithoutKid => Left("Expected OKPPrivateKeyWithKid but got OKPPrivateKeyWithoutKid")
      case k: ECPublicKeyWithKid      => Left("Expected OKPPrivateKeyWithKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected OKPPrivateKeyWithKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Left("Expected OKPPrivateKeyWithKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Left("Expected OKPPrivateKeyWithKid but got ECPrivateKeyWithoutKid")
    }
}
object OKPPrivateKeyWithoutKid {
  given encoder: JsonEncoder[OKPPrivateKeyWithoutKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[OKPPrivateKeyWithoutKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid     => Left("Expected OKPPrivateKeyWithoutKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKeyWithoutKid  => Left("Expected OKPPrivateKeyWithoutKid but got OKPPublicKeyWithoutKid")
      case k: OKPPrivateKeyWithKid    => Left("Expected OKPPrivateKeyWithoutKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKeyWithoutKid => Right(k)
      case k: ECPublicKeyWithKid      => Left("Expected OKPPrivateKeyWithoutKid but got ECPublicKeyWithKid")
      case k: ECPublicKeyWithoutKid   => Left("Expected OKPPrivateKeyWithoutKid but got ECPublicKeyWithoutKid")
      case k: ECPrivateKeyWithKid     => Left("Expected OKPPrivateKeyWithoutKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKeyWithoutKid  => Left("Expected OKPPrivateKeyWithoutKid but got ECPrivateKeyWithoutKid")
    }
}
