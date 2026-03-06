package fmgp.vc

import com.authlete.sd.Disclosure
import fmgp.vc.sdjwt.DigestSHA256ListBuilder
import munit.FunSuite

/** Test suite for DigestSHA256ListBuilder using test vectors from authlete's DigestListBuilderTest.java.
  *
  * These tests verify compatibility with the authlete implementation and ensure correct behavior per
  * the SD-JWT specification.
  */
class DigestListBuilderSuite extends FunSuite:

  test("test_01_1_disclosure - matches authlete output") {
    // Test vector from authlete: single disclosure with known digest
    val disclosure = Disclosure.parse("WyI2cU1RdlJMNWhhaiIsICJmYW1pbHlfbmFtZSIsICJNw7ZiaXVzIl0")

    val result = DigestSHA256ListBuilder.empty
      .withDisclosureDigest(disclosure)
      .build()

    assertEquals(result.size, 1)
    // Expected digest from authlete test
    assertEquals(result.head, "uutlBuYeMDyjLLTpf6Jxi7yNkEF35jdyWMn9U7b_RYY")
  }

  test("test_02_4_disclosures - sorted output") {
    // Test vector from authlete: four address-related disclosures
    val street = Disclosure.parse("WyI0d3dqUzlyMm4tblBxdzNpTHR0TkFBIiwgInN0cmVldF9hZGRyZXNzIiwgIlNjaHVsc3RyLiAxMiJd")
    val locality = Disclosure.parse("WyJXcEtIQmVTa3A5U2MyNVV4a1F1RmNRIiwgImxvY2FsaXR5IiwgIlNjaHVscGZvcnRhIl0")
    val region = Disclosure.parse("WyIzSl9xWGctdUwxYzdtN1FoT0hUNTJnIiwgInJlZ2lvbiIsICJTYWNoc2VuLUFuaGFsdCJd")
    val country = Disclosure.parse("WyIwN2U3bWY2YWpTUDJjZkQ3NmJCZE93IiwgImNvdW50cnkiLCAiREUiXQ")

    val result = DigestSHA256ListBuilder.empty
      .withDisclosureDigest(street)
      .withDisclosureDigest(locality)
      .withDisclosureDigest(region)
      .withDisclosureDigest(country)
      .build()

    assertEquals(result.size, 4)

    // Verify digests are sorted alphanumerically (per SD-JWT spec)
    // The result should be sorted in ascending lexicographic order
    val sorted = result.sorted
    assertEquals(result, sorted, "Digests should be sorted alphanumerically")

    // Additional check: first digest should start with a lower alphanumeric character
    // This is a sanity check that sorting actually occurred
    assert(
      result.head <= result(1) && result(1) <= result(2) && result(2) <= result.last,
      "Digests should be in ascending order"
    )
  }

  test("test_03_1_disclosure_2_decoys - total count") {
    // Test vector from authlete: one real disclosure plus two decoy digests
    val disclosure = Disclosure.parse("WyI2cU1RdlJMNWhhaiIsICJmYW1pbHlfbmFtZSIsICJNw7ZiaXVzIl0")

    val result = DigestSHA256ListBuilder.empty
      .withDisclosureDigest(disclosure)
      .withDecoyDigests(2)
      .build()

    // Should have 3 total digests: 1 real + 2 decoys
    assertEquals(result.size, 3)

    // Verify all digests are non-empty strings
    result.foreach(digest => assert(digest.nonEmpty, "Digests should not be empty"))

    // Verify digests are URL-safe base64 (should not contain +, /, or =)
    result.foreach { digest =>
      assert(!digest.contains('+'), "Digest should not contain '+'")
      assert(!digest.contains('/'), "Digest should not contain '/'")
      assert(!digest.contains('='), "Digest should not contain '='")
    }
  }

  test("immutability - original unchanged after withDisclosureDigest") {
    // Verify that the builder is immutable and methods return new instances
    val disclosure = new Disclosure("name", "Alice")
    val original = DigestSHA256ListBuilder.empty
    val modified = original.withDisclosureDigest(disclosure)

    assertEquals(original.claimDigests.size, 0, "Original builder should be unchanged")
    assertEquals(modified.claimDigests.size, 1, "Modified builder should have one digest")
  }

  test("immutability - original unchanged after withDecoyDigest") {
    // Verify that adding decoy digests returns new instance
    val original = DigestSHA256ListBuilder.empty
    val modified = original.withDecoyDigest()

    assertEquals(original.decoyDigests.size, 0, "Original builder should be unchanged")
    assertEquals(modified.decoyDigests.size, 1, "Modified builder should have one decoy digest")
  }

  test("withDisclosureDigests - batch add multiple disclosures") {
    // Test convenience method for adding multiple disclosures at once
    val disclosure1 = new Disclosure("name", "Alice")
    val disclosure2 = new Disclosure("age", Integer.valueOf(30))
    val disclosure3 = new Disclosure("email", "alice@example.com")

    val result = DigestSHA256ListBuilder.empty
      .withDisclosureDigests(Seq(disclosure1, disclosure2, disclosure3))
      .build()

    assertEquals(result.size, 3)
  }

  test("withDecoyDigests - batch add multiple decoys") {
    // Test convenience method for adding multiple decoy digests
    val result = DigestSHA256ListBuilder.empty
      .withDecoyDigests(5)
      .build()

    assertEquals(result.size, 5)
  }

  test("overwrite behavior - same claim name replaces digest") {
    // Verify that adding a disclosure for the same claim name overwrites the previous digest
    val disclosure1 = new Disclosure("name", "Alice")
    val disclosure2 = new Disclosure("name", "Bob")

    val result = DigestSHA256ListBuilder.empty
      .withDisclosureDigest(disclosure1)
      .withDisclosureDigest(disclosure2)
      .build()

    // Should only have 1 digest (the second one), not 2
    assertEquals(result.size, 1)
  }

  test("empty builder produces empty list") {
    // Verify that building without adding any digests produces an empty list
    val result = DigestSHA256ListBuilder.empty.build()
    assertEquals(result.size, 0)
  }

  test("chainable API") {
    // Verify that methods can be chained fluently
    val disclosure1 = new Disclosure("name", "Alice")
    val disclosure2 = new Disclosure("age", Integer.valueOf(30))

    val result = DigestSHA256ListBuilder.empty
      .withDisclosureDigest(disclosure1)
      .withDecoyDigest()
      .withDisclosureDigest(disclosure2)
      .withDecoyDigests(2)
      .build()

    // Should have 2 claim digests + 3 decoy digests = 5 total
    assertEquals(result.size, 5)
  }

  test("array element disclosures - null claim name") {
    // Test handling of array element disclosures (claim name is null)
    val arrayElementDisclosure = new Disclosure("element value")

    val result = DigestSHA256ListBuilder.empty
      .withDisclosureDigest(arrayElementDisclosure)
      .build()

    assertEquals(result.size, 1)
    // Array elements should be treated as decoy digests per spec
  }
