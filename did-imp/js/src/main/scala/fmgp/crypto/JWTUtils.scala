package fmgp.crypto

import fmgp.util.Base64
import scala.scalajs.js

extension (header: JWTHeader) {
  def jsonStr: String = js.JSON.stringify(UtilsJS.makeJWSHeader(header))

  /** JWT uses base64url */
  def base64: Base64 = Base64.encode(jsonStr)
}
