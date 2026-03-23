package fmgp.did.method.prism

import munit.*
import scalus.cardano.wallet.hd.HdKeyPair
import scalus.crypto.ed25519.given

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm.*
import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.proto.PrismKeyUsage
import org.hyperledger.identus.apollo.derivation.HDKey
import fmgp.util.bytes2Hex
import fmgp.crypto.{given, *}
import fmgp.crypto.UtilsJVM.*

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.CardanoWalletConfigSuite */
class CardanoWalletConfigSuite extends FunSuite {

  test("Cip0000.didPath follows the deterministic-prism-did-generation-proposal") {
    val path = Cip0000.didPath(didIndex = 0, keyUsage = PrismKeyUsage.MasterKeyUsage, keyIndex = 0)
    assertEquals(path, s"m/29'/29'/0'/1'/0'")
  }

  test("HdKeyPair deriveHardened vs deriveNormal") {
    val hdKey1 = HdKeyPair.fromMnemonic(TestPeer.mnemonic, "", path = s"m/29'/29'/0'/1'/0'")
    val hdKey2 = HdKeyPair.fromMnemonic(TestPeer.mnemonic, "", path = s"m/29'/29'/0'/1'").deriveHardened(0)
    val hdKey3 = HdKeyPair.fromMnemonic(TestPeer.mnemonic, "", path = s"m/29'/29'/0'/1'").deriveNormal(0)
    val hdKey4 = HdKeyPair.fromMnemonic(TestPeer.mnemonic, "", path = s"m/29'/29'/0'/1'").deriveChild(0)

    assertEquals(hdKey1, hdKey2)
    assertEquals(hdKey3, hdKey4)
    assertNotEquals(hdKey1, hdKey3)

    assert(!java.util.Arrays.equals(hdKey1.extendedKey.kL, hdKey3.extendedKey.kL))
    assert(!java.util.Arrays.equals(hdKey1.extendedKey.kR, hdKey3.extendedKey.kR))
    assert(!java.util.Arrays.equals(hdKey1.extendedKey.chainCode, hdKey3.extendedKey.chainCode))
  }

  test("secp256k1 test vector") {
    val mnemonic =
      "vacuum only object oxygen sell engine " +
        "firm fiscal shiver finish village clock " +
        "limit unable reject lawn hard adapt " +
        "plunge between lawsuit stuff educate knock"

    val walletConfig: CardanoWalletConfig = CardanoWalletConfig.fromMnemonicPhrase(mnemonic).getOrElse(???)

    val testVectorPath2secp256k1PrivateKey = Seq( // The MASTER_KEY and VDR_KEY are of the type secp256k1
      "m/29'/29'/0'/1'/0'" -> "158bf13202ccafe551b5b4e60ed516efe0fe190e5c1421c3387f0f9fef2a6111",
      "m/29'/29'/0'/1'/1'" -> "a6836a79808f82cf91a443e6040041f12799a2567039b29b72bb0a31357368d1",
      "m/29'/29'/0'/8'/0'" -> "12d9eda38cc9a8a26b3ffb5d421367977bb3adbe766e188ec54f3c713d123681",
      "m/29'/29'/1'/1'/0'" -> "7419c4c397bcc7209dadbd8a7b9a957ea9266bea605867a330b5d40073e9df30",
      "m/29'/29'/1'/1'/1'" -> "9525cb7bfbce9b34da5e730aa82d5a1779bd5a337777909059a9ee91b244d561",
      "m/29'/29'/1'/8'/0'" -> "856b8496af764af3f8b864f3dbf00be61aeee46e531f07248ff77dc58eb51160",
      "m/29'/29'/2'/1'/0'" -> "591f15d298c49611003bf7b85b7d4b4d100aa34a9680e1faf55a409f837e2f96",
      "m/29'/29'/2'/1'/1'" -> "61a9950ff1c6cb3775e2776c7772ed1bd0bd8f9ced15cffd7d633f893ef55cd8",
      "m/29'/29'/2'/8'/0'" -> "1e17ca1575b2084366441abb2c109fc0600e495e6b7097d1678645b7662d4ca6",
    )

    testVectorPath2secp256k1PrivateKey.map { case (path, expectedPrivateKeyRaw) =>
      val hexApollo = bytes2Hex(HDKey(walletConfig.seed, 0, 0).derive(path).getKMMSecp256k1PrivateKey().getEncoded())
      val pk_rawBytes = bytes2Hex(walletConfig.secp256k1DerivePath(path).rawBytes)

      assertEquals(hexApollo, expectedPrivateKeyRaw)
      assertEquals(pk_rawBytes, expectedPrivateKeyRaw)
    }
  }

  test("ed25519 test vector") {
    val mnemonic =
      "vacuum only object oxygen sell engine " +
        "firm fiscal shiver finish village clock " +
        "limit unable reject lawn hard adapt " +
        "plunge between lawsuit stuff educate knock"

    val walletConfig: CardanoWalletConfig = CardanoWalletConfig.fromMnemonicPhrase(mnemonic).getOrElse(???)

    val testVectorPath = Seq( // TODO REVIEW
      "m/29'/29'/0'/3'/0'" -> "08f27fbb51b2a6d6efa5fe7e4c80fff1f2665f36a94c00f4538f0b86deb88f5fb0dcc3669b9cbbb7fd9b32bfcb73572a964eb77b35a756916e75db48a23f6b7e",
    )

    testVectorPath.map { case (path, expectedPrivateKeyRaw) =>

      val hdKeyPair = HdKeyPair.fromMnemonic(mnemonic, "", path = path)
      val hex = bytes2Hex(hdKeyPair.extendedKey.extendedSecretKey)
      assertEquals(hex, expectedPrivateKeyRaw)
    }
  }

  test("x25519 test vector (derived from ed25519 kL)") {
    val mnemonic =
      "vacuum only object oxygen sell engine " +
        "firm fiscal shiver finish village clock " +
        "limit unable reject lawn hard adapt " +
        "plunge between lawsuit stuff educate knock"
    val walletConfig = CardanoWalletConfig.fromMnemonicPhrase(mnemonic).getOrElse(???)

    val key = walletConfig.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 0)

    assertEquals(key.crv, fmgp.crypto.Curve.X25519)

    // Determinism
    assertEquals(key, walletConfig.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 0))

    // Public key consistency: x must match X25519.publicFromPrivate(d)
    val dBytes = java.util.Base64.getUrlDecoder.decode(key.d)
    val expectedPubBytes = com.google.crypto.tink.subtle.X25519.publicFromPrivate(dBytes)
    assertEquals(key.x, fmgp.util.Base64.encode(expectedPubBytes).urlBase64WithoutPadding)
  }

  test("x25519 golden vectors") {
    val mnemonic =
      "vacuum only object oxygen sell engine " +
        "firm fiscal shiver finish village clock " +
        "limit unable reject lawn hard adapt " +
        "plunge between lawsuit stuff educate knock"
    val wc = CardanoWalletConfig.fromMnemonicPhrase(mnemonic).getOrElse(???)
    val k0 = wc.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    val k1 = wc.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 1)
    val k2 = wc.x25519DerivePrism(1, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    assertEquals(k0.d, "CPJ_u1Gyptbvpf5-TID_8fJmXzapTAD0U48Lht64j18")
    assertEquals(k0.x, "SdzjTteWPgFzTYQrRozb81Gptf7p1F3-HhsDMZfOUnE")
    assertEquals(k1.d, "CC_Yfic8IveduWE5vLzg3kLCLmuQjsJE1JnSrtu4j18")
    assertEquals(k1.x, "_AAUtInXX5uJ0Fsf_wNNCYl4rlqZcb3vyg8oVSNMxUA")
    assertEquals(k2.d, "UOuseGdStDrWziiRY3173Gj6eRauSeoxptlBwd-4j18")
    assertEquals(k2.x, "PrQ1Bb4dtsiU1rsGYFoFs4qod1Pqi9RQRMuzBrco0l8")
  }

  test("x25519 keys are distinct across indices") {
    val key0 = TestPeer.cw.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    val key1 = TestPeer.cw.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 1)
    val key2 = TestPeer.cw.x25519DerivePrism(1, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    assertNotEquals(key0.d, key1.d)
    assertNotEquals(key0.d, key2.d)
  }

  test("x25519 anonEncrypt/anonDecrypt round-trip (raw)") {
    val key = TestPeer.cw.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    val kid = VerificationMethodReferenced("did:prism:test#key-x25519-0")
    val clearText = "hello world".getBytes

    val header = AnonHeaderBuilder(
      apv = APV(Seq(kid)),
      enc = ENCAlgorithm.`A256CBC-HS512`,
      alg = KWAlgorithm.`ECDH-ES+A256KW`,
    )

    val result = for {
      encrypted     <- ECDH_AnonOKP.encrypt(Seq((kid, key.toPublicKey)), header, clearText)
      jweRecipients  = encrypted.recipients.map(r => JWERecipient(r.header.kid, r.encrypted_key))
      decrypted     <- ECDH_AnonOKP.decrypt(Seq((kid, key)), encrypted.`protected`, jweRecipients, encrypted.iv, encrypted.ciphertext, encrypted.tag)
    } yield decrypted

    result match
      case Left(err)    => fail(s"round-trip failed: $err")
      case Right(plain) => assertEquals(new String(plain), "hello world")
  }

  test("x25519 authEncrypt/authDecrypt round-trip (raw)") {
    val senderKey = TestPeer.cw.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    val recipientKey = TestPeer.cw.x25519DerivePrism(1, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    val senderKid = VerificationMethodReferenced("did:prism:test#sender-x25519")
    val recipientKid = VerificationMethodReferenced("did:prism:test#recipient-x25519")
    val clearText = "authenticated hello".getBytes

    val header = AuthHeaderBuilder(
      apv = APV(Seq(recipientKid)),
      skid = senderKid,
      apu = APU(senderKid),
      enc = ENCAlgorithm.`A256CBC-HS512`,
      alg = KWAlgorithm.`ECDH-1PU+A256KW`,
    )

    val result = for {
      encrypted     <- ECDH_AuthOKP.encrypt(senderKey, Seq((recipientKid, recipientKey.toPublicKey)), header, clearText)
      jweRecipients  = encrypted.recipients.map(r => JWERecipient(r.header.kid, r.encrypted_key))
      decrypted     <- ECDH_AuthOKP.decrypt(senderKey.toPublicKey, Seq((recipientKid, recipientKey)), encrypted.`protected`, jweRecipients, encrypted.iv, encrypted.ciphertext, encrypted.tag)
    } yield decrypted

    result match
      case Left(err)    => fail(s"authEncrypt/authDecrypt round-trip failed: $err")
      case Right(plain) => assertEquals(new String(plain), "authenticated hello")
  }

  test("x25519 anonDecrypt with wrong key fails") {
    val correctKey = TestPeer.cw.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    val wrongKey = TestPeer.cw.x25519DerivePrism(1, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    val kid = VerificationMethodReferenced("did:prism:test#key-x25519-0")
    val clearText = "secret".getBytes

    val header = AnonHeaderBuilder(
      apv = APV(Seq(kid)),
      enc = ENCAlgorithm.`A256CBC-HS512`,
      alg = KWAlgorithm.`ECDH-ES+A256KW`,
    )

    val result = for {
      encrypted     <- ECDH_AnonOKP.encrypt(Seq((kid, correctKey.toPublicKey)), header, clearText)
      jweRecipients  = encrypted.recipients.map(r => JWERecipient(r.header.kid, r.encrypted_key))
      decrypted     <- ECDH_AnonOKP.decrypt(Seq((kid, wrongKey)), encrypted.`protected`, jweRecipients, encrypted.iv, encrypted.ciphertext, encrypted.tag)
    } yield decrypted

    assert(result.isLeft, "decrypting with a wrong key should fail")
  }

  test("x25519 anonEncrypt ciphertext is non-deterministic (ephemeral key)") {
    val key = TestPeer.cw.x25519DerivePrism(0, PrismKeyUsage.KeyAgreementKeyUsage, 0)
    val kid = VerificationMethodReferenced("did:prism:test#key-x25519-0")
    val data = "same plaintext".getBytes
    val mkHeader =
      AnonHeaderBuilder(apv = APV(Seq(kid)), enc = ENCAlgorithm.`A256CBC-HS512`, alg = KWAlgorithm.`ECDH-ES+A256KW`)

    val ct1 = ECDH_AnonOKP.encrypt(Seq((kid, key.toPublicKey)), mkHeader, data).map(_.ciphertext)
    val ct2 = ECDH_AnonOKP.encrypt(Seq((kid, key.toPublicKey)), mkHeader, data).map(_.ciphertext)
    assertNotEquals(ct1, ct2)
  }

  test("OKP EdDSA (BIP32-Ed25519 key) sign via signJWT and verify via Extensions") {
    val hdKeyPair = CardanoWalletConfig
      .fromMnemonicPhrase(
        "year degree tiger isolate notice barely indicate journey female citizen general begin speak brass abandon appear owner planet giraffe document syrup just final parent"
      )
      .getOrElse(???)
      .ed25519DerivePrism(0, PrismKeyUsage.AssertionMethodKeyUsage, 0)

    val walletEd25519PublicKey = hdKeyPair.publicJWK.withoutKid.withKid(kid = "wallet-key-Ed25519")

    val jwtUnsigned = JWTUnsigned(
      header = JWTHeader(alg = JWAAlgorithm.EdDSA, kid = Some(walletEd25519PublicKey.kid)),
      payload = Payload.fromBytes("hello world".getBytes),
    )
    jwtUnsigned.signWithExtendedKey(hdKeyPair) match
      case Left(error) => fail(error)
      case Right(jwt)  => assert(jwt.verifyWith(walletEd25519PublicKey), "verify failed")
  }
}
