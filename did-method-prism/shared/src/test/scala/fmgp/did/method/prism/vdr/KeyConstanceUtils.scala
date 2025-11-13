package fmgp.did.method.prism.vdr

import fmgp.did.method.prism.cardano._

object KeyConstanceUtils {
  val wallet = CardanoWalletConfig()
  val pkMaster = wallet.secp256k1PrivateKey(0, 0) // HDKey(seed, 0, 0).getKMMSecp256k1PrivateKey()
  val pk1VDR = wallet.secp256k1PrivateKey(0, 1) // HDKey(seed, 0, 1).getKMMSecp256k1PrivateKey()
  val pk2VDR = wallet.secp256k1PrivateKey(0, 2) // HDKey(seed, 0, 2).getKMMSecp256k1PrivateKey()
  val pk3VDR = wallet.secp256k1PrivateKey(0, 3) // HDKey(seed, 0, 3).getKMMSecp256k1PrivateKey()
}
