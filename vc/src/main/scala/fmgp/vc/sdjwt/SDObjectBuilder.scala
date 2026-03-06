package fmgp.vc.sdjwt

import com.authlete.sd.Disclosure
import zio.json.*
import zio.json.ast.Json
import scala.jdk.CollectionConverters.*

/** Immutable builder for constructing SD-JWT payload objects with selective disclosure.
  *
  * This is a Scala reimplementation of authlete's SDObjectBuilder using functional programming principles. All methods
  * return new instances, making this builder thread-safe and composable.
  *
  * The builder manages both regular claims (always visible) and SD claims (selectively disclosable). SD claims are
  * replaced with an `_sd` array containing disclosure digests, while regular claims remain in the payload as-is.
  *
  * Usage example: {{{ val payload = SDObjectBuilder.empty .withClaim("iss", Json.Str("did:prism:issuer123"))
  * .withClaim("sub", Json.Str("did:prism:subject456")) .withSDClaim(nameDisclosure) .withSDClaim(ageDisclosure)
  * .withDecoyDigests(3) .build() }}}
  *
  * @param claims
  *   Map of regular (non-SD) claim names to their JSON values
  * @param digestListBuilder
  *   Embedded DigestListBuilder for managing SD claim digests
  */
final case class SDObjectBuilder(
    claims: Map[String, Json] = Map.empty,
    digestListBuilder: DigestSHA256ListBuilder = DigestSHA256ListBuilder.empty
) {

  /** The hash algorithm to use for computing disclosure digests (default: "sha-256") */
  def hashAlgorithm: String = "sha-256"

  /** Add a regular (non-SD) claim to the payload.
    *
    * Regular claims are always visible in the final JWT payload. If a claim with the same name was previously added as
    * an SD claim, it will be converted to a regular claim (the digest will be removed).
    *
    * Reserved claim names `_sd` and `_sd_alg` cannot be used as they are managed internally by the builder.
    *
    * @param name
    *   The claim name (must be non-null, non-empty, and not reserved)
    * @param value
    *   The claim value as a zio-json AST node
    * @return
    *   A new SDObjectBuilder with the claim added
    * @throws IllegalArgumentException
    *   if the claim name is null, empty, or reserved
    */
  def withClaim(name: String, value: Json): SDObjectBuilder =
    require(name != null && name.nonEmpty, "Claim name must not be null or empty")
    require(name != "_sd" && name != "_sd_alg", s"Reserved claim name: $name")

    // If this claim was previously SD, remove from digest list
    val newDigestList =
      if digestListBuilder.claimDigests.contains(name) then
        digestListBuilder.copy(claimDigests = digestListBuilder.claimDigests - name)
      else digestListBuilder

    copy(
      claims = claims + (name -> value),
      digestListBuilder = newDigestList
    )

  /** Add multiple regular claims to the payload.
    *
    * This is a convenience method for adding multiple claims at once using a fold operation.
    *
    * @param claimsToAdd
    *   Map of claim names to their JSON values
    * @return
    *   A new SDObjectBuilder with all claims added
    */
  def withClaims(claimsToAdd: Map[String, Json]): SDObjectBuilder =
    claimsToAdd.foldLeft(this) { case (builder, (name, value)) =>
      builder.withClaim(name, value)
    }

  /** Add an SD (selectively disclosable) claim to the payload.
    *
    * SD claims are not included directly in the payload. Instead, their digest is added to the `_sd` array. The actual
    * claim value must be disclosed separately via the disclosure mechanism.
    *
    * If a claim with the same name was previously added as a regular claim, it will be converted to an SD claim (the
    * value will be removed from the payload).
    *
    * @param disclosure
    *   The authlete Disclosure object containing the claim to make selectively disclosable
    * @return
    *   A new SDObjectBuilder with the SD claim added
    */
  def withSDClaim(disclosure: Disclosure): SDObjectBuilder =
    val claimName = disclosure.getClaimName()

    // Remove from regular claims if present
    val newClaims = if claimName != null then claims - claimName else claims

    // Add to digest list
    val newDigestList = digestListBuilder.withDisclosureDigest(disclosure)

    copy(claims = newClaims, digestListBuilder = newDigestList)

  /** Add multiple SD claims to the payload.
    *
    * This is a convenience method for adding multiple SD claims at once using a fold operation.
    *
    * @param disclosures
    *   Sequence of Disclosure objects to make selectively disclosable
    * @return
    *   A new SDObjectBuilder with all SD claims added
    */
  def withSDClaims(disclosures: Seq[Disclosure]): SDObjectBuilder =
    disclosures.foldLeft(this)((builder, disclosure) => builder.withSDClaim(disclosure))

  /** Add a single random decoy digest to the `_sd` array.
    *
    * Decoy digests are random values that provide privacy by making it harder to determine how many claims are actually
    * selectively disclosable.
    *
    * @return
    *   A new SDObjectBuilder with one additional random decoy digest
    */
  def withDecoyDigest(): SDObjectBuilder =
    copy(digestListBuilder = digestListBuilder.withDecoyDigest())

  /** Add multiple random decoy digests to the `_sd` array.
    *
    * This is a convenience method that calls `withDecoyDigest()` multiple times.
    *
    * @param count
    *   The number of random decoy digests to add
    * @return
    *   A new SDObjectBuilder with the specified number of additional random decoy digests
    */
  def withDecoyDigests(count: Int): SDObjectBuilder =
    copy(digestListBuilder = digestListBuilder.withDecoyDigests(count))

  /** Build the final SD-JWT payload without the `_sd_alg` claim.
    *
    * Creates a payload containing:
    *   - All regular claims
    *   - An `_sd` array with sorted disclosure digests (if any SD claims were added)
    *
    * The `_sd_alg` claim is omitted (use `build(includeHashAlgorithm = true)` to include it).
    *
    * @return
    *   A Map of claim names to JSON values representing the complete payload
    */
  def build(): Map[String, Json] =
    build(includeHashAlgorithm = false)

  /** Build the final SD-JWT payload with optional `_sd_alg` claim.
    *
    * Creates a payload containing:
    *   - All regular claims
    *   - An `_sd` array with sorted disclosure digests (if any SD claims were added)
    *   - Optionally, an `_sd_alg` claim specifying the hash algorithm used
    *
    * Per the SD-JWT specification, the `_sd_alg` claim can be omitted if the default algorithm ("sha-256") is used.
    *
    * @param includeHashAlgorithm
    *   Whether to include the `_sd_alg` claim in the payload
    * @return
    *   A Map of claim names to JSON values representing the complete payload
    */
  def build(includeHashAlgorithm: Boolean): Map[String, Json] =
    val digestSeq = digestListBuilder.build()

    val sdClaim =
      if digestSeq.isEmpty then Map.empty[String, Json]
      else Map("_sd" -> Json.Arr(digestSeq.map(Json.Str(_))*))

    val algClaim =
      if includeHashAlgorithm then Map("_sd_alg" -> Json.Str(hashAlgorithm))
      else Map.empty[String, Json]

    claims ++ sdClaim ++ algClaim

  /** Convert the payload to a Java Map for compatibility with authlete library.
    *
    * This method builds the payload and converts it from zio-json's AST to a Java `LinkedHashMap[String, Object]`,
    * preserving insertion order. This is useful for interop with authlete's Java-based SD-JWT library.
    *
    * @param includeHashAlgorithm
    *   Whether to include the `_sd_alg` claim in the payload
    * @return
    *   A Java LinkedHashMap representing the complete payload
    */
  def toJavaMap(includeHashAlgorithm: Boolean = false): java.util.Map[String, Object] =
    val jsonMap = build(includeHashAlgorithm)
    convertJsonToJavaMap(jsonMap)

  private def convertJsonToJavaMap(map: Map[String, Json]): java.util.LinkedHashMap[String, Object] =
    val javaMap = new java.util.LinkedHashMap[String, Object]()
    map.foreach { case (key, value) =>
      javaMap.put(key, jsonToJavaObject(value))
    }
    javaMap

  private def jsonToJavaObject(json: Json): Object =
    json match
      case Json.Obj(fields) =>
        val javaMap = new java.util.LinkedHashMap[String, Object]()
        fields.foreach { case (k, v) => javaMap.put(k, jsonToJavaObject(v)) }
        javaMap
      case Json.Arr(elements) =>
        elements.map(jsonToJavaObject).asJava
      case Json.Str(value)  => value
      case Json.Num(value)  => value
      case Json.Bool(value) => java.lang.Boolean.valueOf(value)
      case Json.Null        => null
}

object SDObjectBuilder {

  /** An empty SDObjectBuilder with default hash algorithm.
    *
    * Use this as a starting point for building an SD-JWT payload: {{{ SDObjectBuilder.empty.withClaim("iss",
    * Json.Str("issuer123")) }}}
    */
  val empty: SDObjectBuilder = SDObjectBuilder()
}
