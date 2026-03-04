package fmgp.crypto

import com.nimbusds.jose.*
import com.nimbusds.jose.util.Base64URL

import fmgp.util.*
import fmgp.crypto.UtilsJVM.*
import fmgp.did.comm.*

given Conversion[Base64Obj[ProtectedHeader], JWEHeader] with
  def apply(x: Base64Obj[ProtectedHeader]) = {
    val encryptionMethod = x.obj.enc match
      case ENCAlgorithm.XC20P           => EncryptionMethod.XC20P
      case ENCAlgorithm.A256GCM         => EncryptionMethod.A256GCM
      case ENCAlgorithm.`A256CBC-HS512` => EncryptionMethod.A256CBC_HS512

    val algorithm = x.obj.alg match
      case KWAlgorithm.`ECDH-ES+A256KW`  => JWEAlgorithm.ECDH_ES_A256KW
      case KWAlgorithm.`ECDH-1PU+A256KW` => JWEAlgorithm.ECDH_1PU_A256KW

    x match
      case Base64Obj(_, Some(original)) =>
        JWEHeader.parse(Base64URL.from(original.urlBase64))
      case Base64Obj(obj, None) =>
        obj match
          case AnonProtectedHeader(epk, apv, typ, enc, alg) =>
            val aux = new JWEHeader.Builder(algorithm, encryptionMethod)
              .agreementPartyVInfo(apv.base64)
              .ephemeralPublicKey(epk.toJWK)
            typ.map(e => aux.`type`(JOSEObjectType(e.typ)))
            aux.build()
          case AuthProtectedHeader(epk, apv, skid, apu, typ, enc, alg) =>
            val aux = new JWEHeader.Builder(algorithm, encryptionMethod)
              .agreementPartyVInfo(apv.base64)
              .ephemeralPublicKey(epk.toJWK)
              .senderKeyID(skid.value)
              .agreementPartyUInfo(apu.base64)
            typ.map(e => aux.`type`(JOSEObjectType(e.typ)))
            aux.build()
  }

given Conversion[Base64, com.nimbusds.jose.util.Base64URL] with
  def apply(x: Base64) = new com.nimbusds.jose.util.Base64URL(x.urlBase64)

given Conversion[JWAAlgorithm, JWSAlgorithm] with
  def apply(alg: JWAAlgorithm) = alg.asNimbusds
