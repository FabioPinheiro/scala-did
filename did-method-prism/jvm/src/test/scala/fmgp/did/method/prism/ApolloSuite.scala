package fmgp.did.method.prism

import munit.FunSuite
import org.hyperledger.identus.apollo.utils._
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey

/** didResolverPrismJVM/testOnly fmgp.prism.ApolloSuite */
class ApolloSuite extends FunSuite {

  test("create key with Apollo") {
    val seed = MnemonicHelper.Companion.createRandomSeed("")
    val pk = HDKey(seed, 0, 0).getKMMSecp256k1PrivateKey()
    val sign = pk.sign("abc".getBytes())

    val pubK = pk.getPublicKey()
    assert(pubK.verify(sign, "abc".getBytes()))

    // pubK.getCurvePoint().getX()
    // pubK.getCurvePoint().getY()
    // pubK.getCompressed()
  }
}
