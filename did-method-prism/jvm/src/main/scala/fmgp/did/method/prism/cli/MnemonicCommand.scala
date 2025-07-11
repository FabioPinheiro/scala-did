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

object MnemonicCommand {
  def newWallet = CardanoWalletConfig(
    mnemonic = MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq,
    passphrase = ""
  )

  val command: Command[CMD.MnemonicCMD] =
    Command("mnemonic")
      .subcommands(
        Command("new", ConfigCommand.options ++ walletTypeOpt.withDefault(WalletType.SSI)).map { (setup, walletType) =>
          CMD.MnemonicCreate(setup, walletType)
        },
        Command("seed", ConfigCommand.options ++ mnemonicWords.optional).map { (setup, mWallet) =>
          CMD.MnemonicSeed(setup, mWallet)
        },
        Command(
          "address",
          ConfigCommand.options ++
            walletOpt.optional.orElse(walletTypeOpt).withDefault(WalletType.Cardano) ++
            networkFlag
        )
          .map { case (setup, walletOrType, network) =>
            walletOrType match
              case Some(wallet)       => CMD.MnemonicAddress(setup, wallet, network)
              case WalletType.SSI     => CMD.MnemonicAddress(setup, WalletType.SSI, network)
              case WalletType.Cardano => CMD.MnemonicAddress(setup, WalletType.Cardano, network)
              case None               => CMD.MnemonicAddress(setup, WalletType.Cardano, network) // D
          },
      )

  def program(cmd: CMD.MnemonicCMD): ZIO[Any, PrismCliError, Unit] = cmd match {
    case CMD.MnemonicCreate(setup: Setup, walletType: WalletType) =>
      (for {
        words <- ZIO.succeed(MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq)
        newWallet = CardanoWalletConfig(mnemonic = words, passphrase = "")
        _ <- walletType match {
          case WalletType.SSI     => updateState(s => s.copy(ssiWallet = Some(newWallet)))
          case WalletType.Cardano => updateState(s => s.copy(cardanoWallet = Some(newWallet)))
        }
        _ <- Console.printLine(words.mkString(" ")).orDie
      } yield ()).provideLayer(setup.layer)

    case CMD.MnemonicSeed(setup, mWallet) => {
      val (info, wallet) = mWallet.orElse(setup.mState.flatMap(_.ssiWallet)) match
        case Some(wallet) => (s"Lodding ssi wallet: $wallet", wallet)
        case None         => { val tmp = newWallet; (s"Generateing new wallet: $tmp", tmp) }
      (for {
        _ <- ZIO.log(info)
        seed = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)
        _ <- updateState(e => e.copy(ssiWallet = Some(wallet), seed = Some(seed)))
        _ <- Console.printLine(bytes2Hex(seed)).orDie
      } yield ()).provideLayer(setup.layer)
    }

    case CMD.MnemonicAddress(setup, walletOrType, network) =>
      // import com.bloxbean.cardano.client.account.Account
      // import com.bloxbean.cardano.client.common.model.Networks
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
        _ <- ZIO.when(wallet.passphrase.nonEmpty)(ZIO.logError("FIXME this is ignores wallet passphrase"))
        account = CardanoService.makeAccount(network, wallet)
        _ <- ZIO.log("base Address:      " + account.baseAddress)
        _ <- ZIO.log("stake Address:     " + account.stakeAddress())
        _ <- ZIO.log("base Path:         " + account.hdKeyPair().getPath())
        _ <- ZIO.log("stake Path:        " + account.stakeHdKeyPair().getPath())
        _ <- ZIO.log("stake KeyHash:     " + bytes2Hex(account.stakeHdKeyPair().getPublicKey().getKeyHash()))
        _ <- ZIO.log("Private Bytes:     " + bytes2Hex(account.hdKeyPair().getPrivateKey().getBytes()))
        _ <- ZIO.log("Public Bytes:      " + bytes2Hex(account.hdKeyPair().getPublicKey().getBytes()))
        _ <- ZIO.log("Private KeyData:   " + bytes2Hex(account.hdKeyPair().getPrivateKey().getKeyData()))
        _ <- ZIO.log("Public KeyData:    " + bytes2Hex(account.hdKeyPair().getPublicKey().getKeyData()))
        _ <- ZIO.log("Private ChainCode: " + bytes2Hex(account.hdKeyPair().getPrivateKey().getChainCode()))
        _ <- ZIO.log("Public ChainCode:  " + bytes2Hex(account.hdKeyPair().getPublicKey().getChainCode()))
        _ <- ZIO.log("Public KeyHash:    " + bytes2Hex(account.hdKeyPair().getPublicKey().getKeyHash()))
        _ <- ZIO.log("Public KeyHash:    " + bytes2Hex(account.hdKeyPair().getPublicKey().getKeyHash()))
        _ <- network match
          case CardanoNetwork.Mainnet => ZIO.unit
          case CardanoNetwork.Testnet => ZIO.log(s"Believe the $network network is deprecated")
          case CardanoNetwork.Preprod | CardanoNetwork.Preview =>
            ZIO.log(
              s"A $network wallet can be popup with tADA using https://docs.cardano.org/cardano-testnets/tools/faucet"
            )
        _ <- Console.printLine(account.baseAddress).orDie
      } yield ()).provideLayer(setup.layer)
  }
}
