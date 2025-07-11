package fmgp.did.method.prism.cli

import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.vdr.BlockfrostConfig
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import java.nio.file.Path

sealed trait CMD extends Product with Serializable
object CMD {
  final case class Indexer(workdir: Path, mBlockfrostConfig: Option[BlockfrostConfig]) extends CMD
  final case class Version() extends CMD
  final case class ConfigCMD(config: Setup, createFlag: Boolean) extends CMD

  sealed trait MnemonicCMD extends CMD
  final case class MnemonicCreate(setup: Setup, walletTypeOpt: WalletType) extends MnemonicCMD
  final case class MnemonicSeed(setup: Setup, mWallet: Option[CardanoWalletConfig]) extends MnemonicCMD
  final case class MnemonicAddress(
      setup: Setup,
      walletOrType: CardanoWalletConfig | WalletType.SSI.type | WalletType.Cardano.type,
      network: CardanoNetwork,
  ) extends MnemonicCMD

  sealed trait BlockfrostCMD extends CMD
  final case class BlockfrostToken(setup: Setup, network: CardanoNetwork, mBlockfrostConfig: Option[BlockfrostConfig])
      extends BlockfrostCMD
  final case class BlockfrostAddress(
      setup: Setup,
      network: CardanoNetwork,
      mBlockfrostConfig: Option[BlockfrostConfig],
      walletOrType: CardanoWalletConfig | WalletType.SSI.type | WalletType.Cardano.type,
  ) extends BlockfrostCMD

  // sealed trait KeyCMD extends CMD
  final case class Mnemonic2Key(
      setup: Setup,
      mWallet: Option[CardanoWalletConfig],
      derivationPath: String,
      keyLabel: Option[String]
  ) extends CMD

  sealed trait DIDCMD extends CMD
  final case class DIDCreate(
      setup: Setup,
      masterLabel: String,
      masterRaw: Option[String],
      vdrLabel: Option[String],
      vdrRaw: Option[String]
  ) extends DIDCMD
  final case class DIDUpdate(did: DIDPrism) extends DIDCMD
  final case class DIDDeactivate(did: DIDPrism) extends DIDCMD
  final case class DIDResolve(did: DIDPrism, network: CardanoNetwork) extends DIDCMD
  final case class DIDResolveFromFS(did: DIDPrism, workdir: Path, network: CardanoNetwork) extends DIDCMD

  // TODO VDR

  // TODO Event load PrismState
  // TODO Event validate Event
  // TODO Event validate Events
  // TODO Event validate Block

}
