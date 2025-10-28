package fmgp.did.method.prism

import fmgp.did.method.prism.cardano.CardanoNetwork

final case class BlockfrostConfig(token: String) {
  val network: CardanoNetwork = CardanoNetwork.fromBlockfrostToken(token)
}
