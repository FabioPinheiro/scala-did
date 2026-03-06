package fmgp.vc.sdjwt

import fmgp.util.Base64
import fmgp.crypto.SHA256
import zio.json.*
import zio.json.ast.Json
import java.security.SecureRandom

/** Sealed trait for SD-JWT selective disclosure.
  *
  * Two concrete types:
  *   - `ArrayElementDisclosure`: Array element (no claim name) `[salt, claimValue]`
  *   - `ObjectPropertyDisclosure`: Object property (with claim name) `[salt, claimName, claimValue]`
  */
sealed trait Disclosure:
  /** Cryptographic random salt (base64url-encoded, minimum 128-bit entropy) */
  def salt: String

  /** The claim value (can be any JSON-serializable value) */
  def claimValue: Json

  /** The JSON array representation of this disclosure.
    * Implementation differs per subtype.
    */
  def json: String

  /** The base64url-encoded disclosure string (without padding). */
  lazy val disclosure: String =
    Base64.encode(json.getBytes("UTF-8")).urlBase64WithoutPadding

  /** Cached SHA-256 digest of the disclosure string. */
  lazy val defaultDigest: String =
    Base64.encode(SHA256.digest(disclosure)).urlBase64WithoutPadding

  /** Compute digest with specified hash algorithm.
    *
    * @param hashAlgorithm
    *   Hash algorithm to use (default: "sha-256")
    * @return
    *   Base64url-encoded digest
    */
  def digest(hashAlgorithm: String = "sha-256"): String =
    if hashAlgorithm == "sha-256" then defaultDigest
    else throw IllegalArgumentException(s"Unsupported hash algorithm: $hashAlgorithm")

  override def toString: String = disclosure

  override def hashCode: Int = disclosure.hashCode

  override def equals(obj: Any): Boolean = obj match
    case that: Disclosure => this.disclosure == that.disclosure
    case _ => false

/** Array element disclosure: `[salt, claimValue]`
  *
  * Represents an SD-JWT array element disclosure without a claim name.
  * Per the SD-JWT specification, these are encoded as 2-element JSON arrays.
  *
  * @param salt
  *   Cryptographic random salt (base64url-encoded, minimum 128-bit entropy)
  * @param claimValue
  *   The claim value (can be any JSON-serializable value)
  */
final case class ArrayElementDisclosure(
    salt: String,
    claimValue: Json
) extends Disclosure:

  lazy val json: String =
    Json.Arr(Json.Str(salt), claimValue).toJson

  /** Convert to array element representation for SD-JWT spec.
    *
    * Returns a Map with key "..." and value as the disclosure digest.
    * This is used when including the disclosure in an array per the SD-JWT specification.
    *
    * @param hashAlgorithm
    *   Hash algorithm to use for digest
    * @return
    *   Map containing `{"...": "<digest>"}`
    */
  def toArrayElement(hashAlgorithm: String = "sha-256"): Map[String, String] =
    Map("..." -> digest(hashAlgorithm))

/** Object property disclosure: `[salt, claimName, claimValue]`
  *
  * Represents an SD-JWT object property disclosure with a claim name.
  * Per the SD-JWT specification, these are encoded as 3-element JSON arrays.
  *
  * @param salt
  *   Cryptographic random salt (base64url-encoded, minimum 128-bit entropy)
  * @param claimName
  *   The name of the claim (must not be a reserved name)
  * @param claimValue
  *   The claim value (can be any JSON-serializable value)
  */
final case class ObjectPropertyDisclosure(
    salt: String,
    claimName: String,
    claimValue: Json
) extends Disclosure:

  lazy val json: String =
    Json.Arr(Json.Str(salt), Json.Str(claimName), claimValue).toJson

object Disclosure:

  private val secureRandom = new SecureRandom()

  /** Generate a cryptographically secure random salt.
    *
    * Generates 16 bytes (128 bits) of entropy and encodes as base64url.
    *
    * @return
    *   Base64url-encoded salt string
    */
  private def generateSalt(): String =
    val bytes = new Array[Byte](16)
    secureRandom.nextBytes(bytes)
    Base64.encode(bytes).urlBase64WithoutPadding

  /** Create an array element disclosure (no claim name).
    *
    * @param claimValue
    *   The value to disclose
    * @param salt
    *   Optional explicit salt (generates random if not provided)
    * @return
    *   Immutable ArrayElementDisclosure instance
    */
  def arrayElement(claimValue: Json, salt: String = generateSalt()): ArrayElementDisclosure =
    require(salt != null && salt.nonEmpty, "Salt must not be null or empty")
    ArrayElementDisclosure(salt = salt, claimValue = claimValue)

  /** Create an object property disclosure (with claim name).
    *
    * @param claimName
    *   The name of the claim
    * @param claimValue
    *   The value to disclose
    * @param salt
    *   Optional explicit salt (generates random if not provided)
    * @return
    *   Immutable ObjectPropertyDisclosure instance
    */
  def property(claimName: String, claimValue: Json, salt: String = generateSalt()): ObjectPropertyDisclosure =
    require(salt != null && salt.nonEmpty, "Salt must not be null or empty")
    require(claimName != null && claimName.nonEmpty, "Claim name must not be null or empty")
    // Validate no reserved keys
    require(
      claimName != "_sd" && claimName != "_sd_alg" && claimName != "...",
      s"Reserved claim name: $claimName"
    )
    ObjectPropertyDisclosure(salt = salt, claimName = claimName, claimValue = claimValue)

  /** Parse a base64url-encoded disclosure string.
    *
    * @param disclosureString
    *   Base64url-encoded disclosure
    * @return
    *   Parsed Disclosure instance (either ArrayElementDisclosure or ObjectPropertyDisclosure)
    * @throws IllegalArgumentException
    *   if the disclosure string is invalid or contains reserved keys
    */
  def parse(disclosureString: String): Disclosure =
    require(disclosureString != null && disclosureString.nonEmpty, "Disclosure string must not be empty")

    // Decode base64url
    val decoded = Base64(disclosureString).decodeToString

    // Parse JSON array
    decoded.fromJson[Json] match
      case Left(error) =>
        throw IllegalArgumentException(s"Invalid disclosure JSON: $error")

      case Right(Json.Arr(elements)) if elements.size == 2 =>
        // Array element: [salt, claimValue]
        elements match
          case Seq(Json.Str(salt), claimValue) =>
            arrayElement(claimValue, salt)
          case _ =>
            throw IllegalArgumentException("Invalid array element disclosure format")

      case Right(Json.Arr(elements)) if elements.size == 3 =>
        // Object property: [salt, claimName, claimValue]
        elements match
          case Seq(Json.Str(salt), Json.Str(name), claimValue) =>
            property(name, claimValue, salt)
          case _ =>
            throw IllegalArgumentException("Invalid object property disclosure format")

      case Right(_) =>
        throw IllegalArgumentException("Disclosure must be a JSON array with 2 or 3 elements")
