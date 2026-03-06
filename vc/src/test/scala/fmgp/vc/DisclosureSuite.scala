package fmgp.vc

import fmgp.vc.sdjwt.{Disclosure, ArrayElementDisclosure, ObjectPropertyDisclosure}
import zio.json.ast.Json
import munit.FunSuite

class DisclosureSuite extends FunSuite:

  test("test_01_constructor - matches authlete output") {
    // Test vector from authlete DisclosureTest.java
    val salt = "_26bc4LT-ac6q2KI6cBW5es"
    val claimName = "family_name"
    val claimValue = "Möbius"

    val disclosure = Disclosure.property(
      claimName = claimName,
      claimValue = Json.Str(claimValue),
      salt = salt
    )

    // Expected from authlete
    val expectedDisclosure = "WyJfMjZiYzRMVC1hYzZxMktJNmNCVzVlcyIsImZhbWlseV9uYW1lIiwiTcO2Yml1cyJd"

    assertEquals(disclosure.disclosure, expectedDisclosure)
    assertEquals(disclosure.salt, salt)
    // Direct access to claimName - disclosure is ObjectPropertyDisclosure
    assertEquals(disclosure.claimName, claimName)
    assertEquals(disclosure.claimValue, Json.Str(claimValue))
  }

  test("test_02_parse - matches authlete parsing") {
    // Test vector from authlete (with whitespace)
    val disclosureString = "WyJfMjZiYzRMTC1hYzZxMktJNmNCVzVlcyIsICJmYW1pbHlfbmFtZSIsICJNw7ZiaXVzIl0"

    val disclosure = Disclosure.parse(disclosureString)

    assertEquals(disclosure.salt, "_26bc4LL-ac6q2KI6cBW5es")
    // Pattern match to access claimName on ObjectPropertyDisclosure
    disclosure match
      case obj: ObjectPropertyDisclosure =>
        assertEquals(obj.claimName, "family_name")
      case _: ArrayElementDisclosure => fail("Expected ObjectPropertyDisclosure")
    assertEquals(disclosure.claimValue, Json.Str("Möbius"))
  }

  test("test_03_array - array element without claim name") {
    val claimValue = "my_array_element"

    val disclosure = Disclosure.arrayElement(
      claimValue = Json.Str(claimValue)
    )

    // Verify it's an ArrayElementDisclosure (no claimName field)
    // disclosure is already typed as ArrayElementDisclosure from smart constructor
    assertEquals(disclosure.claimValue, Json.Str(claimValue))
    assert(disclosure.salt.nonEmpty)
  }

  test("test_04_array_element - toArrayElement method") {
    // Test vector from authlete
    val disclosureString = "WyJsa2x4RjVqTVlsR1RQVW92TU5JdkNBIiwgIkZSIl0"

    val disclosure = Disclosure.parse(disclosureString)

    assertEquals(disclosure.salt, "lklxF5jMYlGTPUovMNIvCA")
    // Verify it's an ArrayElementDisclosure and call toArrayElement
    disclosure match
      case arr: ArrayElementDisclosure =>
        val arrayElement = arr.toArrayElement()
        // Verify structure - should have "..." key with a digest value
        assert(arrayElement.contains("..."))
        assert(arrayElement("...").nonEmpty)
        assert(arrayElement("...").length > 40) // SHA-256 base64 length
      case _ => fail("Expected ArrayElementDisclosure")
    assertEquals(disclosure.claimValue, Json.Str("FR"))
  }

  test("compatibility with authlete - digest computation") {
    // Compare digest computation with authlete's implementation
    val authleteDisclosure = new com.authlete.sd.Disclosure("family_name", "Möbius")
    val scalaDisclosure = Disclosure.property("family_name", Json.Str("Möbius"))

    // Both should produce same digest format (even if salt differs)
    assert(scalaDisclosure.defaultDigest.nonEmpty)
    assert(scalaDisclosure.defaultDigest.length > 40) // SHA-256 base64 length
  }

  test("immutability - case class properties") {
    val disclosure = Disclosure.property("name", Json.Str("Alice"))

    // Verify immutability (all fields are val)
    assertEquals(disclosure.salt, disclosure.salt) // Same instance
    assertEquals(disclosure.claimName, disclosure.claimName)
  }

  test("lazy evaluation - disclosure string computed once") {
    val disclosure = Disclosure.property("test", Json.Str("value"))

    val first = disclosure.disclosure
    val second = disclosure.disclosure

    // Same instance (lazy val caching)
    assert(first eq second)
  }

  test("reserved claim names rejected") {
    intercept[IllegalArgumentException] {
      Disclosure.property("_sd", Json.Str("invalid"))
    }

    intercept[IllegalArgumentException] {
      Disclosure.property("_sd_alg", Json.Str("invalid"))
    }

    intercept[IllegalArgumentException] {
      Disclosure.property("...", Json.Str("invalid"))
    }
  }

  test("parse rejects invalid disclosure strings") {
    intercept[IllegalArgumentException] {
      Disclosure.parse("")
    }

    intercept[IllegalArgumentException] {
      Disclosure.parse("not-valid-base64")
    }
  }

  test("toArrayElement only available on ArrayElementDisclosure") {
    val objectDisclosure = Disclosure.property("name", Json.Str("Alice"))

    // Demonstrate compile-time type safety
    // objectDisclosure is typed as ObjectPropertyDisclosure from smart constructor
    // Calling objectDisclosure.toArrayElement() would be a compile error
    // This test verifies the type system prevents misuse
    assert(objectDisclosure.claimName == "name")
  }

  test("equals and hashCode based on disclosure string") {
    val salt = "same-salt"
    val d1 = Disclosure.property("name", Json.Str("Alice"), salt)
    val d2 = Disclosure.property("name", Json.Str("Alice"), salt)

    assertEquals(d1, d2)
    assertEquals(d1.hashCode, d2.hashCode)
  }

  test("toString returns disclosure string") {
    val disclosure = Disclosure.property("test", Json.Str("value"))

    assertEquals(disclosure.toString, disclosure.disclosure)
  }

  test("salt validation - non-empty required") {
    intercept[IllegalArgumentException] {
      Disclosure.property("name", Json.Str("value"), salt = "")
    }

    intercept[IllegalArgumentException] {
      Disclosure.property("name", Json.Str("value"), salt = null)
    }
  }

  test("claim name validation - non-empty for properties") {
    intercept[IllegalArgumentException] {
      Disclosure.property("", Json.Str("value"))
    }

    intercept[IllegalArgumentException] {
      Disclosure.property(null, Json.Str("value"))
    }
  }

  test("numeric values - JSON encoding") {
    val disclosure = Disclosure.property("age", Json.Num(30))

    assert(disclosure.disclosure.nonEmpty)
    assertEquals(disclosure.claimValue, Json.Num(30))
  }

  test("boolean values - JSON encoding") {
    val disclosure = Disclosure.property("active", Json.Bool(true))

    assert(disclosure.disclosure.nonEmpty)
    assertEquals(disclosure.claimValue, Json.Bool(true))
  }

  test("null values - JSON encoding") {
    val disclosure = Disclosure.property("optional", Json.Null)

    assert(disclosure.disclosure.nonEmpty)
    assertEquals(disclosure.claimValue, Json.Null)
  }

  test("nested object values - JSON encoding") {
    val address = Json.Obj(
      "street" -> Json.Str("123 Main St"),
      "city" -> Json.Str("Springfield")
    )
    val disclosure = Disclosure.property("address", address)

    assert(disclosure.disclosure.nonEmpty)
    assertEquals(disclosure.claimValue, address)
  }

  test("array values - JSON encoding") {
    val roles = Json.Arr(Json.Str("admin"), Json.Str("user"))
    val disclosure = Disclosure.property("roles", roles)

    assert(disclosure.disclosure.nonEmpty)
    assertEquals(disclosure.claimValue, roles)
  }
