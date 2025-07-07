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

  sealed trait MnemonicSubcommand extends Subcommand
  final case class MnemonicCreate() extends MnemonicSubcommand
  final case class MnemonicSeed(mWallet: Option[CardanoWalletConfig]) extends MnemonicSubcommand
  final case class Mnemonic2Key(mWallet: Option[CardanoWalletConfig], depth: Int, childIndex: Int)
      extends MnemonicSubcommand

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
