package fmgp.did.method.prism.vdr

import fmgp.did.method.prism.cardano.CardanoNetwork

final case class BlockfrastConfig(token: String) {
  val network: CardanoNetwork = CardanoNetwork.fromBlockfrostToken(token)
}
