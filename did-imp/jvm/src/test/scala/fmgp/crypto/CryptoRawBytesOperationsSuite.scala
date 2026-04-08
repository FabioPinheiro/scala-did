package fmgp.crypto

import munit.*
import zio.json.*

import fmgp.did.comm.*
import fmgp.crypto.CryptoRawBytesOperationsJVM.*
import fmgp.util.Base64

/** didImpJVM/testOnly fmgp.crypto.CryptoRawBytesOperationsSuite
  *
  * TODO MOVE to shared folder
  */
class CryptoRawBytesOperationsSuite extends FunSuite {

  /** Convert a hex string to a base64url-encoded string (without padding). */
  private def hexToBase64url(hex: String): String = {
    val bytes = hex.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray
    java.util.Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }

  /** Convert a hex string to a byte array. */
  private def hexToBytes(hex: String): Array[Byte] =
    hex.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  val okpKey = OKPPrivateKey(
    kty = KTY.OKP,
    crv = Curve.Ed25519,
    d = "pFRUKkyzx4kHdJtFSnlPA9WzqkDT1HWV0xZ5OYZd2SY",
    x = "G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww",
  )

  val payload: Array[Byte] = DIDCommExamples.plaintextMessageObj.toJson.getBytes

  test("Ed25519 sign raw bytes and verify") {
    val signature = okpKeySignBytesWithEd25519(okpKey, payload)
    assertEquals(signature.length, 64)
    assert(okpKeyVerifyBytesWithEd25519(okpKey.toPublicKey, payload, signature))
  }

  test("Ed25519 raw signature is deterministic") {
    val sig1 = okpKeySignBytesWithEd25519(okpKey, payload)
    val sig2 = okpKeySignBytesWithEd25519(okpKey, payload)
    assertEquals(sig1.toSeq, sig2.toSeq)
  }

  test("Ed25519 raw signature fails verification with wrong payload") {
    val signature = okpKeySignBytesWithEd25519(okpKey, payload)
    val wrongPayload = "wrong data".getBytes
    assert(!okpKeyVerifyBytesWithEd25519(okpKey.toPublicKey, wrongPayload, signature))
  }

  test("Ed25519 raw signature fails verification with wrong key") {
    val signature = okpKeySignBytesWithEd25519(okpKey, payload)

    val otherKey = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = "INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug",
      x = "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA",
    )

    assert(!okpKeyVerifyBytesWithEd25519(otherKey.toPublicKey, payload, signature))
  }

  // =====================================================
  // RFC 8032 Section 7.2 - Ed25519ctx test vectors
  // NOTE: Ed25519ctx is a different algorithm from Ed25519.
  //   It uses a context string and a different dom2 prefix.
  //   Tink (and our implementation) only supports plain Ed25519,
  //   so these vectors cannot be tested here.
  // =====================================================

  // =====================================================
  // RFC 8032 Section 7.3 - Ed25519ph test vector
  // NOTE: Ed25519ph (prehashed) is a different algorithm from Ed25519.
  //   It hashes the message with SHA-512 before signing and uses a dom2 prefix.
  //   Tink (and our implementation) only supports plain Ed25519,
  //   so this vector cannot be tested here.
  // =====================================================

  // =====================================================
  // RFC 8032 Section 7.1 - Ed25519 test vectors
  // https://datatracker.ietf.org/doc/html/rfc8032#section-7.1
  // =====================================================

  test("RFC 8032 Section 7.1 - Ed25519 Test Vector 1 (empty message)") {
    val secretKey = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"
    val publicKey = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"
    val message = Array.emptyByteArray
    val expectedSig =
      "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"

    val key = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = hexToBase64url(secretKey),
      x = hexToBase64url(publicKey),
    )
    val signature = okpKeySignBytesWithEd25519(key, message)
    assertEquals(signature.toSeq, hexToBytes(expectedSig).toSeq)
    assert(okpKeyVerifyBytesWithEd25519(key.toPublicKey, message, signature))
  }

  test("RFC 8032 Section 7.1 - Ed25519 Test Vector 2 (1 byte)") {
    val secretKey = "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb"
    val publicKey = "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c"
    val message = "72"
    val expectedSig =
      "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00"

    val key = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = hexToBase64url(secretKey),
      x = hexToBase64url(publicKey),
    )
    val signature = okpKeySignBytesWithEd25519(key, hexToBytes(message))
    assertEquals(signature.toSeq, hexToBytes(expectedSig).toSeq)
    assert(okpKeyVerifyBytesWithEd25519(key.toPublicKey, hexToBytes(message), signature))
  }

  test("RFC 8032 Section 7.1 - Ed25519 Test Vector 3 (2 bytes)") {
    val secretKey = "c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7"
    val publicKey = "fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025"
    val message = "af82"
    val expectedSig =
      "6291d657deec24024827e69c3abe01a30ce548a284743a445e3680d7db5ac3ac18ff9b538d16f290ae67f760984dc6594a7c15e9716ed28dc027beceea1ec40a"

    val key = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = hexToBase64url(secretKey),
      x = hexToBase64url(publicKey),
    )
    val signature = okpKeySignBytesWithEd25519(key, hexToBytes(message))
    assertEquals(signature.toSeq, hexToBytes(expectedSig).toSeq)
    assert(okpKeyVerifyBytesWithEd25519(key.toPublicKey, hexToBytes(message), signature))
  }

  test("RFC 8032 Section 7.1 - Ed25519 Test Vector 4 (1023 bytes)") {
    val secretKey = "f5e5767cf153319517630f226876b86c8160cc583bc013744c6bf255f5cc0ee5"
    val publicKey = "278117fc144c72340f67d0f2316e8386ceffbf2b2428c9c51fef7c597f1d426e"
    val message =
      "08b8b2b733424243760fe426a4b54908632110a66c2f6591eabd3345e3e4eb98" +
        "fa6e264bf09efe12ee50f8f54e9f77b1e355f6c50544e23fb1433ddf73be84d8" +
        "79de7c0046dc4996d9e773f4bc9efe5738829adb26c81b37c93a1b270b20329d" +
        "658675fc6ea534e0810a4432826bf58c941efb65d57a338bbd2e26640f89ffbc" +
        "1a858efcb8550ee3a5e1998bd177e93a7363c344fe6b199ee5d02e82d522c4fe" +
        "ba15452f80288a821a579116ec6dad2b3b310da903401aa62100ab5d1a36553e" +
        "06203b33890cc9b832f79ef80560ccb9a39ce767967ed628c6ad573cb116dbef" +
        "efd75499da96bd68a8a97b928a8bbc103b6621fcde2beca1231d206be6cd9ec7" +
        "aff6f6c94fcd7204ed3455c68c83f4a41da4af2b74ef5c53f1d8ac70bdcb7ed1" +
        "85ce81bd84359d44254d95629e9855a94a7c1958d1f8ada5d0532ed8a5aa3fb2" +
        "d17ba70eb6248e594e1a2297acbbb39d502f1a8c6eb6f1ce22b3de1a1f40cc24" +
        "554119a831a9aad6079cad88425de6bde1a9187ebb6092cf67bf2b13fd65f270" +
        "88d78b7e883c8759d2c4f5c65adb7553878ad575f9fad878e80a0c9ba63bcbcc" +
        "2732e69485bbc9c90bfbd62481d9089beccf80cfe2df16a2cf65bd92dd597b07" +
        "07e0917af48bbb75fed413d238f5555a7a569d80c3414a8d0859dc65a46128ba" +
        "b27af87a71314f318c782b23ebfe808b82b0ce26401d2e22f04d83d1255dc51a" +
        "ddd3b75a2b1ae0784504df543af8969be3ea7082ff7fc9888c144da2af58429e" +
        "c96031dbcad3dad9af0dcbaaaf268cb8fcffead94f3c7ca495e056a9b47acdb7" +
        "51fb73e666c6c655ade8297297d07ad1ba5e43f1bca32301651339e22904cc8c" +
        "42f58c30c04aafdb038dda0847dd988dcda6f3bfd15c4b4c4525004aa06eeff8" +
        "ca61783aacec57fb3d1f92b0fe2fd1a85f6724517b65e614ad6808d6f6ee34df" +
        "f7310fdc82aebfd904b01e1dc54b2927094b2db68d6f903b68401adebf5a7e08" +
        "d78ff4ef5d63653a65040cf9bfd4aca7984a74d37145986780fc0b16ac451649" +
        "de6188a7dbdf191f64b5fc5e2ab47b57f7f7276cd419c17a3ca8e1b939ae49e4" +
        "88acba6b965610b5480109c8b17b80e1b7b750dfc7598d5d5011fd2dcc5600a3" +
        "2ef5b52a1ecc820e308aa342721aac0943bf6686b64b2579376504ccc493d97e" +
        "6aed3fb0f9cd71a43dd497f01f17c0e2cb3797aa2a2f256656168e6c496afc5f" +
        "b93246f6b1116398a346f1a641f3b041e989f7914f90cc2c7fff357876e506b5" +
        "0d334ba77c225bc307ba537152f3f1610e4eafe595f6d9d90d11faa933a15ef1" +
        "369546868a7f3a45a96768d40fd9d03412c091c6315cf4fde7cb68606937380d" +
        "b2eaaa707b4c4185c32eddcdd306705e4dc1ffc872eeee475a64dfac86aba41c" +
        "0618983f8741c5ef68d3a101e8a3b8cac60c905c15fc910840b94c00a0b9d0"
    val expectedSig =
      "0aab4c900501b3e24d7cdf4663326a3a87df5e4843b2cbdb67cbf6e460fec350aa5371b1508f9f4528ecea23c436d94b5e8fcd4f681e30a6ac00a9704a188a03"

    val key = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = hexToBase64url(secretKey),
      x = hexToBase64url(publicKey),
    )
    val msgBytes = hexToBytes(message)
    assertEquals(msgBytes.length, 1023)
    val signature = okpKeySignBytesWithEd25519(key, msgBytes)
    assertEquals(signature.toSeq, hexToBytes(expectedSig).toSeq)
    assert(okpKeyVerifyBytesWithEd25519(key.toPublicKey, msgBytes, signature))
  }

  test("RFC 8032 Section 7.1 - Ed25519 Test Vector 5 (SHA(abc))") {
    val secretKey = "833fe62409237b9d62ec77587520911e9a759cec1d19755b7da901b96dca3d42"
    val publicKey = "ec172b93ad5e563bf4932c70e1245034c35467ef2efd4d64ebf819683467e2bf"
    val message =
      "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"
    val expectedSig =
      "dc2a4459e7369633a52b1bf277839a00201009a3efbf3ecb69bea2186c26b58909351fc9ac90b3ecfdfbc7c66431e0303dca179c138ac17ad9bef1177331a704"

    val key = OKPPrivateKey(
      kty = KTY.OKP,
      crv = Curve.Ed25519,
      d = hexToBase64url(secretKey),
      x = hexToBase64url(publicKey),
    )
    val signature = okpKeySignBytesWithEd25519(key, hexToBytes(message))
    assertEquals(signature.toSeq, hexToBytes(expectedSig).toSeq)
    assert(okpKeyVerifyBytesWithEd25519(key.toPublicKey, hexToBytes(message), signature))
  }

  // =====================================================
  // EC ECDSA raw bytes tests
  // =====================================================

  // --- P-256 (ES256) ---

  val p256Key1: ECPrivateKey = JWKExamples.senderKeyP256.fromJson[ECPrivateKey].toOption.get
  val p256Key2: ECPrivateKey = JWKExamples.senderKeyP256_2.fromJson[ECPrivateKey].toOption.get

  test("EC P-256 (ES256) sign raw bytes and verify") {
    val signature = ecKeySignBytesWithEC(p256Key1, payload)
    assert(ecKeyVerifyBytesWithEC(p256Key1.toPublicKey, payload, signature))
  }

  test("EC P-256 (ES256) raw signature fails with wrong payload") {
    val signature = ecKeySignBytesWithEC(p256Key1, payload)
    assert(!ecKeyVerifyBytesWithEC(p256Key1.toPublicKey, "wrong data".getBytes, signature))
  }

  test("EC P-256 (ES256) raw signature fails with wrong key") {
    val signature = ecKeySignBytesWithEC(p256Key1, payload)
    assert(!ecKeyVerifyBytesWithEC(p256Key2.toPublicKey, payload, signature))
  }

  // --- secp256k1 (ES256K) ---

  val secp256k1Key1: ECPrivateKey = JWKExamples.senderKeySecp256k1.fromJson[ECPrivateKey].toOption.get
  val secp256k1Key2: ECPrivateKey =
    JWKExamples.recipientKey4.fromJson[ECPrivateKey].toOption.get // P-256, different curve for wrong-key test

  test("EC secp256k1 (ES256K) sign raw bytes and verify") {
    val signature = ecKeySignBytesWithEC(secp256k1Key1, payload)
    assert(ecKeyVerifyBytesWithEC(secp256k1Key1.toPublicKey, payload, signature))
  }

  test("EC secp256k1 (ES256K) raw signature fails with wrong payload") {
    val signature = ecKeySignBytesWithEC(secp256k1Key1, payload)
    assert(!ecKeyVerifyBytesWithEC(secp256k1Key1.toPublicKey, "wrong data".getBytes, signature))
  }

  test("EC secp256k1 (ES256K) raw signature fails with wrong key") {
    val signature = ecKeySignBytesWithEC(secp256k1Key1, payload)
    // Use a P-256 key — different curve, verification should fail
    assert(!ecKeyVerifyBytesWithEC(p256Key1.toPublicKey, payload, signature))
  }

  // --- P-384 (ES384) ---

  val p384Key1: ECPrivateKey = JWKExamples.recipientKey6.fromJson[ECPrivateKey].toOption.get
  val p384Key2: ECPrivateKey = JWKExamples.recipientKey7.fromJson[ECPrivateKey].toOption.get

  test("EC P-384 (ES384) sign raw bytes and verify") {
    val signature = ecKeySignBytesWithEC(p384Key1, payload)
    assert(ecKeyVerifyBytesWithEC(p384Key1.toPublicKey, payload, signature))
  }

  test("EC P-384 (ES384) raw signature fails with wrong payload") {
    val signature = ecKeySignBytesWithEC(p384Key1, payload)
    assert(!ecKeyVerifyBytesWithEC(p384Key1.toPublicKey, "wrong data".getBytes, signature))
  }

  test("EC P-384 (ES384) raw signature fails with wrong key") {
    val signature = ecKeySignBytesWithEC(p384Key1, payload)
    assert(!ecKeyVerifyBytesWithEC(p384Key2.toPublicKey, payload, signature))
  }

  // --- P-521 (ES512) ---

  val p521Key1: ECPrivateKey = JWKExamples.senderKeyP521.fromJson[ECPrivateKey].toOption.get
  val p521Key2: ECPrivateKey = JWKExamples.recipientKey8.fromJson[ECPrivateKey].toOption.get

  test("EC P-521 (ES512) sign raw bytes and verify") {
    val signature = ecKeySignBytesWithEC(p521Key1, payload)
    assert(ecKeyVerifyBytesWithEC(p521Key1.toPublicKey, payload, signature))
  }

  test("EC P-521 (ES512) raw signature fails with wrong payload") {
    val signature = ecKeySignBytesWithEC(p521Key1, payload)
    assert(!ecKeyVerifyBytesWithEC(p521Key1.toPublicKey, "wrong data".getBytes, signature))
  }

  test("EC P-521 (ES512) raw signature fails with wrong key") {
    val signature = ecKeySignBytesWithEC(p521Key1, payload)
    assert(!ecKeyVerifyBytesWithEC(p521Key2.toPublicKey, payload, signature))
  }

}
