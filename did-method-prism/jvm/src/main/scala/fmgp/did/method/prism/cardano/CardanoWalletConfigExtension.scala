package fmgp.did.method.prism.cardano

import scala.jdk.CollectionConverters._
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey

extension (wallet: CardanoWalletConfig) {

  def seed: Array[Byte] = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)

  def secp256k1PrivateKey(depth: Int, childIndex: Int): KMMECSecp256k1PrivateKey =
    HDKey(wallet.seed, depth, childIndex).getKMMSecp256k1PrivateKey()

  def xxx = {
    val aaa: Seq[Int] = ???
    aaa.asJava
  }
}

// object CardanoWalletConfigExtension {}
