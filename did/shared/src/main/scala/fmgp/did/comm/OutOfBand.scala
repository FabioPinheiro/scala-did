package fmgp.did.comm

import util.chaining.scalaUtilChainingOps
import zio.json._

import fmgp.util.Base64

/** OutOfBand make more type safe for OOB with Message */

case class OutOfBand(msg: Message, data: Base64) {
  def makeURI(base: String) = base + "?_oob=" + data.urlBase64
}

/** OOB - OutOfBand */
object OutOfBand {
  private val errorInfo = Left("Missing '_oob'")

  def from(base64: Base64): Either[String, OutOfBand] =
    base64.decodeToString.fromJson[Message].map(msg => OutOfBand(msg, base64))
  def from(msg: Message) = OutOfBand(msg, Base64.encode(msg.toJson))

  def safeBase64(oobBase64: String): Either[String, OutOfBand] =
    Base64
      .safeBase64url(oobBase64)
      .left
      .map(error => s"'_oob' $error")
      .flatMap(data => OutOfBand.from(data))

  /** Parse a out-of-band URI into a OutOfBand message
    *
    * @param str
    *   out-of-band link (URI) as a String
    */
  def oob(str: String): Either[String, OutOfBand] =
    parse(str) match
      case None         => errorInfo
      case Some(base64) => OutOfBand.from(base64)

  /** TODO Behavior may change if issues doesn't go through. See
    * https://github.com/decentralized-identity/didcomm-messaging/issues/443
    */
  def parse(str: String): Option[Base64] = parseParameter(str).orElse(parseFragment(str))
  def parseParameter(str: String): Option[Base64] = extractURIParameter(str)
  def parseFragment(str: String): Option[Base64] = extractURIFragment(str)

  /** Note finding the '_oob' must be greedy */
  private val patternParameter = """^[^\?\#\s]*\?[^\#\s:]*?_oob=([^\#&\s:]+)[^\#\s:]*(\#[^\s]*)?$""".r
  inline def extractURIParameter(id: String) = id match {
    case patternParameter(_oob, fragment) => Option(_oob).filterNot(_.isEmpty()).map(Base64.fromBase64url(_))
    case _                                => None
  }

  /** Note finding the '_oob' must be greedy */
  private val patternFragment = """^[^\#\s]*\#.*?&?_oob=([^\#&\s:]+)[^\s]*$""".r
  inline def extractURIFragment(id: String) = id match {
    case patternFragment(_oobFragment) => Option(_oobFragment).filterNot(_.isEmpty()).map(Base64.fromBase64url(_))
    case _                             => None
  }
}
