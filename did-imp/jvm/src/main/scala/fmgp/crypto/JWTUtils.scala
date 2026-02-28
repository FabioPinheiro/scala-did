package fmgp.crypto

import fmgp.util.Base64

extension (header: JWTHeader) {
  def jsonStr: String = UtilsJVM.makeJWSHeader(header).toString

  /** JWT uses base64url */
  def base64: Base64 = Base64.encode(jsonStr) // Base64(UtilsJVM.makeJWSHeader(header).toBase64URL().toString())
}
