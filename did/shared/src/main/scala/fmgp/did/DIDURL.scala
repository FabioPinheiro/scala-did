package fmgp.did

import fmgp.did.comm.FROMTO
object DIDURL {
  val pattern = """^did:([^\s:]+):([^/\?\#\s]*)([^\?\#\s]*)(\?[^\#\s:]*)?(\#.*)?$""".r
  // ------------------|--method-|------id-----|----path---|---query----|-fragment

  def parseString(id: String): Either[String, DIDURL] = id match {
    case pattern(namespace, subject, path, query, fragment) =>
      Right(
        DIDURL(
          namespace,
          subject,
          Option(path).getOrElse(""),
          Option(query).getOrElse(""),
          Option(fragment).getOrElse("")
        )
      )
    case _ => Left(s"Fail to parse DIDURL: '$id'")
  }

  /** @throws AssertionError if not a valid DIDSubject */
  inline def unsafeParseString(id: String): DIDURL = parseString(id) match
    case Right(value) => value
    case Left(fail)   => throw new java.lang.AssertionError(fail)

}

/** for https://identity.foundation/didcomm-messaging/spec/#construction */
type DIDURI = DIDURL //TODO is this the same?

/** DIDURL
  *
  * did-url = did path-abempty [ "?" query ] [ "#" fragment ]
  *
  * @see
  *   https://www.w3.org/TR/did-core/#did-url-syntax
  */
case class DIDURL(
    val namespace: String,
    didSyntax: DIDSyntax,
    path: PathAbempty = "",
    query: Query = "",
    fragment: Fragment = "",
) { self =>
  def specificId: String = didSyntax + path + query + fragment
  def string = s"did:$namespace:$specificId"
  def toFROMTO = FROMTO(s"did:$namespace:$didSyntax$path$query") // no fragment
  def toDID: DID = new {
    val namespace = self.namespace
    val specificId = self.specificId
  }

}

/** @see
  *   https://www.rfc-editor.org/rfc/rfc3986#section-3.3
  *
  * {{{
  * path          = path-abempty    ; begins with "/" or is empty
  *               / path-absolute   ; begins with "/" but not "//"
  *               / path-noscheme   ; begins with a non-colon segment
  *               / path-rootless   ; begins with a segment
  *               / path-empty      ; zero characters
  * path-abempty  = *( "/" segment )
  * path-absolute = "/" [ segment-nz *( "/" segment ) ]
  * path-noscheme = segment-nz-nc *( "/" segment )
  * path-rootless = segment-nz *( "/" segment )
  * path-empty    = 0<pchar>
  * }}}
  */
type PathAbempty = String

/** @see
  *   https://www.rfc-editor.org/rfc/rfc3986#section-3.4
  *
  * {{{
  * query       = *( pchar / "/" / "?" )
  * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
  * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~""
  * pct-encoded = "%" HEXDIG HEXDIG
  * sub-delims    = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
  * }}}
  */
type Query = String

/** @see
  *   https://www.rfc-editor.org/rfc/rfc3986#section-3.5
  *
  * {{{
  * query       = *( pchar / "/" / "?" )
  * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
  * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~""
  * pct-encoded = "%" HEXDIG HEXDIG
  * sub-delims    = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
  * }}}
  */
type Fragment = String
