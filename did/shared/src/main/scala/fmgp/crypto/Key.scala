package fmgp.crypto

import zio.json._
import zio.json.ast.Json
import zio.json.ast.JsonCursor
import fmgp.util.{Base64, safeValueOf}
import fmgp.did.comm.JSON_RFC7159.apply

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

opaque type ECCurveOpaque = Curve.`P-256`.type | Curve.`P-384`.type | Curve.`P-521`.type | Curve.secp256k1.type
object ECCurveOpaque:
  def apply(value: Curve.`P-256`.type | Curve.`P-384`.type | Curve.`P-521`.type | Curve.secp256k1.type): ECCurveOpaque =
    value
  extension (curveEnum: ECCurveOpaque)
    def curve: Curve.`P-256`.type | Curve.`P-384`.type | Curve.`P-521`.type | Curve.secp256k1.type = curveEnum
  given decoder: JsonDecoder[ECCurveOpaque] = JsonDecoder.string.mapOrFail(e =>
    safeValueOf(Curve.valueOf(e)).flatMap {
      case o: ECCurve  => Right(o)
      case o: OKPCurve => Left(s"$o is a OKPCruve but is expecting a ECCruve")
    }
  )
opaque type OKPCurveOpaque = OKPCurve
object OKPCurveOpaque:
  def apply(
      value: OKPCurve
  ): OKPCurveOpaque =
    value
  extension (curveEnum: OKPCurveOpaque) def curve: OKPCurve = curveEnum
  given decoder: JsonDecoder[OKPCurveOpaque] = JsonDecoder.string.mapOrFail(e =>
    safeValueOf(Curve.valueOf(e)).flatMap {
      case o: OKPCurve => Right(o)
      case o: ECCurve  => Left(s"$o is a ECCruve but is expecting a OKPCruve")
    }
  )

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
  def maybeKid: Option[String] = this match
    case o: WithKid => Some(o.kid)
    case o          => None
}
sealed trait WithKid extends MaybeKid { def kid: String; override final def maybeKid: Option[String] = Some(kid) }

@jsonDiscriminator("kty")
// sealed trait OKP_EC_Key {
sealed abstract class OKP_EC_Key extends JWKObj {
  def kty: KTY // EC.type = KTY.EC
  def crv: Curve
  // def kid: Option[String]
  def x: String
  def xNumbre = Base64.fromBase64url(x).decodeToBigInt

  // TODO // Should I make this type safe? Will add another dimension of types, just to move the error to the parser.
  // REMOVE. Because this is now type safe
  assert(
    crv match {
      case Curve.secp256k1 => kty == KTY.EC
      case Curve.`P-256`   => kty == KTY.EC
      case Curve.`P-384`   => kty == KTY.EC
      case Curve.`P-521`   => kty == KTY.EC
      case Curve.X25519    => kty == KTY.OKP
      case Curve.Ed25519   => kty == KTY.OKP
    },
    s"$crv is not a $kty alg"
  )

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
sealed abstract class ECKey extends OKP_EC_Key {
  override def kty: KTY.EC.type
  def y: String
  def yNumbre = Base64.fromBase64url(y).decodeToBigInt
  def getCurve: ECCurve = crv.asECCurve
  def isPointOnCurve = getCurve.isPointOnCurve(xNumbre, yNumbre)
}

sealed trait PublicKeyWithKid extends PublicKey with WithKid
sealed trait PublicKey extends OKP_EC_Key
sealed trait PrivateKeyWithKid extends PrivateKey with WithKid
sealed trait PrivateKey extends OKP_EC_Key { def toPublicKey: PublicKey; def d: String }

// ##########
// ## OKP ###
// ##########

case class OKPPublicKeyWithKid(
    override val kty: KTY.OKP.type,
    override val crv: OKPCurve,
    override val x: String,
    override val kid: String
) extends OKPPublicKey(kty = kty, crv = crv, x = x)
    with PublicKeyWithKid

sealed class OKPPublicKey(
    override val kty: KTY.OKP.type,
    override val crv: OKPCurve,
    override val x: String
) extends OKPKey
    with PublicKey

case class OKPPrivateKeyWithKid(
    override val kty: KTY.OKP.type,
    override val crv: OKPCurve,
    override val d: String,
    override val x: String,
    override val kid: String
) extends OKPPrivateKey(kty = kty, crv = crv, d = d, x = x)
    with PrivateKeyWithKid {
  override def toPublicKey: OKPPublicKeyWithKid = OKPPublicKeyWithKid(kty = kty, crv = crv, x = x, kid = kid)
}

sealed class OKPPrivateKey(
    override val kty: KTY.OKP.type,
    override val crv: OKPCurve,
    override val d: String,
    override val x: String
) extends OKPKey
    with PrivateKey {

  def toPublicKey: OKPPublicKey = OKPPublicKey(kty = kty, crv = crv, x = x)
}

// ##########
// ### EC ###
// ##########

case class ECPublicKeyWithKid(
    override val kty: KTY.EC.type,
    override val crv: ECCurve,
    override val x: String,
    override val y: String,
    override val kid: String
) extends ECPublicKey(kty = kty, crv = crv, x = x, y = y)
    with PublicKeyWithKid

sealed class ECPublicKey(
    override val kty: KTY.EC.type,
    override val crv: ECCurve,
    override val x: String,
    override val y: String
) extends ECKey
    with PublicKey

case class ECPrivateKeyWithKid(
    override val kty: KTY.EC.type,
    override val crv: ECCurve,
    override val d: String,
    override val x: String,
    override val y: String,
    override val kid: String
) extends ECPrivateKey(kty = kty, crv = crv, d = d, x = x, y = y)
    with PrivateKeyWithKid {
  override def toPublicKey: ECPublicKeyWithKid = ECPublicKeyWithKid(kty = kty, crv = crv, x = x, y = y, kid = kid)
}

sealed class ECPrivateKey(
    override val kty: KTY.EC.type,
    override val crv: ECCurve,
    override val d: String,
    override val x: String,
    override val y: String
) extends ECKey
    with PrivateKey {
  override def toPublicKey: ECPublicKey = ECPublicKey(kty = kty, crv = crv, x = x, y = y)
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
        ).flatten: _*
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
                case (None, None)      => ECPublicKey(kty = ktyEC, crv = crv.curve, x = x, y = y)
                case (None, Some(kid)) => ECPublicKeyWithKid(kty = ktyEC, crv = crv.curve, x = x, y = y, kid = kid)
                case (Some(d), None)   => ECPrivateKey(kty = ktyEC, crv = crv.curve, d = d, x = x, y = y)
                case (Some(d), Some(kid)) =>
                  ECPrivateKeyWithKid(kty = ktyEC, crv = crv.curve, d = d, x = x, y = y, kid = kid)
            } yield ret
          case ktyOKP: KTY.OKP.type =>
            for {
              crv <- obj.get(crvCursor).flatMap(_.as[OKPCurveOpaque])
              ret = (maybeD, maybeKid) match
                case (None, None)      => OKPPublicKey(kty = ktyOKP, crv = crv.curve, x = x)
                case (None, Some(kid)) => OKPPublicKeyWithKid(kty = ktyOKP, crv = crv.curve, x = x, kid = kid)
                case (Some(d), None)   => OKPPrivateKey(kty = ktyOKP, crv = crv.curve, d = d, x = x)
                case (Some(d), Some(kid)) =>
                  OKPPrivateKeyWithKid(kty = ktyOKP, crv = crv.curve, d = d, x = x, kid = kid)
            } yield ret
      } yield key
    }
}

object PublicKey {
  given encoder: JsonEncoder[PublicKey] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[PublicKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Right(k)
      case k: OKPPublicKey         => Right(k)
      case k: OKPPrivateKeyWithKid => Right(k.toPublicKey)
      case k: OKPPrivateKey        => Right(k.toPublicKey)
      case k: ECPublicKeyWithKid   => Right(k)
      case k: ECPublicKey          => Right(k)
      case k: ECPrivateKeyWithKid  => Right(k.toPublicKey)
      case k: ECPrivateKey         => Right(k.toPublicKey)
    }
  // given decoder: JsonDecoder[PublicKey] = Json.Obj.decoder.mapOrFail { originalAst =>
  //   originalAst
  //     .get(JsonCursor.field("kty"))
  //     .flatMap(ast => KTY.decoder.fromJsonAST(ast))
  //     .flatMap {
  //       case KTY.EC =>
  //         // ECPublicKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ? see didJVM/testOnly *.KeySuite (parse Key with no kid)
  //         ECPublicKey.decoder.decodeJson(originalAst.toJson)
  //       case KTY.OKP =>
  //         // OKPPublicKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ? see didJVM/testOnly *.KeySuite (parse Key with no kid)
  //         OKPPublicKey.decoder.decodeJson(originalAst.toJson)
  //     }
  // }

  // given encoder: JsonEncoder[PublicKey] = new JsonEncoder[PublicKey] {
  //   override def unsafeEncode(b: PublicKey, indent: Option[Int], out: zio.json.internal.Write): Unit = b match {
  //     case obj: OKPPublicKey => OKPPublicKey.encoder.unsafeEncode(obj, indent, out)
  //     case obj: ECPublicKey  => ECPublicKey.encoder.unsafeEncode(obj, indent, out)
  //   }
  // }

}

object PublicKeyWithKid {
  given encoder: JsonEncoder[PublicKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[PublicKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Right(k)
      case k: OKPPublicKey         => Left("Expected PublicKeyWithKid but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Right(k.toPublicKey)
      case k: OKPPrivateKey        => Left("Expected PublicKeyWithKid but got OKPPrivateKey")
      case k: ECPublicKeyWithKid   => Right(k)
      case k: ECPublicKey          => Left("Expected PublicKeyWithKid but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Right(k.toPublicKey)
      case k: ECPrivateKey         => Left("Expected PublicKeyWithKid but got ECPrivateKey")
    }
}

object PrivateKey {
  given encoder: JsonEncoder[PrivateKey] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[PrivateKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Left("Expected PrivateKey but got OKPPublicKeyWithKid")
      case k: OKPPublicKey         => Left("Expected PrivateKey but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Right(k)
      case k: OKPPrivateKey        => Right(k)
      case k: ECPublicKeyWithKid   => Left("Expected PrivateKey but got ECPublicKeyWithKid")
      case k: ECPublicKey          => Left("Expected PrivateKey but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Right(k)
      case k: ECPrivateKey         => Right(k)
    }
  // given Conversion[PrivateKey, PublicKey] with
  //   def apply(key: PrivateKey) = key.toPublicKey
}

object PrivateKeyWithKid {
  given encoder: JsonEncoder[PrivateKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[PrivateKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Left("Expected PrivateKeyWithKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKey         => Left("Expected PrivateKeyWithKid but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Right(k)
      case k: OKPPrivateKey        => Left("Expected PrivateKeyWithKid but got OKPPrivateKey")
      case k: ECPublicKeyWithKid   => Left("Expected PrivateKeyWithKid but got ECPublicKeyWithKid")
      case k: ECPublicKey          => Left("Expected PrivateKeyWithKid but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Right(k)
      case k: ECPrivateKey         => Left("Expected PrivateKeyWithKid but got ECPrivateKey")
    }
}

object ECPublicKey {
//   given encoder: JsonEncoder[ECPublicKey] = DeriveJsonEncoder.gen[ECPublicKey] // FIXME
  def apply(kty: KTY.EC.type, crv: ECCurve, x: String, y: String): ECPublicKey =
    new ECPublicKey(kty = kty, crv = crv, x = x, y = y)
  def apply(kty: KTY.EC.type, crv: ECCurve, x: String, y: String, kid: String): ECPublicKeyWithKid =
    ECPublicKeyWithKid(kty = kty, crv = crv, x = x, y = y, kid = kid)
  def unapply(key: ECPublicKey): Option[(KTY, ECCurve, String, String, Option[String])] =
    Some((key.kty, key.crv, key.x, key.y, key.maybeKid))

  given encoder: JsonEncoder[ECPublicKey] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[ECPublicKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Left("Expected ECPublicKey but got OKPPublicKeyWithKid")
      case k: OKPPublicKey         => Left("Expected ECPublicKey but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Left("Expected ECPublicKey but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKey        => Left("Expected ECPublicKey but got OKPPrivateKey")
      case k: ECPublicKeyWithKid   => Right(k)
      case k: ECPublicKey          => Right(k)
      case k: ECPrivateKeyWithKid  => Right(k.toPublicKey)
      case k: ECPrivateKey         => Right(k.toPublicKey)
    }
}

object ECPublicKeyWithKid {
  given encoder: JsonEncoder[ECPublicKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[ECPublicKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Left("Expected ECPublicKeyWithKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKey         => Left("Expected ECPublicKeyWithKid but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Left("Expected ECPublicKeyWithKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKey        => Left("Expected ECPublicKeyWithKid but got OKPPrivateKey")
      case k: ECPublicKeyWithKid   => Right(k)
      case k: ECPublicKey          => Left("Expected ECPublicKeyWithKid but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Right(k.toPublicKey)
      case k: ECPrivateKey         => Left("Expected ECPublicKeyWithKid but got ECPrivateKey")
    }
}

object ECPrivateKey {
  def apply(kty: KTY.EC.type, crv: ECCurve, d: String, x: String, y: String): ECPrivateKey =
    new ECPrivateKey(kty = kty, crv = crv, d = d, x = x, y = y)
  def apply(kty: KTY.EC.type, crv: ECCurve, d: String, x: String, y: String, kid: String): ECPrivateKeyWithKid =
    ECPrivateKeyWithKid(kty = kty, crv = crv, d = d, x = x, y = y, kid = kid)
  def unapply(key: ECPrivateKey): Option[(KTY, ECCurve, String, String, String, Option[String])] =
    Some((key.kty, key.crv, key.d, key.x, key.y, key.maybeKid))

  given encoder: JsonEncoder[ECPrivateKey] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[ECPrivateKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Left("Expected ECPrivateKey but got OKPPublicKeyWithKid")
      case k: OKPPublicKey         => Left("Expected ECPrivateKey but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Left("Expected ECPrivateKey but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKey        => Left("Expected ECPrivateKey but got OKPPrivateKey")
      case k: ECPublicKeyWithKid   => Left("Expected ECPrivateKey but got ECPublicKeyWithKid")
      case k: ECPublicKey          => Left("Expected ECPrivateKey but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Right(k)
      case k: ECPrivateKey         => Right(k)
    }
}

object ECPrivateKeyWithKid {
  given encoder: JsonEncoder[ECPrivateKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[ECPrivateKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Left("Expected ECPrivateKeyWithKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKey         => Left("Expected ECPrivateKeyWithKid but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Left("Expected ECPrivateKeyWithKid but got OKPPrivateKeyWithKid")
      case k: OKPPrivateKey        => Left("Expected ECPrivateKeyWithKid but got OKPPrivateKey")
      case k: ECPublicKeyWithKid   => Left("Expected ECPrivateKeyWithKid but got ECPublicKeyWithKid")
      case k: ECPublicKey          => Left("Expected ECPrivateKeyWithKid but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Right(k)
      case k: ECPrivateKey         => Left("Expected ECPrivateKeyWithKid but got ECPrivateKey")
    }
}

object OKPPublicKey {
  def apply(kty: KTY.OKP.type, crv: OKPCurve, x: String): OKPPublicKey =
    new OKPPublicKey(kty = kty, crv = crv, x = x)
  def apply(kty: KTY.OKP.type, crv: OKPCurve, x: String, kid: String): OKPPublicKeyWithKid =
    OKPPublicKeyWithKid(kty = kty, crv = crv, x = x, kid = kid)
  def unapply(key: OKPPublicKey): Option[(KTY, OKPCurve, String, Option[String])] =
    Some((key.kty, key.crv, key.x, key.maybeKid))

  given encoder: JsonEncoder[OKPPublicKey] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[OKPPublicKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Right(k)
      case k: OKPPublicKey         => Right(k)
      case k: OKPPrivateKeyWithKid => Right(k.toPublicKey)
      case k: OKPPrivateKey        => Right(k.toPublicKey)
      case k: ECPublicKeyWithKid   => Left("Expected OKPPublicKey but got ECPublicKeyWithKid")
      case k: ECPublicKey          => Left("Expected OKPPublicKey but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Left("Expected OKPPublicKey but got ECPrivateKeyWithKid")
      case k: ECPrivateKey         => Left("Expected OKPPublicKey but got ECPrivateKey")
    }
}

object OKPPublicKeyWithKid {
  given encoder: JsonEncoder[OKPPublicKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  // TODO the code 'Right(k.toPublicKey)' // Can be optimized at compile time
  given decoder: JsonDecoder[OKPPublicKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Right(k)
      case k: OKPPublicKey         => Left("Expected OKPPublicKeyWithKid but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Right(k.toPublicKey)
      case k: OKPPrivateKey        => Left("Expected OKPPublicKeyWithKid but got OKPPrivateKey")
      case k: ECPublicKeyWithKid   => Left("Expected OKPPublicKeyWithKid but got ECPublicKeyWithKid")
      case k: ECPublicKey          => Left("Expected OKPPublicKeyWithKid but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Left("Expected OKPPublicKeyWithKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKey         => Left("Expected OKPPublicKeyWithKid but got ECPrivateKey")
    }
}

object OKPPrivateKey {

  def apply(kty: KTY.OKP.type, crv: OKPCurve, d: String, x: String): OKPPrivateKey =
    new OKPPrivateKey(kty = kty, crv = crv, d = d, x = x)
  def apply(kty: KTY.OKP.type, crv: OKPCurve, d: String, x: String, kid: String): OKPPrivateKeyWithKid =
    OKPPrivateKeyWithKid(kty = kty, crv = crv, d = d, x = x, kid = kid)
  def unapply(key: OKPPrivateKey): Option[(KTY, OKPCurve, String, String, Option[String])] =
    Some((key.kty, key.crv, key.d, key.x, key.maybeKid))

  given encoder: JsonEncoder[OKPPrivateKey] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[OKPPrivateKey] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Left("Expected OKPPrivateKey but got OKPPublicKeyWithKid")
      case k: OKPPublicKey         => Left("Expected OKPPrivateKey but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Right(k)
      case k: OKPPrivateKey        => Right(k)
      case k: ECPublicKeyWithKid   => Left("Expected OKPPrivateKey but got ECPublicKeyWithKid")
      case k: ECPublicKey          => Left("Expected OKPPrivateKey but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Left("Expected OKPPrivateKey but got ECPrivateKeyWithKid")
      case k: ECPrivateKey         => Left("Expected OKPPrivateKey but got ECPrivateKey")
    }
}

object OKPPrivateKeyWithKid {
  given encoder: JsonEncoder[OKPPrivateKeyWithKid] = JsonEncoder[OKP_EC_Key].narrow
  given decoder: JsonDecoder[OKPPrivateKeyWithKid] = JsonDecoder[OKP_EC_Key]
    .mapOrFail {
      case k: OKPPublicKeyWithKid  => Left("Expected OKPPrivateKeyWithKid but got OKPPublicKeyWithKid")
      case k: OKPPublicKey         => Left("Expected OKPPrivateKeyWithKid but got OKPPublicKey")
      case k: OKPPrivateKeyWithKid => Right(k)
      case k: OKPPrivateKey        => Left("Expected OKPPrivateKeyWithKid but got OKPPrivateKey")
      case k: ECPublicKeyWithKid   => Left("Expected OKPPrivateKeyWithKid but got ECPublicKeyWithKid")
      case k: ECPublicKey          => Left("Expected OKPPrivateKeyWithKid but got ECPublicKey")
      case k: ECPrivateKeyWithKid  => Left("Expected OKPPrivateKeyWithKid but got ECPrivateKeyWithKid")
      case k: ECPrivateKey         => Left("Expected OKPPrivateKeyWithKid but got ECPrivateKey")
    }
}
