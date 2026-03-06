package fmgp.vc.sdjwt

import com.authlete.sd.Disclosure
import scala.util.Random

/** Immutable builder for constructing SD-JWT `_sd` arrays containing digest values.
  *
  * This is a Scala reimplementation of authlete's DigestSHA256ListBuilder using functional programming principles. All
  * methods return new instances, making this builder thread-safe and composable.
  *
  * The `_sd` array in SD-JWT contains disclosure digests (for selectively-disclosable claims) and optionally decoy
  * digests (random values to protect privacy). Per the SD-JWT specification, the final digest list must be sorted
  * alphanumerically.
  *
  * Usage example: {{{ val digestList = DigestSHA256ListBuilder.empty .withDisclosureDigest(nameDisclosure)
  * .withDisclosureDigest(ageDisclosure) .withDecoyDigests(2) .build() }}}
  *
  * @param claimDigests
  *   Map of claim names to their disclosure digests
  * @param decoyDigests
  *   Set of random decoy digests for privacy
  */
final case class DigestSHA256ListBuilder(
    claimDigests: Map[String, Digest] = Map.empty,
    decoyDigests: Set[Digest] = Set.empty
):

  /** Add a disclosure digest to the builder.
    *
    * For named claims (with non-null claim name), the digest is added to the claim digests map. If a digest already
    * exists for this claim name, it will be overwritten.
    *
    * For array elements (with null claim name), the digest is added to the decoy digests set per the SD-JWT
    * specification.
    *
    * @param disclosure
    *   The authlete Disclosure object to compute a digest for
    * @return
    *   A new DigestSHA256ListBuilder with the disclosure digest added
    */
  def withDisclosureDigest(disclosure: Disclosure): DigestSHA256ListBuilder =
    val digest = DigestSHA256.fromDisclosure(disclosure)
    val claimName = disclosure.getClaimName()

    if claimName != null then
      // Named claim - add to claimDigests map
      copy(claimDigests = claimDigests + (claimName -> digest))
    else
      // Array element - add to decoy digests (per spec)
      copy(decoyDigests = decoyDigests + digest)

  /** Add multiple disclosure digests to the builder.
    *
    * This is a convenience method for adding multiple disclosures at once using a fold operation.
    *
    * @param disclosures
    *   Sequence of Disclosure objects to add
    * @return
    *   A new DigestSHA256ListBuilder with all disclosure digests added
    */
  def withDisclosureDigests(disclosures: Seq[Disclosure]): DigestSHA256ListBuilder =
    disclosures.foldLeft(this)((builder, disclosure) => builder.withDisclosureDigest(disclosure))

  /** Add a single random decoy digest.
    *
    * Generates a 32-byte random value and adds it as a decoy digest. Decoy digests are used to protect privacy by
    * making it harder to determine which claims are actually disclosed.
    *
    * @return
    *   A new DigestSHA256ListBuilder with one additional random decoy digest
    */
  def withDecoyDigest(): DigestSHA256ListBuilder =
    val randomBytes = new Array[Byte](32)
    Random.nextBytes(randomBytes)
    val decoyDigest = DigestSHA256.fromBytes(randomBytes)
    copy(decoyDigests = decoyDigests + decoyDigest)

  /** Add multiple random decoy digests.
    *
    * Generates the specified number of random decoy digests. This is a convenience method that calls
    * `withDecoyDigest()` multiple times.
    *
    * @param count
    *   The number of random decoy digests to add
    * @return
    *   A new DigestSHA256ListBuilder with the specified number of additional random decoy digests
    */
  def withDecoyDigests(count: Int): DigestSHA256ListBuilder =
    (0 until count).foldLeft(this)((builder, _) => builder.withDecoyDigest())

  /** Build the final sorted digest list.
    *
    * Combines all claim digests and decoy digests into a single sequence, then sorts them alphanumerically as required
    * by the SD-JWT specification. The sorting ensures that the original order of claims cannot be inferred from the
    * digest positions.
    *
    * @return
    *   An immutable Seq of digest strings sorted in alphanumeric order
    */
  def build(): Seq[String] =
    val allDigests = claimDigests.values.map(_.value) ++ decoyDigests.map(_.value)
    allDigests.toSeq.sorted

object DigestSHA256ListBuilder:

  /** An empty DigestSHA256ListBuilder with default hash algorithm.
    *
    * Use this as a starting point for building a digest list: {{{
    * DigestSHA256ListBuilder.empty.withDisclosureDigest(disclosure) }}}
    */
  val empty: DigestSHA256ListBuilder = DigestSHA256ListBuilder()
