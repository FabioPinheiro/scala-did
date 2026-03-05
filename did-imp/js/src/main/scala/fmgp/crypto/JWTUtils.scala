package fmgp.crypto

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import fmgp.typings.jose.typesMod.JWTHeaderParameters
import fmgp.crypto.UtilsJS.toPickJWKktycrvxyen
import fmgp.util.Base64

extension (header: JWTHeader) {

  def makeJWTHeader: JWTHeaderParameters = {
    val h = JWTHeaderParameters(header.alg.symbol)
    header match
      case JWTHeader(alg, jku, jwk, kid, typ, cty, crit) =>
        jku.map(e => h.setJku(e))
        jwk.map(e => h.setJwk(e.toPickJWKktycrvxyen))
        kid.map(e => h.setKid(e))
        typ.map(e => h.setTyp(e))
        cty.map(e => h.setCty(e))
        crit.map(e => h.setCrit(e.toJSArray))
    h
  }

  def jsonStr: String = js.JSON.stringify(header.makeJWTHeader)

  /** JWT uses base64url */
  def base64: Base64 = Base64.encode(jsonStr)
}
