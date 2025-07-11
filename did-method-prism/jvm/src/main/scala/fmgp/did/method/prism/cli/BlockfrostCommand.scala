package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import scala.jdk.CollectionConverters._
import scala.util.Try

import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import fmgp.did.method.prism.CardanoService
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.did.method.prism.vdr.BlockfrostConfig
import fmgp.util.bytes2Hex

object BlockfrostCommand {

  val command: Command[CMD.BlockfrostCMD] = Command("bf")
    .withHelp("Blockfrost API")
    .subcommands(
      saveTokenCommand,
      addressCommand,
    )

  def saveTokenCommand = Command(
    "token",
    ConfigCommand.options,
    networkArgs ++ Args.text("token").map(token => BlockfrostConfig(token = token))
  ).map { case (setup, (network, mBlockfrostConfig)) =>
    CMD.BlockfrostToken(setup, network, Some(mBlockfrostConfig))
  }

  def addressCommand = Command(
    "address",
    ConfigCommand.options ++
      walletOpt.optional.orElse(walletTypeOpt).withDefault(WalletType.Cardano) ++
      networkFlag ++
      blockfrostConfigOpt
  )
    .map { case (setup, walletOrType, network, mBlockfrostConfig) =>
      walletOrType match
        case Some(wallet)       => CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, wallet)
        case WalletType.SSI     => CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.SSI)
        case WalletType.Cardano => CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.Cardano)
        case None               => CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.Cardano)
    }

  def program(cmd: CMD.BlockfrostCMD): ZIO[Any, Throwable, Unit] = cmd match {
    case CMD.BlockfrostToken(setup, network, mBlockfrostConfig) =>
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
    case CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, walletOrType) =>
      (for {
        _ <- ZIO.log(cmd.toString)
        wallet <- walletOrType match {
          case w: CardanoWalletConfig => ZIO.succeed(w)
          case WalletType.SSI =>
            setup.mState.flatMap(_.ssiWallet) match
              case None => ZIO.logError("SSI Wallet not found.") *> ZIO.fail(PrismCliError("SSI Wallet not found"))
              case Some(ssiWallet) => ZIO.log(s"Lodding ssi wallet") *> ZIO.succeed(ssiWallet)
          case WalletType.Cardano =>
            setup.mState.flatMap(_.cardanoWallet) match
              case None =>
                ZIO.logError("Cardano Wallet not found") *> ZIO.fail(PrismCliError("SSI Wallet not found"))
              case Some(cardanoWallet) => ZIO.log(s"Lodding cardano wallet") *> ZIO.succeed(cardanoWallet)
        }
        account = CardanoService.makeAccount(network, wallet)
        addresses = account.baseAddress()
        _ <- ZIO.log(s"Get addresses totals for $addresses")
        mBFConfig <- mBlockfrostConfig match
          case None         => stateLen[BlockfrostConfig](_.blockfrost(network))
          case Some(config) => ZIO.succeed(Some(config))
        _ <- ZIO.log(s"Get addresses totals for $addresses")
        totalAda <- mBFConfig match
          case None           => ZIO.fail(PrismCliError(s"Token not found ($network)"))
          case Some(bfConfig) => CardanoService.addressesTotalAda(addresses).provideEnvironment(ZEnvironment(bfConfig))
        _ <- Console.printLine(totalAda).orDie
      } yield ()).provideLayer(setup.layer)
  }
}
