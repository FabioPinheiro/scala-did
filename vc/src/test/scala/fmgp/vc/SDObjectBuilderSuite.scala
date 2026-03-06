package fmgp.vc

import com.authlete.sd.Disclosure
import zio.json.ast.Json
import fmgp.vc.sdjwt.SDObjectBuilder
import munit.FunSuite

/** Test suite for SDObjectBuilder.
  *
  * Verifies correct behavior for building SD-JWT payload objects with selective disclosure, regular
  * claims, and decoy digests.
  */
class SDObjectBuilderSuite extends FunSuite:

  test("build with regular claims only") {
    val result = SDObjectBuilder.empty
      .withClaim("iss", Json.Str("issuer123"))
      .withClaim("sub", Json.Str("subject456"))
      .build()

    assertEquals(result.size, 2)
    assert(!result.contains("_sd"), "_sd should not be present when there are no SD claims")
    assertEquals(result("iss"), Json.Str("issuer123"))
    assertEquals(result("sub"), Json.Str("subject456"))
  }

  test("build with SD claims creates _sd array") {
    val disclosure = new Disclosure("name", "Alice")

    val result = SDObjectBuilder.empty
      .withClaim("iss", Json.Str("issuer123"))
      .withSDClaim(disclosure)
      .build()

    assertEquals(result.size, 2)
    assert(result.contains("_sd"), "_sd array should be present")
    result("_sd") match
      case Json.Arr(elements) => assertEquals(elements.size, 1)
      case _                  => fail("_sd should be an array")
  }

  test("withSDClaim removes from regular claims") {
    val disclosure = new Disclosure("name", "Alice")

    val result = SDObjectBuilder.empty
      .withClaim("name", Json.Str("Bob"))
      .withSDClaim(disclosure)
      .build()

    assert(!result.contains("name"), "name should be removed from regular claims")
    assert(result.contains("_sd"), "_sd array should be present")
  }

  test("withClaim removes from SD claims") {
    val disclosure = new Disclosure("name", "Alice")

    val builder = SDObjectBuilder.empty
      .withSDClaim(disclosure)

    // Verify SD claim was added
    assertEquals(builder.digestListBuilder.claimDigests.size, 1)

    // Now add as regular claim
    val result = builder
      .withClaim("name", Json.Str("Bob"))
      .build()

    assert(result.contains("name"), "name should be present as regular claim")
    assertEquals(result("name"), Json.Str("Bob"))

    // The _sd array should either not exist or be empty
    result.get("_sd") match
      case None => // OK, no _sd array
      case Some(Json.Arr(elements)) =>
        // If _sd exists, it should have 0 elements (name was removed)
        assertEquals(elements.size, 0)
      case _ => fail("_sd should be an array if present")
  }

  test("build with includeHashAlgorithm=true adds _sd_alg") {
    val disclosure = new Disclosure("name", "Alice")

    val result = SDObjectBuilder.empty
      .withSDClaim(disclosure)
      .build(includeHashAlgorithm = true)

    assert(result.contains("_sd_alg"), "_sd_alg should be present")
    assertEquals(result("_sd_alg"), Json.Str("sha-256"))
  }

  test("build without includeHashAlgorithm omits _sd_alg") {
    val disclosure = new Disclosure("name", "Alice")

    val result = SDObjectBuilder.empty
      .withSDClaim(disclosure)
      .build()

    assert(!result.contains("_sd_alg"), "_sd_alg should not be present by default")
  }

  test("toJavaMap converts to java.util.Map") {
    val result = SDObjectBuilder.empty
      .withClaim("iss", Json.Str("issuer123"))
      .toJavaMap()

    assert(result.isInstanceOf[java.util.Map[?, ?]])
    assertEquals(result.get("iss"), "issuer123")
  }

  test("toJavaMap with nested objects") {
    val cnfValue = Json.Obj(
      "jwk" -> Json.Obj(
        "kty" -> Json.Str("EC"),
        "crv" -> Json.Str("P-256")
      )
    )

    val result = SDObjectBuilder.empty
      .withClaim("cnf", cnfValue)
      .toJavaMap()

    val cnf = result.get("cnf").asInstanceOf[java.util.Map[String, Object]]
    assert(cnf != null)
    val jwk = cnf.get("jwk").asInstanceOf[java.util.Map[String, Object]]
    assertEquals(jwk.get("kty"), "EC")
  }

  test("toJavaMap with arrays") {
    val arrayValue = Json.Arr(Json.Str("value1"), Json.Str("value2"), Json.Str("value3"))

    val result = SDObjectBuilder.empty
      .withClaim("myArray", arrayValue)
      .toJavaMap()

    val arr = result.get("myArray").asInstanceOf[java.util.List[?]]
    assertEquals(arr.size(), 3)
    assertEquals(arr.get(0).asInstanceOf[String], "value1")
  }

  test("toJavaMap with numbers and booleans") {
    val result = SDObjectBuilder.empty
      .withClaim("age", Json.Num(30))
      .withClaim("active", Json.Bool(true))
      .toJavaMap()

    // Json.Num returns java.math.BigDecimal
    val age = result.get("age").asInstanceOf[java.math.BigDecimal]
    assertEquals(age.intValue(), 30)
    assertEquals(result.get("active"), java.lang.Boolean.TRUE)
  }

  test("immutability - original unchanged after withClaim") {
    val original = SDObjectBuilder.empty
    val modified = original.withClaim("iss", Json.Str("issuer123"))

    assertEquals(original.claims.size, 0, "Original builder should be unchanged")
    assertEquals(modified.claims.size, 1, "Modified builder should have one claim")
  }

  test("immutability - original unchanged after withSDClaim") {
    val disclosure = new Disclosure("name", "Alice")
    val original = SDObjectBuilder.empty
    val modified = original.withSDClaim(disclosure)

    assertEquals(original.digestListBuilder.claimDigests.size, 0, "Original builder should be unchanged")
    assertEquals(modified.digestListBuilder.claimDigests.size, 1, "Modified builder should have one SD claim")
  }

  test("withClaims - batch add multiple claims") {
    val claimsMap = Map(
      "iss" -> Json.Str("issuer123"),
      "sub" -> Json.Str("subject456"),
      "iat" -> Json.Num(1234567890)
    )

    val result = SDObjectBuilder.empty
      .withClaims(claimsMap)
      .build()

    assertEquals(result.size, 3)
    assertEquals(result("iss"), Json.Str("issuer123"))
    assertEquals(result("sub"), Json.Str("subject456"))
    assertEquals(result("iat"), Json.Num(1234567890))
  }

  test("withSDClaims - batch add multiple SD claims") {
    val disclosure1 = new Disclosure("name", "Alice")
    val disclosure2 = new Disclosure("age", Integer.valueOf(30))
    val disclosure3 = new Disclosure("email", "alice@example.com")

    val result = SDObjectBuilder.empty
      .withSDClaims(Seq(disclosure1, disclosure2, disclosure3))
      .build()

    result("_sd") match
      case Json.Arr(elements) => assertEquals(elements.size, 3)
      case _                  => fail("_sd should be an array with 3 elements")
  }

  test("withDecoyDigest adds decoy digests") {
    val disclosure = new Disclosure("name", "Alice")

    val result = SDObjectBuilder.empty
      .withSDClaim(disclosure)
      .withDecoyDigest()
      .build()

    result("_sd") match
      case Json.Arr(elements) => assertEquals(elements.size, 2, "Should have 1 real + 1 decoy = 2 total")
      case _                  => fail("_sd should be an array")
  }

  test("withDecoyDigests - batch add multiple decoys") {
    val disclosure = new Disclosure("name", "Alice")

    val result = SDObjectBuilder.empty
      .withSDClaim(disclosure)
      .withDecoyDigests(3)
      .build()

    result("_sd") match
      case Json.Arr(elements) => assertEquals(elements.size, 4, "Should have 1 real + 3 decoys = 4 total")
      case _                  => fail("_sd should be an array")
  }

  test("reserved claim names are rejected - _sd") {
    intercept[IllegalArgumentException] {
      SDObjectBuilder.empty.withClaim("_sd", Json.Str("invalid"))
    }
  }

  test("reserved claim names are rejected - _sd_alg") {
    intercept[IllegalArgumentException] {
      SDObjectBuilder.empty.withClaim("_sd_alg", Json.Str("invalid"))
    }
  }

  test("null claim name is rejected") {
    intercept[IllegalArgumentException] {
      SDObjectBuilder.empty.withClaim(null, Json.Str("value"))
    }
  }

  test("empty claim name is rejected") {
    intercept[IllegalArgumentException] {
      SDObjectBuilder.empty.withClaim("", Json.Str("value"))
    }
  }

  test("empty builder produces empty map") {
    val result = SDObjectBuilder.empty.build()
    assertEquals(result.size, 0)
  }

  test("chainable API") {
    val disclosure1 = new Disclosure("name", "Alice")
    val disclosure2 = new Disclosure("age", Integer.valueOf(30))

    val result = SDObjectBuilder.empty
      .withClaim("iss", Json.Str("issuer123"))
      .withSDClaim(disclosure1)
      .withDecoyDigest()
      .withClaim("sub", Json.Str("subject456"))
      .withSDClaim(disclosure2)
      .withDecoyDigests(2)
      .build()

    assertEquals(result.size, 3, "Should have iss, sub, and _sd")
    assert(result.contains("iss"))
    assert(result.contains("sub"))
    assert(result.contains("_sd"))

    result("_sd") match
      case Json.Arr(elements) => assertEquals(elements.size, 5, "Should have 2 real + 3 decoys = 5 total")
      case _                  => fail("_sd should be an array")
  }

  test("_sd array is sorted") {
    val disclosure1 = new Disclosure("zzz", "last")
    val disclosure2 = new Disclosure("aaa", "first")
    val disclosure3 = new Disclosure("mmm", "middle")

    val result = SDObjectBuilder.empty
      .withSDClaim(disclosure1)
      .withSDClaim(disclosure2)
      .withSDClaim(disclosure3)
      .build()

    result("_sd") match
      case Json.Arr(elements) =>
        val digests = elements.collect { case Json.Str(s) => s }
        val sorted = digests.sorted
        assertEquals(digests, sorted, "Digests should be sorted alphanumerically")
      case _ => fail("_sd should be an array")
  }

  test("no _sd array when only regular claims") {
    val result = SDObjectBuilder.empty
      .withClaim("iss", Json.Str("issuer123"))
      .withClaim("sub", Json.Str("subject456"))
      .build()

    assert(!result.contains("_sd"), "_sd should not be present")
  }

  test("no _sd array when all SD claims converted to regular") {
    val disclosure = new Disclosure("name", "Alice")

    val result = SDObjectBuilder.empty
      .withSDClaim(disclosure)
      .withClaim("name", Json.Str("Bob"))  // Convert SD to regular
      .build()

    assert(result.contains("name"))
    assert(!result.contains("_sd"), "_sd should not be present after conversion")
  }
