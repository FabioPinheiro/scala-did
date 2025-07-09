package fmgp.did.method.prism.cli

import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.vdr.BlockfrostConfig
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import java.nio.file.Path

sealed trait Subcommand extends Product with Serializable
object Subcommand {
  final case class Indexer(workdir: Path, mBlockfrostConfig: Option[BlockfrostConfig]) extends Subcommand
  // final case class DID() extends Subcommand
  final case class Version() extends Subcommand
  final case class Staging(config: Setup, createFlag: Boolean) extends Subcommand
  final case class Test(config: Setup, data: String) extends Subcommand

  sealed trait MnemonicSubcommand extends Subcommand
  final case class MnemonicCreate(setup: Setup) extends MnemonicSubcommand
  final case class MnemonicSeed(setup: Setup, mWallet: Option[CardanoWalletConfig]) extends MnemonicSubcommand

  // sealed trait KeySubcommand extends Subcommand
  final case class Mnemonic2Key(
      setup: Setup,
      mWallet: Option[CardanoWalletConfig],
      derivationPath: String,
      keyLabel: Option[String]
  ) extends Subcommand

  sealed trait DIDSubcommand extends Subcommand
  final case class DIDCreate() extends DIDSubcommand
  final case class DIDUpdate(did: DIDPrism) extends DIDSubcommand
  final case class DIDDeactivate(did: DIDPrism) extends DIDSubcommand
  final case class DIDResolve(did: DIDPrism, network: CardanoNetwork) extends DIDSubcommand
  final case class DIDResolveFromFS(did: DIDPrism, workdir: Path, network: CardanoNetwork) extends DIDSubcommand

  // TODO VDR

  // TODO Event load PrismState
  // TODO Event validate Event
  // TODO Event validate Events
  // TODO Event validate Block

}
