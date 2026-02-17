package fmgp.did.method.prism

import munit.*
import scalus.cardano.wallet.hd.HdKeyPair
import scalus.crypto.ed25519.given

import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.proto.PrismKeyUsage
import org.hyperledger.identus.apollo.derivation.HDKey
import fmgp.util.bytes2Hex

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

}
