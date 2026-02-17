package fmgp.did.method.prism

import zio.*
import scalus.cardano.node.BlockfrostProvider
import fmgp.did.method.prism.cardano.*

extension (bf: BlockfrostConfig)
  def nodeProvider: Task[BlockfrostProvider] = bf.network match
    case PublicCardanoNetwork.Mainnet => ZIO.fromFuture(implicit ec => BlockfrostProvider.mainnet(bf.token))
    case PublicCardanoNetwork.Testnet => ???
    case PublicCardanoNetwork.Preprod => ??? // TODO
    case PublicCardanoNetwork.Preview => ??? // TODO
    case PrivateCardanoNetwork(blockfrostURL, protocolMagic) => ???
