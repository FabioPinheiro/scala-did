package fmgp.did.method.prism.cli

import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.RefVDR
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import java.nio.file.Path
import proto.prism.SignedPrismOperation

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

  final case class BlockfrostSubmitEvents(
      setup: Setup,
      network: CardanoNetwork,
      events: Seq[SignedPrismOperation],
  ) extends BlockfrostCMD

  sealed trait KeyCMD extends CMD
  final case class Mnemonic2Key(
      setup: Setup,
      mWallet: Option[CardanoWalletConfig],
      derivationPath: String,
      keyLabel: Option[String]
  ) extends KeyCMD

  final case class Mnemonic2Key2SSITestVector(
      setup: Setup,
      mWallet: Option[CardanoWalletConfig],
      index: Int
  ) extends KeyCMD

  sealed trait DIDCMD extends CMD
  final case class DIDCreate(
      setup: Setup,
      masterKeyLabel: String,
      masterKeyRaw: Option[String],
      vdrKeyLabel: Option[String],
      vdrKeyRaw: Option[String]
  ) extends DIDCMD
  final case class DIDUpdate(did: DIDPrism) extends DIDCMD
  final case class DIDDeactivate(did: DIDPrism) extends DIDCMD
  final case class DIDResolve(did: DIDPrism, network: CardanoNetwork) extends DIDCMD
  final case class DIDResolveFromFS(did: DIDPrism, workdir: Path, network: CardanoNetwork) extends DIDCMD

  sealed trait VDRCMD extends CMD
  final case class VDRCreateBytes(
      setup: Setup, // TODO option to get the owner SSI/DID from the state
      network: CardanoNetwork,
      workdir: Path,
      didOwner: DIDPrism, // and make it optional
      vdrKeyLabel: String,
      vdrKeyRaw: Option[Array[Byte]],
      data: Array[Byte],
  ) extends VDRCMD
  final case class VDRCreateIPFS(
      setup: Setup, // TODO option to get the owner SSI/DID from the state
      network: CardanoNetwork,
      workdir: Path,
      didOwner: DIDPrism, // and make it optional
      vdrKeyLabel: String,
      vdrKeyRaw: Option[Array[Byte]],
      cid: String,
  ) extends VDRCMD
  final case class VDRUpdateBytes(
      setup: Setup,
      network: CardanoNetwork,
      workdir: Path,
      vdrEntryRef: RefVDR,
      vdrKeyLabel: String,
      vdrKeyRaw: Option[Array[Byte]],
      data: Array[Byte],
  ) extends VDRCMD
  final case class VDRUpdateIPFS(
      setup: Setup,
      network: CardanoNetwork,
      workdir: Path,
      vdrEntryRef: RefVDR,
      vdrKeyLabel: String,
      vdrKeyRaw: Option[Array[Byte]],
      cid: String,
  ) extends VDRCMD
  final case class VDRDeactivateEntry(
      setup: Setup,
      network: CardanoNetwork,
      workdir: Path,
      vdrEntryRef: RefVDR,
      vdrKeyLabel: String,
      vdrKeyRaw: Option[Array[Byte]],
  ) extends VDRCMD

  final case class VDRFetchEntry(
      setup: Setup,
      network: CardanoNetwork,
      workdir: Path,
      vdrEntryRef: RefVDR,
  ) extends VDRCMD

  final case class VDRProofEntry(
      setup: Setup,
      network: CardanoNetwork,
      workdir: Path,
      vdrEntryRef: RefVDR,
  ) extends VDRCMD

  sealed trait CommCMD extends CMD
  final case class CommLogin(did: DIDPrism, key: Array[Byte]) extends CommCMD

  // TODO VDR

  // TODO Event load PrismState
  // TODO Event validate Event
  // TODO Event validate Events
  // TODO Event validate Block

  sealed trait ServicesCMD extends CMD
  final case class SubmitDID(setup: Setup) extends ServicesCMD
}
