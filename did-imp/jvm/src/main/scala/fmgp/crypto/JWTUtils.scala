package fmgp.crypto

import scala.jdk.CollectionConverters.*
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JOSEObjectType
import fmgp.util.Base64
import fmgp.crypto.UtilsJVM.*

extension (header: JWTHeader) {
  def makeJWTHeader: JWSHeader = {
    val h = new JWSHeader.Builder(header.alg.asNimbusds)
    header match
      case JWTHeader(alg, jku, jwk, kid, typ, cty, crit) =>
        jku.map(e => h.jwkURL(java.net.URI(e)))
        jwk.map(e => h.jwk(e.toJWK))
        kid.map(e => h.keyID(e))
        typ.map(e => h.`type`(JOSEObjectType(e)))
        cty.map(e => h.contentType(e))
        crit.map(e => h.criticalParams(e.asJava))
    h.build()
  }

  def jsonStr: String = header.makeJWTHeader.toString()

  /** JWT uses base64url */
  def base64: Base64 = Base64.encode(jsonStr) // Base64(UtilsJVM.makeJWSHeader(header).toBase64URL().toString())
}
