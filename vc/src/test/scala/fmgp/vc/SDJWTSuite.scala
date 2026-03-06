package fmgp.vc

import com.authlete.sd.{Disclosure, SDObjectBuilder, SDJWT}
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import java.util.{LinkedHashMap, UUID}
import scala.jdk.CollectionConverters.*

import zio.json.*

import fmgp.crypto.{given, *}
import fmgp.crypto.UtilsJVM.toJWK
import fmgp.did.method.prism.cardano.{CardanoWalletConfig, ed25519DerivePrism}
import fmgp.did.method.prism.proto.PrismKeyUsage
import fmgp.did.method.prism.{publicJWK, signWithExtendedKey}
import fmgp.did.comm.MediaTypes

// testOnly fmgp.vc.SDJWTSuite
class SDJWTSuite extends munit.FunSuite {

  // Step 1: Create wallet from default BIP39 mnemonic for deterministic key derivation
  // mnemonic create with -> println(com.bloxbean.cardano.client.account.Account.apply().mnemonic())
  val issuerWallet = CardanoWalletConfig
    .fromMnemonicPhrase(
      "year degree tiger isolate notice barely indicate journey female citizen general begin speak brass abandon appear owner planet giraffe document syrup just final parent"
    )
    .getOrElse(???)
  val holderWallet = CardanoWalletConfig
    .fromMnemonicPhrase(
      "cover fork pen sight double sword evil seek elbow spatial amount cinnamon deny dilemma aunt nerve mountain odor rabbit view pear snake physical garlic"
    )
    .getOrElse(???)

  // Generate KEYS Ed25519
  val issuerHdKeyPair = issuerWallet.ed25519DerivePrism(0, PrismKeyUsage.AssertionMethodKeyUsage, 0)
  val issuerEd25519KeyOKP = issuerHdKeyPair.publicJWK.withoutKid.withKid(kid = "issuer-key-Ed25519")

  val holderHdKeyPair = holderWallet.ed25519DerivePrism(0, PrismKeyUsage.AssertionMethodKeyUsage, 0)
  val holderEd25519KeyOKP = holderHdKeyPair.publicJWK.withoutKid.withKid(kid = "holder-key-Ed25519")
  val holderJWT = holderEd25519KeyOKP.toJWK

  assertEquals(issuerEd25519KeyOKP.jwsAlgorithmto.symbol, JWSAlgorithm.EdDSA.getName())
  assertEquals(holderEd25519KeyOKP.jwsAlgorithmto.symbol, JWSAlgorithm.EdDSA.getName())

  // Disclosures as class-level vals so the same instances (with the same salt) are shared across all tests
  val uniqueIDDisclosure = new Disclosure("uniqueID", Integer.valueOf(9854321))
  val cardIDDisclosure = new Disclosure("cardID", Integer.valueOf(123456789))
  val countryDisclosure = new Disclosure("country", "PT")
  val firstNameDisclosure = new Disclosure("firstName", "Jon")
  val lastNameDisclosure = new Disclosure("lastName", "Doe")
  val allDisclosures =
    Seq(uniqueIDDisclosure, cardIDDisclosure, countryDisclosure, firstNameDisclosure, lastNameDisclosure)

  // Shared state between tests
  var vcSDJWT: SDJWT = scala.compiletime.uninitialized
  var vpSDJWT: SDJWT = scala.compiletime.uninitialized
  var selectedDisclosures: Seq[Disclosure] = scala.compiletime.uninitialized
  var bindingJwt: JWT = scala.compiletime.uninitialized
  var credentialJwt: JWT = scala.compiletime.uninitialized

  // https://github.com/authlete/sd-jwt/blob/main/src/test/java/com/authlete/sd/VerificationTest.java
  test("Issuer generates the VC") {

    // Step 1: Define the claims for the credential
    val sub = "did:prism:3eb2e85f1f87a31078cbcaa64d33e224890b31ef424789ec2627eb0b54850565"
    val iss = "did:prism:06e872079923651c3e6eb989176bac6529a2f671b2fecf8905455ff4315c089e"
    val iat = System.currentTimeMillis() / 1000
    val exp = iat + 86400 // 24 hours
    val vct = "https://credentials.example.com/identity-card/v1"

    // Step 2: Build the SD-JWT payload using SDObjectBuilder
    val builder = new SDObjectBuilder()
    builder.putClaim("sub", sub)
    builder.putClaim("iss", iss)
    builder.putClaim("iat", Long.box(iat))
    builder.putClaim("exp", Long.box(exp))
    builder.putClaim("vct", vct)

    // Add cnf claim with holder's public key for holder binding
    val cnfMap = new java.util.HashMap[String, Object]()
    cnfMap.put("jwk", holderJWT.toPublicJWK().toJSONObject()) // FIXME make it type safe
    builder.putClaim("cnf", cnfMap)

    allDisclosures.foreach(builder.putSDClaim)

    val credentialClaims: Map[String, Object] = builder.build().asScala.toMap

    // Step 3: Sign credential JWT with issuer key
    val credentialClaimsSet = JWTClaimsSet.parse(credentialClaims.asJava)
    val jwtTmp = JWTUnsigned(
      header = JWTHeader(
        alg = issuerEd25519KeyOKP.jwsAlgorithmto,
        kid = Some(issuerEd25519KeyOKP.kid),
        typ = Some(MediaTypes.DIGITAL_CREDENTIAL_SDJWT.subType),
      ),
      payload = Payload.fromBytes(credentialClaimsSet.toPayload().toBytes())
    )
    credentialJwt = jwtTmp.signWithExtendedKey(issuerHdKeyPair).getOrElse(???)
    // println(s"Signed credential JWT: ${credentialJwt.base64JWTFormat}")

    // Step 4: Wrap in SDJWT with all disclosures
    vcSDJWT = new SDJWT(credentialJwt.base64JWTFormat, allDisclosures.asJava)
    // println(s"Created VC with ${allDisclosures.size} disclosures")
  }

  test("Holder verifies the VC") {
    // Verify issuer signature on the credential JWT
    assert(credentialJwt.verifyWith(issuerEd25519KeyOKP), "Credential JWT signature should be valid")
    println("Credential JWT signature verified with issuer's public key")

    // Check cnf claim is present (holder binding)
    val parsedCredentialJWT = SignedJWT.parse(vcSDJWT.getCredentialJwt())
    val credentialClaimsFromJWT = parsedCredentialJWT.getJWTClaimsSet()
    assert(credentialClaimsFromJWT.getClaim("cnf") != null, "Credential JWT should have 'cnf' claim")
    println("Holder binding (cnf) claim present in credential")
  }

  test("Holder generates the Presentation and binds to the Verifier") {
    // Step 9: Select subset of disclosures for presentation (selective disclosure)
    selectedDisclosures = Seq(uniqueIDDisclosure) // Only reveal uniqueID
    // println(s"Creating VP with ${selectedDisclosures.size} selected disclosure(s)")

    // Step 10: Compute SD hash from credential + selected disclosures
    val vpForHash = new SDJWT(credentialJwt.base64JWTFormat, selectedDisclosures.asJava)
    val sdHash = vpForHash.getSDHash()
    // println(s"Computed SD hash: $sdHash")

    // Step 11: Build binding JWT payload with iat, aud, nonce, sd_hash
    val nonce = UUID.randomUUID().toString
    // println(s"Binding JWT nonce: $nonce")
    val bindingPayload = new LinkedHashMap[String, Object]()
    bindingPayload.put("iat", Long.box(System.currentTimeMillis() / 1000))
    bindingPayload.put("aud", java.util.List.of("https://verifier.example.com"))
    bindingPayload.put("nonce", nonce)
    bindingPayload.put("sd_hash", sdHash)

    // Step 12: Sign binding JWT with holder key
    val bindingJwtTmp = JWTUnsigned(
      header = JWTHeader(
        alg = holderEd25519KeyOKP.jwsAlgorithmto,
        kid = Some(holderEd25519KeyOKP.kid),
        typ = Some(MediaTypes.BINDING_JWT.subType),
      ),
      payload = Payload.fromBytes(JWTClaimsSet.parse(bindingPayload).toPayload().toBytes())
    )
    bindingJwt = bindingJwtTmp.signWithExtendedKey(holderHdKeyPair).getOrElse(???)
    // println(s"Signed binding JWT: ${bindingJwt.base64JWTFormat}")

    // Step 13: Assemble complete verifiable presentation
    vpSDJWT = new SDJWT(credentialJwt.base64JWTFormat, selectedDisclosures.asJava, bindingJwt.base64JWTFormat)
    // println(s"VP assembled with ${selectedDisclosures.size} disclosure(s)")
  }

  test("Verifier verifies the Presentation") {
    // Step 14: Verify credential JWT signature with issuer's public key
    assert(credentialJwt.verifyWith(issuerEd25519KeyOKP), "Credential JWT signature should be valid")
    println("Credential JWT signature verified with issuer's public key")

    // Step 15: Extract binding key from cnf claim
    val parsedCredentialJWT = SignedJWT.parse(vpSDJWT.getCredentialJwt())
    val credentialClaimsFromJWT = parsedCredentialJWT.getJWTClaimsSet()
    val cnf = credentialClaimsFromJWT.getClaim("cnf").asInstanceOf[java.util.Map[String, Object]]
    val jwkFromCnf = cnf.get("jwk").asInstanceOf[java.util.Map[String, Object]]
    val extractedBindingKey = JWK.parse(jwkFromCnf)
    // println(s"Extracted binding key from cnf claim: ${extractedBindingKey.getKeyID}")

    // Step 16: Verify binding JWT signature with extracted key
    assert(bindingJwt.verifyWith(holderEd25519KeyOKP), "Binding JWT signature should be valid")
    println("Binding JWT signature verified with holder's public key from cnf")

    // Step 17: Verify sd_hash matches computed value
    val parsedBindingJWT = SignedJWT.parse(vpSDJWT.getBindingJwt())
    val bindingClaims = parsedBindingJWT.getJWTClaimsSet()
    val sdHashFromBinding = bindingClaims.getStringClaim("sd_hash")
    val expectedSDHash = vpSDJWT.getSDHash()
    assertEquals(expectedSDHash, sdHashFromBinding, "sd_hash in binding JWT should match computed SD hash")
    // println(s"SD hash verified: $sdHashFromBinding")

    // Step 18: Additional checks (required claims present)
    assert(bindingClaims.getClaim("iat") != null, "Binding JWT should have 'iat' claim")
    assert(bindingClaims.getClaim("aud") != null, "Binding JWT should have 'aud' claim")
    assert(bindingClaims.getClaim("nonce") != null, "Binding JWT should have 'nonce' claim")
    assert(bindingClaims.getClaim("sd_hash") != null, "Binding JWT should have 'sd_hash' claim")
    println("All required binding JWT claims present")

    assertEquals(
      selectedDisclosures.size,
      vpSDJWT.getDisclosures().size(),
      "VP should contain exactly the selected disclosures"
    )
    // println(s"Disclosure count verified: ${vpSDJWT.getDisclosures().size()}")

    // Step 19-20: Serialization round-trip
    val vpString = vpSDJWT.toString()
    val parsedVP = SDJWT.parse(vpString)
    assert(parsedVP.getCredentialJwt() != null, "Parsed VP should have credential JWT")
    assert(parsedVP.getBindingJwt() != null, "Parsed VP should have binding JWT")
    assertEquals(
      selectedDisclosures.size,
      parsedVP.getDisclosures().size(),
      "Parsed VP should have same disclosure count"
    )
    // println(s"VP serialization round-trip successful (${vpString.length} chars)")
  }

}
