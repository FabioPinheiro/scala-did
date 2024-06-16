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

sealed trait MaybeKid { def maybeKid: Option[String] }
sealed trait WithKid extends MaybeKid { def kid: String; def maybeKid: Option[String] = Some(kid) }
sealed trait WithoutKid extends MaybeKid { def maybeKid: Option[String] = None }

@jsonDiscriminator("kty")
// sealed trait OKP_EC_Key {
sealed abstract class OKP_EC_Key extends JWKObj {
  def kty: KTY // EC.type = KTY.EC
  def crv: Curve
  // def kid: Option[String]
  def x: String
  def xNumbre = Base64.fromBase64url(x).decodeToBigInt

  // TODO // Should I make this type safe? Will add another dimension of types, just to move the error to the parser.
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
  def getCurve: OKPCurve = crv.asOKPCurve
}
sealed abstract class ECKey extends OKP_EC_Key {
  override def kty: KTY.EC.type
  def y: String
  def yNumbre = Base64.fromBase64url(y).decodeToBigInt
  def getCurve: ECCurve = crv.asECCurve
  def isPointOnCurve = getCurve.isPointOnCurve(xNumbre, yNumbre)
}

sealed trait PublicKey extends OKP_EC_Key
sealed trait PublicKeyWithKid extends PublicKey with WithKid
sealed trait PublicKeyWithoutKid extends PublicKey with WithoutKid
sealed trait PrivateKey extends OKP_EC_Key { def toPublicKey: PublicKey; def d: String }
sealed trait PrivateKeyWithKid extends PrivateKey with WithKid
sealed trait PrivateKeyWithoutKid extends PrivateKey with WithoutKid

sealed trait OKPPublicKey extends OKPKey with PublicKey
case class OKPPublicKeyWithKid(kty: KTY.OKP.type, crv: Curve, x: String, kid: String)
    extends OKPPublicKey
    with PublicKeyWithKid
case class OKPPublicKeyWithoutKid(kty: KTY.OKP.type, crv: Curve, x: String)
    extends OKPPublicKey
    with PublicKeyWithoutKid

sealed trait OKPPrivateKey extends OKPKey with PrivateKey
case class OKPPrivateKeyWithKid(kty: KTY.OKP.type, crv: Curve, d: String, x: String, kid: String)
    extends OKPPrivateKey
    with PrivateKeyWithKid {
  def toPublicKey: OKPPublicKeyWithKid = OKPPublicKeyWithKid(kty = kty, crv = crv, x = x, kid = kid)
}
case class OKPPrivateKeyWithoutKid(kty: KTY.OKP.type, crv: Curve, d: String, x: String)
    extends OKPPrivateKey
    with PrivateKeyWithoutKid {
  def toPublicKey: OKPPublicKeyWithoutKid = OKPPublicKeyWithoutKid(kty = kty, crv = crv, x = x)
}

sealed trait ECPublicKey extends ECKey with PublicKey

case class ECPublicKeyWithKid(kty: KTY.EC.type, crv: Curve, x: String, y: String, kid: String)
    extends ECPublicKey
    with PublicKeyWithKid
case class ECPublicKeyWithoutKid(kty: KTY.EC.type, crv: Curve, x: String, y: String)
    extends ECPublicKey
    with PublicKeyWithoutKid

sealed trait ECPrivateKey extends ECKey with PrivateKey

case class ECPrivateKeyWithKid(kty: KTY.EC.type, crv: Curve, d: String, x: String, y: String, kid: String)
    extends ECPrivateKey
    with PrivateKeyWithKid {
  def toPublicKey: ECPublicKeyWithKid = ECPublicKeyWithKid(kty = kty, crv = crv, x = x, y = y, kid = kid)
}
case class ECPrivateKeyWithoutKid(kty: KTY.EC.type, crv: Curve, d: String, x: String, y: String)
    extends ECPrivateKey
    with PrivateKeyWithoutKid {
  def toPublicKey: ECPublicKeyWithoutKid = ECPublicKeyWithoutKid(kty = kty, crv = crv, x = x, y = y)
}
// object OKP_EC_Key {
//   given decoder: JsonDecoder[OKP_EC_Key] = ???
//   given encoder: JsonEncoder[OKP_EC_Key] = ???
// }

object PublicKey {

  given decoder: JsonDecoder[PublicKey] = Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst
      .get(JsonCursor.field("kty"))
      .flatMap(ast => KTY.decoder.fromJsonAST(ast))
      .flatMap {
        case KTY.EC =>
          // ECPublicKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ? see didJVM/testOnly *.KeySuite (parse Key with no kid)
          ECPublicKey.decoder.decodeJson(originalAst.toJson)
        case KTY.OKP =>
          // OKPPublicKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ? see didJVM/testOnly *.KeySuite (parse Key with no kid)
          OKPPublicKey.decoder.decodeJson(originalAst.toJson)
      }
  }

  given encoder: JsonEncoder[PublicKey] = new JsonEncoder[PublicKey] {
    override def unsafeEncode(b: PublicKey, indent: Option[Int], out: zio.json.internal.Write): Unit = b match {
      case obj: OKPPublicKey => OKPPublicKey.encoder.unsafeEncode(obj, indent, out)
      case obj: ECPublicKey  => ECPublicKey.encoder.unsafeEncode(obj, indent, out)
    }
  }

}

object PrivateKey {
  given Conversion[PrivateKey, PublicKey] with
    def apply(key: PrivateKey) = key.toPublicKey

  given decoder: JsonDecoder[PrivateKey] = Json.Obj.decoder.mapOrFail { originalAst =>
    originalAst
      .get(JsonCursor.field("kty"))
      .flatMap { ast => KTY.decoder.fromJsonAST(ast) }
      .flatMap {
        case KTY.EC =>
          // ECPrivateKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ?
          ECPrivateKey.decoder.decodeJson(originalAst.toJson)
        case KTY.OKP =>
          // OKPPrivateKey.decoder.fromJsonAST(originalAst) // FIXME REPORT BUG ?
          OKPPrivateKey.decoder.decodeJson(originalAst.toJson)
      }
  }
  given encoder: JsonEncoder[PrivateKey] = new JsonEncoder[PrivateKey] {
    override def unsafeEncode(b: PrivateKey, indent: Option[Int], out: zio.json.internal.Write): Unit = b match {
      case obj: OKPPrivateKey => OKPPrivateKey.encoder.unsafeEncode(obj, indent, out)
      case obj: ECPrivateKey  => ECPrivateKey.encoder.unsafeEncode(obj, indent, out)
    }
  }
}

object PublicKeyWithKid {
  given decoder: JsonDecoder[PublicKeyWithKid] = DeriveJsonDecoder.gen[PublicKeyWithKid] // FIXME
  given encoder: JsonEncoder[PublicKeyWithKid] = DeriveJsonEncoder.gen[PublicKeyWithKid] // FIXME
}
object PublicKeyWithoutKid {
  given decoder: JsonDecoder[PublicKeyWithoutKid] = DeriveJsonDecoder.gen[PublicKeyWithoutKid] // FIXME
  given encoder: JsonEncoder[PublicKeyWithoutKid] = DeriveJsonEncoder.gen[PublicKeyWithoutKid] // FIXME
}
object PrivateKeyWithKid {
  given decoder: JsonDecoder[PrivateKeyWithKid] = DeriveJsonDecoder.gen[PrivateKeyWithKid] // FIXME
  given encoder: JsonEncoder[PrivateKeyWithKid] = DeriveJsonEncoder.gen[PrivateKeyWithKid] // FIXME
}
object PrivateKeyWithoutKid {
  given decoder: JsonDecoder[PrivateKeyWithoutKid] = DeriveJsonDecoder.gen[PrivateKeyWithoutKid] // FIXME
  given encoder: JsonEncoder[PrivateKeyWithoutKid] = DeriveJsonEncoder.gen[PrivateKeyWithoutKid] // FIXME
}

object ECPublicKey {
  given decoder: JsonDecoder[ECPublicKey] = DeriveJsonDecoder.gen[ECPublicKey] // FIXME
  given encoder: JsonEncoder[ECPublicKey] = DeriveJsonEncoder.gen[ECPublicKey] // FIXME
  def unapply(key: ECPublicKey): Option[(KTY, Curve, String, String, Option[String])] =
    Some((key.kty, key.crv, key.x, key.y, key.maybeKid))
}
object ECPrivateKey {
  given decoder: JsonDecoder[ECPrivateKey] = DeriveJsonDecoder.gen[ECPrivateKey] // FIXME
  given encoder: JsonEncoder[ECPrivateKey] = DeriveJsonEncoder.gen[ECPrivateKey] // FIXME
  def unapply(key: ECPrivateKey): Option[(KTY, Curve, String, String, String, Option[String])] =
    Some((key.kty, key.crv, key.d, key.x, key.y, key.maybeKid))

}

object OKPPublicKey {
  given decoder: JsonDecoder[OKPPublicKey] = DeriveJsonDecoder.gen[OKPPublicKey] // FIXME
  given encoder: JsonEncoder[OKPPublicKey] = DeriveJsonEncoder.gen[OKPPublicKey] // FIXME
  def unapply(key: OKPPublicKey): Option[(KTY, Curve, String, Option[String])] =
    Some((key.kty, key.crv, key.x, key.maybeKid))
}
object OKPPrivateKey {
  given decoder: JsonDecoder[OKPPrivateKey] = DeriveJsonDecoder.gen[OKPPrivateKey] // FIXME
  given encoder: JsonEncoder[OKPPrivateKey] = DeriveJsonEncoder.gen[OKPPrivateKey] // FIXME
  def unapply(key: OKPPrivateKey): Option[(KTY, Curve, String, String, Option[String])] =
    Some((key.kty, key.crv, key.d, key.x, key.maybeKid))
}
//TODO
// object OKPPrivateKeyWithKid {
//   given decoder: JsonDecoder[OKPPrivateKeyWithKid] = DeriveJsonDecoder.gen[OKPPrivateKeyWithKid]
//   given encoder: JsonEncoder[OKPPrivateKeyWithKid] = DeriveJsonEncoder.gen[OKPPrivateKeyWithKid]
// }
// object OKPPrivateKeyWithoutKid {
//   given decoder: JsonDecoder[OKPPrivateKeyWithoutKid] = DeriveJsonDecoder.gen[OKPPrivateKeyWithoutKid]
//   given encoder: JsonEncoder[OKPPrivateKeyWithoutKid] = DeriveJsonEncoder.gen[OKPPrivateKeyWithoutKid]
// }
