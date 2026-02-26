package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import scala.jdk.CollectionConverters.*
import scala.util.Try

import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.*
import fmgp.util.bytes2Hex

object MnemonicCommand {
  def newWallet = CardanoWalletConfig(
    mnemonic = MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq,
    passphrase = ""
  )

  val command: Command[CMD.MnemonicCMD] =
    Command("mnemonic")
      .subcommands(
        Command("new", ConfigCommand.options ++ walletTypeOpt.withDefault(WalletType.SSIWallet))
          .map { (setup, walletType) =>
            CMD.MnemonicCreate(setup, walletType)
          },
        Command("seed", ConfigCommand.options ++ mnemonicWords.optional)
          .map { (setup, mWallet) =>
            CMD.MnemonicSeed(setup, mWallet)
          },
        Command(
          "address",
          ConfigCommand.options ++
            walletOpt.optional.orElse(walletTypeOpt).withDefault(WalletType.AdaWallet) ++
            networkFlag
        )
          .map { case (setup, walletOrType, network) =>
            walletOrType match
              case Some(wallet)         => CMD.MnemonicAddress(setup, wallet, network)
              case WalletType.SSIWallet => CMD.MnemonicAddress(setup, WalletType.SSIWallet, network)
              case WalletType.AdaWallet => CMD.MnemonicAddress(setup, WalletType.AdaWallet, network)
              case None                 => CMD.MnemonicAddress(setup, WalletType.AdaWallet, network)
          },
      )

  def program(cmd: CMD.MnemonicCMD): ZIO[Any, PrismCliError, Unit] = cmd match {
    case CMD.MnemonicCreate(setup: Setup, walletType: WalletType) =>
      (for {
        words <- ZIO.succeed(MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq)
        newWallet = CardanoWalletConfig(mnemonic = words, passphrase = "")
        _ <- walletType match {
          case WalletType.SSIWallet => updateState(s => s.copy(ssiWallet = Some(newWallet)))
          case WalletType.AdaWallet => updateState(s => s.copy(cardanoWallet = Some(newWallet)))
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
        _ <- updateState(e => e.copy(ssiWallet = Some(wallet)))
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
          case WalletType.SSIWallet   =>
            setup.mState.flatMap(_.ssiWallet) match
              case None => ZIO.logError("SSI Wallet not found.") *> ZIO.fail(PrismCliError("SSI Wallet not found"))
              case Some(ssiWallet) => ZIO.log(s"Lodding ssi wallet") *> ZIO.succeed(ssiWallet)
          case WalletType.AdaWallet =>
            setup.mState.flatMap(_.cardanoWallet) match
              case None =>
                ZIO.logError("Cardano Wallet not found") *> ZIO.fail(PrismCliError("SSI Wallet not found"))
              case Some(cardanoWallet) => ZIO.log(s"Lodding cardano wallet") *> ZIO.succeed(cardanoWallet)
        }
        _ <- ZIO.when(wallet.passphrase.nonEmpty)(ZIO.logError("FIXME this is ignores wallet passphrase"))
        address = network.match
          case PublicCardanoNetwork.Mainnet => wallet.addressMainnet()
          case PublicCardanoNetwork.Testnet => wallet.addressTestnet()
          case PublicCardanoNetwork.Preprod => wallet.addressTestnet()
          case PublicCardanoNetwork.Preview => wallet.addressTestnet()
        _ <- ZIO.log("base Address:        " + address.encode.get)
        _ <- ZIO.log("base Address Bech32: " + address.toBech32.get)
        _ <- ZIO.log("stake KeyHash:       " + wallet.account().stakeKeyHash)
        _ <- ZIO.log("stake Public kye:   " + bytes2Hex(wallet.account().stakeKeyPair.verificationKey.bytes))
        // _ <- ZIO.log("stake Private kye:  " + bytes2Hex(wallet.account().stakeKeyPair.privateKeyBytes))
        _ <- ZIO.log("payment KeyHash:     " + wallet.account().paymentKeyHash)
        _ <- ZIO.log("payment Public kye:   " + bytes2Hex(wallet.account().paymentKeyPair.verificationKey.bytes))
        // _ <- ZIO.log("payment Private kye:  " + bytes2Hex(wallet.account().paymentKeyPair.privateKeyBytes))
        _ <- ZIO.log("change Public kye:   " + bytes2Hex(wallet.account().changeKeyPair.verificationKey.bytes))
        // _ <- ZIO.log("change Private kye:  " + bytes2Hex(wallet.account().changeKeyPair.privateKeyBytes))
        _ <- network match
          case PublicCardanoNetwork.Mainnet => ZIO.unit
          case PublicCardanoNetwork.Testnet => ZIO.log(s"Believe the $network network is deprecated")
          case PublicCardanoNetwork.Preprod | PublicCardanoNetwork.Preview =>
            ZIO.log(
              s"A $network wallet can be popup with tADA using https://docs.cardano.org/cardano-testnets/tools/faucet"
            )
        _ <- Console.printLine(address.encode.get).orDie
      } yield ()).provideLayer(setup.layer)
  }
}
