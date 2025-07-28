package fmgp.did.method.prism

import zio._
import fmgp.did.method.prism.cardano.TxHash
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import _root_.proto.prism.SignedPrismOperation

case class PrismChainServiceImpl(
    bfConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
) extends PrismChainService {
  def commitAndPush(
      prismEvents: Seq[SignedPrismOperation],
      msg: Option[String],
  ): ZIO[Any, Throwable, TxHash] =
    for {
      tx <- ZIO.succeed(CardanoService.makeTrasation(bfConfig, wallet, prismEvents, msg))
      txHash <- CardanoService
        .submitTransaction(tx)
        .provideEnvironment(ZEnvironment(bfConfig))
    } yield (txHash)
}
