package fmgp.did.method.prism

import fmgp.did.method.prism.cardano.PublicCardanoNetwork
import fmgp.did.method.prism.cardano.PrivateCardanoNetwork
import fmgp.did.method.prism.cardano.CardanoNetwork

final case class BlockfrostConfig(token: String, ryo: Option[BlockfrostRyoConfig] = None) {
  val network: CardanoNetwork =
    ryo match
      case Some(conf) => PrivateCardanoNetwork(conf.url, conf.protocolMagic)
      case _          => PublicCardanoNetwork.fromBlockfrostToken(token)

}

final case class BlockfrostRyoConfig(url: String, protocolMagic: Long)
