package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import scala.jdk.CollectionConverters._
import scala.util.Try

import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import fmgp.did.method.prism.CardanoService
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.util.bytes2Hex
import fmgp.did.method.prism.vdr.BlockfrostConfig

object BlockfrostCommand {

  val command: Command[Subcommand.BlockfrostSubcommand] = Command("bf")
    .withHelp("Blockfrost")
    .subcommands(
      saveTokenCommand,
      addressCommand,
    )

  def saveTokenCommand = Command(
    "token",
    Staging.options,
    networkArgs ++ Args.text("token").map(token => BlockfrostConfig(token = token))
  ).map { case (setup, (network, mBlockfrostConfig)) =>
    Subcommand.BlockfrostToken(setup, network, Some(mBlockfrostConfig))
  }

  def addressCommand = Command(
    "address",
    Staging.options ++
      walletOpt.optional.orElse(walletTypeOpt).withDefault(WalletType.Cardano) ++
      networkFlag ++
      blockfrostConfigOpt
  )
    .map { case (setup, walletOrType, network, mBlockfrostConfig) =>
      walletOrType match
        case Some(wallet)       => Subcommand.BlockfrostAddress(setup, network, mBlockfrostConfig, wallet)
        case WalletType.SSI     => Subcommand.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.SSI)
        case WalletType.Cardano => Subcommand.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.Cardano)
        case None               => Subcommand.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.Cardano)
    }

  def program(cmd: Subcommand.BlockfrostSubcommand): ZIO[Any, PrismCliError, Unit] = cmd match {
    case Subcommand.BlockfrostToken(setup, network, mBlockfrostConfig) =>
      (for {
        _ <- mBlockfrostConfig match
          case None => ZIO.logWarning("No efect")
          case Some(config) =>
            network match
              case CardanoNetwork.Mainnet => updateState(s => s.copy(blockfrostMainnet = Some(config)))
              case CardanoNetwork.Testnet => updateState(s => s.copy(blockfrostTestnet = Some(config)))
              case CardanoNetwork.Preprod => updateState(s => s.copy(blockfrostPreprod = Some(config)))
              case CardanoNetwork.Preview => updateState(s => s.copy(blockfrostPreview = Some(config)))
        _ <- forceStateUpdateAtEnd
        _ <- network match
          case CardanoNetwork.Mainnet =>
            stateLen(_.blockfrostMainnet).flatMap(t => ZIO.log(s"Token in $network is '$t'"))
          case CardanoNetwork.Testnet =>
            stateLen(_.blockfrostTestnet).flatMap(t => ZIO.log(s"Token in $network is '$t'"))
          case CardanoNetwork.Preprod =>
            stateLen(_.blockfrostPreprod).flatMap(t => ZIO.log(s"Token in $network is '$t'"))
          case CardanoNetwork.Preview =>
            stateLen(_.blockfrostPreview).flatMap(t => ZIO.log(s"Token in $network is '$t'"))
      } yield ()).provideLayer(setup.layer)
    case Subcommand.BlockfrostAddress(setup, network, mBlockfrostConfig, walletOrType) =>
      (for {
        _ <- ZIO.log("")
        // wallet <- walletOrType match {
        //   case w: CardanoWalletConfig => ZIO.succeed(w)
        //   case WalletType.SSI =>
        //     setup.mState.flatMap(_.ssiWallet) match
        //       case None => ZIO.logError("SSI Wallet not found.") *> ZIO.fail(PrismCliError("SSI Wallet not found"))
        //       case Some(ssiWallet) => ZIO.log(s"Lodding ssi wallet") *> ZIO.succeed(ssiWallet)
        //   case WalletType.Cardano =>
        //     setup.mState.flatMap(_.cardanoWallet) match
        //       case None =>
        //         ZIO.logError("Cardano Wallet not found") *> ZIO.fail(PrismCliError("SSI Wallet not found"))
        //       case Some(cardanoWallet) => ZIO.log(s"Lodding cardano wallet") *> ZIO.succeed(cardanoWallet)
        // }
        // _ <- ZIO.when(wallet.passphrase.nonEmpty)(ZIO.logError("FIXME this is ignores wallet passphrase"))
        // account = CardanoService.makeAccount(network, wallet)
        // _ <- ZIO.log("base Address:      " + account.baseAddress)
        // _ <- ZIO.log("stake Address:     " + account.stakeAddress())
        // _ <- ZIO.log("base Path:         " + account.hdKeyPair().getPath())
        // _ <- ZIO.log("stake Path:        " + account.stakeHdKeyPair().getPath())
        // _ <- ZIO.log("stake KeyHash:     " + bytes2Hex(account.stakeHdKeyPair().getPublicKey().getKeyHash()))
        // _ <- ZIO.log("Private Bytes:     " + bytes2Hex(account.hdKeyPair().getPrivateKey().getBytes()))
        // _ <- ZIO.log("Public Bytes:      " + bytes2Hex(account.hdKeyPair().getPublicKey().getBytes()))
        // _ <- ZIO.log("Private KeyData:   " + bytes2Hex(account.hdKeyPair().getPrivateKey().getKeyData()))
        // _ <- ZIO.log("Public KeyData:    " + bytes2Hex(account.hdKeyPair().getPublicKey().getKeyData()))
        // _ <- ZIO.log("Private ChainCode: " + bytes2Hex(account.hdKeyPair().getPrivateKey().getChainCode()))
        // _ <- ZIO.log("Public ChainCode:  " + bytes2Hex(account.hdKeyPair().getPublicKey().getChainCode()))
        // _ <- ZIO.log("Public KeyHash:    " + bytes2Hex(account.hdKeyPair().getPublicKey().getKeyHash()))
        // _ <- ZIO.log("Public KeyHash:    " + bytes2Hex(account.hdKeyPair().getPublicKey().getKeyHash()))
        // _ <- network match
        //   case CardanoNetwork.Mainnet => ZIO.unit
        //   case CardanoNetwork.Testnet => ZIO.log(s"Believe the $network network is deprecated")
        //   case CardanoNetwork.Preprod | CardanoNetwork.Preview =>
        //     ZIO.log(
        //       s"A $network wallet can be popup with tADA using https://docs.cardano.org/cardano-testnets/tools/faucet"
        //     )
        // _ <- Console.printLine(account.baseAddress).orDie
      } yield ()).provideLayer(setup.layer)
  }
}
