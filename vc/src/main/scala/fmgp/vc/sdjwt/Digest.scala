package fmgp.vc.sdjwt

import fmgp.util.Base64
import fmgp.crypto.SHA256

type Digest = DigestSHA256

/** Opaque type for SD-JWT disclosure digests.
  *
  * Provides type safety to prevent mixing digest values with regular strings at compile time. Uses URL-safe base64
  * encoding without padding as specified in SD-JWT specification.
  */
opaque type DigestSHA256 = String

object DigestSHA256:

  /** Create a Digest from a raw string value.
    *
    * Note: This should only be used when you already have a properly formatted digest string. For computing digests
    * from data, use `fromBytes`, `fromString`, or `fromDisclosure`.
    */
  def apply(value: String): DigestSHA256 = value

  extension (d: DigestSHA256)
    /** Extract the underlying string value from a Digest.
      *
      * @return
      *   The digest as a URL-safe base64 string without padding
      */
    def value: String = d

  /** Compute a digest from raw bytes.
    *
    * @param bytes
    *   The raw bytes to encode. The bytes are encoded as URL-safe base64 without padding as required by the SD-JWT
    *   specification.
    */
  def fromBytes(bytes: Array[Byte]): DigestSHA256 =
    Base64.encode(bytes).urlBase64WithoutPadding

  /** Compute a digest from a string using the specified hash algorithm.
    *
    * @param str
    *   The string to hash
    * @throws IllegalArgumentException
    *   if an unsupported hash algorithm is specified
    */
  def fromString(str: String): DigestSHA256 = fromBytes(SHA256.digest(str))

  /** Compute a digest from an authlete Disclosure object.
    *
    * This method provides interoperability with the authlete sd-jwt library. It computes the digest of the disclosure's
    * serialized form.
    *
    * @param disclosure
    *   The authlete Disclosure object
    */
  def fromDisclosure(disclosure: com.authlete.sd.Disclosure): DigestSHA256 =
    fromString(disclosure.getDisclosure())
