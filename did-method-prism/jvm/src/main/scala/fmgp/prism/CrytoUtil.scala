package fmgp.prism

import org.bouncycastle.jce.ECNamedCurveTable
import java.security.KeyFactory
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import org.bouncycastle.jce.ECPointUtil
import java.security.spec.ECPublicKeySpec
import scala.util._
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.security.spec.ECPoint
import java.security.Signature
import fmgp.prism.PrismPublicKey.VoidKey
import fmgp.prism.PrismPublicKey.UncompressedECKey
import fmgp.prism.PrismPublicKey.CompressedECKey
import org.bouncycastle.crypto.params.ECDomainParameters
import java.security.SecureRandom
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import fmgp.util.bytes2Hex

object CrytoUtil {
  val provider = new org.bouncycastle.jce.provider.BouncyCastleProvider()

  def unsafeFromPrismPublicKey(key: PrismPublicKey): Either[String, java.security.PublicKey] = key match
    case VoidKey(id, reason)                       => Left(s"Fail to parse key '$id' becuase $reason")
    case UncompressedECKey(id, usage, curve, x, y) => unsafeFromByteCoordinates(x = x, y = y)
    case CompressedECKey(id, usage, curve, data)   => unsafeFromCompressed(data)

  // https://github.com/input-output-hk/atala-prism/blob/main/src/main/scala/io/iohk/atala/prism/node/crypto/CryptoUtils.scala
  def unsafeFromCompressed(com: Array[Byte]): Either[String, java.security.PublicKey] = {
    val params = ECNamedCurveTable.getParameterSpec("secp256k1")
    val fact = KeyFactory.getInstance("ECDSA", provider)
    val curve = params.getCurve
    val ellipticCurve = EC5Util.convertCurve(curve, params.getSeed)
    val point = ECPointUtil.decodePoint(ellipticCurve, com.toArray)
    val params2 = EC5Util.convertSpec(ellipticCurve, params)
    val keySpec = new ECPublicKeySpec(point, params2)
    Try(fact.generatePublic(keySpec)) match
      case Failure(exception: java.security.spec.InvalidKeySpecException) => Left(exception.getMessage())
      case Failure(exception)                                             => Left(exception.getMessage())
      case Success(value)                                                 => Right(value)
  }

  def unsafeFromByteCoordinates(x: Array[Byte], y: Array[Byte]): Either[String, java.security.PublicKey] = {
    val xx = BigInt(1, x)
    val yy = BigInt(1, y)
    val keyFactory = KeyFactory.getInstance("ECDSA", provider)
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    val ecNamedCurveSpec =
      ECNamedCurveSpec(ecParameterSpec.getName, ecParameterSpec.getCurve, ecParameterSpec.getG, ecParameterSpec.getN)
    val ecPublicKeySpec = ECPublicKeySpec(java.security.spec.ECPoint(xx.bigInteger, yy.bigInteger), ecNamedCurveSpec)

    Try(keyFactory.generatePublic(ecPublicKeySpec).asInstanceOf[java.security.interfaces.ECPublicKey]) match
      case Failure(exception: java.security.spec.InvalidKeySpecException) => Left(exception.getMessage())
      case Failure(exception)                                             => Left(exception.getMessage())
      case Success(value)                                                 => Right(value)
  }

  def unsafeFromBigIntegerCoordinates(x: BigInt, y: BigInt): Either[String, java.security.PublicKey] = {
    val params = ECNamedCurveTable.getParameterSpec("secp256k1")
    val fact = KeyFactory.getInstance("ECDSA", provider)
    val curve = params.getCurve
    val ellipticCurve = EC5Util.convertCurve(curve, params.getSeed)
    val point = new ECPoint(x.bigInteger, y.bigInteger)
    val params2 = EC5Util.convertSpec(ellipticCurve, params)
    val keySpec = new ECPublicKeySpec(point, params2)
    Try(fact.generatePublic(keySpec)) match
      case Failure(exception: java.security.spec.InvalidKeySpecException) => Left(exception.getMessage())
      case Failure(exception)                                             => Left(exception.getMessage())
      case Success(value)                                                 => Right(value)
  }

  def checkECDSASignature(
      msg: Array[Byte],
      sig: Array[Byte],
      pubKey: java.security.PublicKey
  ): Either[String, Boolean] = {
    val ecdsa = Signature.getInstance("SHA256withECDSA", provider)
    ecdsa.initVerify(pubKey)
    ecdsa.update(msg)
    Try(ecdsa.verify(sig)) match
      case Failure(exception: java.security.SignatureException) => Left(exception.getMessage())
      case Failure(exception)                                   => Left(exception.getMessage())
      case Success(value)                                       => Right(value)
  }

  def signECDSASignature(
      msg: Array[Byte],
      pivKey: java.security.PrivateKey
  ): Either[String, Array[Byte]] = {
    val ecdsa = Signature.getInstance("SHA256withECDSA", provider)
    ecdsa.initSign(pivKey)
    ecdsa.update(msg)
    Try(ecdsa.sign()) match
      case Failure(exception) => Left(exception.getMessage())
      case Success(value)     => Right(value)
  }

  def generateKeyPair = {
    val params = ECNamedCurveTable getParameterSpec ("secp256k1")
    // val fact = KeyFactory.getInstance("ECDSA", provider)
    val domainParams = new ECDomainParameters(params.getCurve, params.getG, params.getN, params.getH, params.getSeed)
    // val domainParams = new ECDomainParameters(params.getCurve, params.getG, params.getN, params.getH)

    val secureRandom = new SecureRandom()
    val keyParams = new ECKeyGenerationParameters(domainParams, secureRandom)

    val generator = ECKeyPairGenerator()
    generator.init(keyParams)
    val keyPair = generator.generateKeyPair()
    // val keyPair = generator.GenerateKeyPair();
    val privateKeyParams = keyPair.getPrivate.asInstanceOf[ECPrivateKeyParameters]
    val publicKeyParams = keyPair.getPublic.asInstanceOf[ECPublicKeyParameters]

    // Console.WriteLine($"Private key: {ToHex(privateKey.D.ToByteArrayUnsigned())}");
    // Console.WriteLine($"Public key: {ToHex(publicKey.Q.GetEncoded())}");

    val privateKeyBytes = privateKeyParams.getD.toByteArray
    val publicKeyBytes = publicKeyParams.getQ.getEncoded(true)

    // println(s"Private key: ${bytes2Hex(privateKeyBytes)}");
    // println(s"Public key:  ${bytes2Hex(publicKeyBytes)}");
    (privateKeyParams, publicKeyParams)
  }

}
