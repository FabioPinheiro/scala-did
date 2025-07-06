package fmgp.did.method.prism.vdr

import fmgp.did.method.prism.cardano.CardanoNetwork

final case class BlockfrostConfig(token: String) {
  val network: CardanoNetwork = CardanoNetwork.fromBlockfrostToken(token)
}
