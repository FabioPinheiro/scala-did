package fmgp.did.method.prism

import zio.*
import scalus.cardano.node.BlockfrostProvider
import fmgp.did.method.prism.cardano.*

extension (bf: BlockfrostConfig)
  def nodeProvider: Task[BlockfrostProvider] = bf.network match
    case PublicCardanoNetwork.Mainnet => ZIO.fromFuture(implicit ec => BlockfrostProvider.mainnet(bf.token))
    case PublicCardanoNetwork.Testnet => ZIO.fail(RuntimeException("Cardano 'testnet' does not exist anymore"))
    case PublicCardanoNetwork.Preprod => ZIO.fromFuture(implicit ec => BlockfrostProvider.preprod(bf.token))
    case PublicCardanoNetwork.Preview => ZIO.fromFuture(implicit ec => BlockfrostProvider.preview(bf.token))
    case PrivateCardanoNetwork(blockfrostURL, protocolMagic) => ???
