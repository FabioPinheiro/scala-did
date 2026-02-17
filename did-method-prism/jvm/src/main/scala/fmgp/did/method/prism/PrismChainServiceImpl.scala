package fmgp.did.method.prism

import zio.*
import fmgp.did.method.prism.cardano.TxHash
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import _root_.proto.prism.SignedPrismEvent

case class PrismChainServiceImpl(
    bfConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
) extends PrismChainService {
  def commitAndPush(
      prismEvents: Seq[SignedPrismEvent],
      msg: Option[String],
  ): ZIO[Any, Throwable, TxHash] = {
    for {
      // tx <- ZIO.succeed(CardanoService.makeTrasation(bfConfig, wallet, prismEvents, msg))
      // txHash <- CardanoService
      //   .submitTransaction(tx)
      //   .provideEnvironment(ZEnvironment(bfConfig))
      tx <- ScalusService
        .makeTrasation(prismEvents, msg)
      txHash <- ScalusService
        .submitTransaction(tx)
    } yield (txHash)
  }.provideEnvironment(ZEnvironment(bfConfig) ++ ZEnvironment(wallet))
}
