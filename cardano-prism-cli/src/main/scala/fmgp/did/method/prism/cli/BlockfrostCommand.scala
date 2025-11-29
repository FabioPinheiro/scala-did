package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import scala.jdk.CollectionConverters.*
import scala.util.Try

import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.CardanoService
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.did.method.prism.cardano.PublicCardanoNetwork
import fmgp.did.method.prism.cardano.TxHash
import fmgp.did.method.prism.cli.CMD.BlockfrostSubmitEvents
import fmgp.did.method.prism.proto.tryParseFrom
import proto.prism.PrismEvent
import proto.prism.SignedPrismEvent
import fmgp.util.bytes2Hex
import fmgp.util.hex2bytes

object BlockfrostCommand {

  val command: Command[CMD.BlockfrostCMD] = Command("bf")
    .withHelp("Blockfrost API")
    .subcommands(
      saveTokenCommand,
      addressCommand,
      submitCommand,
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
      blockfrostConfig.optional
  )
    .map { case (setup, walletOrType, network, mBlockfrostConfig) =>
      walletOrType match
        case Some(wallet)       => CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, wallet)
        case WalletType.SSI     => CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.SSI)
        case WalletType.Cardano => CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.Cardano)
        case None               => CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, WalletType.Cardano)
    }

  def eventArg = Args
    .text("event")
    .atLeast(1)
    .??("hex of protobufs of PRISM signed events")
    .mapOrFail { hexList =>
      hexList
        .map { hex => Right(hex2bytes(hex)) }
        .map { case Right(bytes) => SignedPrismEvent.tryParseFrom(bytes) }
        .zipWithIndex
        .foldLeft[Either[HelpDoc, Seq[SignedPrismEvent]]](Right(Seq.empty)) {
          case (Right(acc), (Right(value), index))   => Right(acc :+ value)
          case (Right(acc), (Left(errorMsh), index)) =>
            Left(HelpDoc.p(s"Fail to parse the Signed Prism Event ($index): $errorMsh"))
          case (Left(acc), (Right(value), index))   => Left(acc)
          case (Left(acc), (Left(errorMsh), index)) =>
            Left(acc + HelpDoc.p(s"Fail to parse the Signed Prism Event ($index) : $errorMsh"))
        }
    }

  def submitCommand = Command(
    "submit",
    ConfigCommand.options ++ networkOnlineFlag,
    eventArg
  ).map { case ((setup, network), events) => CMD.BlockfrostSubmitEvents(setup, network, events) }

  def program(cmd: CMD.BlockfrostCMD): ZIO[Any, Throwable, Unit] = cmd match {
    case CMD.BlockfrostToken(setup, network, mBlockfrostConfig) =>
      (for {
        _ <- mBlockfrostConfig match
          case None         => ZIO.logWarning("No efect")
          case Some(config) =>
            network match
              case PublicCardanoNetwork.Mainnet => updateState(s => s.copy(blockfrostMainnet = Some(config)))
              case PublicCardanoNetwork.Testnet => updateState(s => s.copy(blockfrostTestnet = Some(config)))
              case PublicCardanoNetwork.Preprod => updateState(s => s.copy(blockfrostPreprod = Some(config)))
              case PublicCardanoNetwork.Preview => updateState(s => s.copy(blockfrostPreview = Some(config)))
        _ <- forceStateUpdateAtEnd
        _ <- network match
          case PublicCardanoNetwork.Mainnet =>
            stateLen(_.blockfrostMainnet).flatMap(t => ZIO.log(s"Token in $network is '$t'"))
          case PublicCardanoNetwork.Testnet =>
            stateLen(_.blockfrostTestnet).flatMap(t => ZIO.log(s"Token in $network is '$t'"))
          case PublicCardanoNetwork.Preprod =>
            stateLen(_.blockfrostPreprod).flatMap(t => ZIO.log(s"Token in $network is '$t'"))
          case PublicCardanoNetwork.Preview =>
            stateLen(_.blockfrostPreview).flatMap(t => ZIO.log(s"Token in $network is '$t'"))
      } yield ()).provideLayer(setup.layer)
    case CMD.BlockfrostAddress(setup, network, mBlockfrostConfig, walletOrType) =>
      (for {
        _ <- ZIO.log(cmd.toString)
        wallet <- walletOrType match {
          case w: CardanoWalletConfig => ZIO.succeed(w)
          case WalletType.SSI         =>
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
    case cmd: BlockfrostSubmitEvents =>
      for {
        txId <- submitProgram(cmd).provideLayer(cmd.setup.layer)
        _ <- Console.printLine(txId)
      } yield ()
  }

  def submitProgram(cmd: BlockfrostSubmitEvents): ZIO[Ref[Setup], Throwable, TxHash] = {
    cmd match
      case BlockfrostSubmitEvents(setup, network, events) =>
        for {
          _ <- ZIO.logError(s"CMD: $cmd") // TODO
          bfConfig <- {
            network match
              case PublicCardanoNetwork.Mainnet => stateLen(_.blockfrostMainnet)
              case PublicCardanoNetwork.Testnet => ZIO.fail(PrismCliError("Testnet is no longer available"))
              case PublicCardanoNetwork.Preprod => stateLen(_.blockfrostPreprod)
              case PublicCardanoNetwork.Preview => stateLen(_.blockfrostPreview)
          }.flatMap {
            case None    => ZIO.fail(PrismCliError(s"BlockFrost config is not available for the $network network"))
            case Some(e) => ZIO.succeed(e)
          }
          wallet <- stateLen(e => e.cardanoWallet)
            .flatMap {
              case None    => ZIO.fail(PrismCliError(s"Cardano Wallet is not setup"))
              case Some(e) => ZIO.succeed(e)
            }
          tx = CardanoService.makeTrasation(
            bfConfig = bfConfig,
            wallet = wallet,
            prismEvents = events,
            maybeMsgCIP20 = Some("cardano-prism cli"),
          )
          txHash <-
            CardanoService
              .submitTransaction(tx)
              .provideEnvironment(ZEnvironment(bfConfig))
          // else ZIO.succeed(bytes2Hex(tx.serialize))
        } yield (txHash)
  }
}
