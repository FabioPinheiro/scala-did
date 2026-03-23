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

  // NOTE: Using js.Dynamic.global.JSON instead of js.JSON to avoid Scaladoc TASTy resolution error:
  // "undefined: scala.scalajs.js.JSON.stringify # -1: TermRef(...) at readTasty"
  def jsonStr: String = js.Dynamic.global.JSON.stringify(header.makeJWTHeader.asInstanceOf[js.Any]).asInstanceOf[String]

  /** JWT uses base64url */
  def base64: Base64 = Base64.encode(jsonStr)
}
