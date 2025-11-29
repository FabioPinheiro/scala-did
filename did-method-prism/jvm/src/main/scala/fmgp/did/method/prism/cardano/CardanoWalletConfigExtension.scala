package fmgp.did.method.prism.cardano

import scala.jdk.CollectionConverters.*
import fmgp.crypto.Secp256k1PrivateKey
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey

extension (wallet: CardanoWalletConfig) {

  def seed: Array[Byte] = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)

  def secp256k1PrivateKey(depth: Int, childIndex: Int): Secp256k1PrivateKey =
    Secp256k1PrivateKey(HDKey(wallet.seed, depth, childIndex).getKMMSecp256k1PrivateKey().getEncoded())

}
