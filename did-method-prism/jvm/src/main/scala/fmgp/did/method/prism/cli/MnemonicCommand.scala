package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import scala.jdk.CollectionConverters._
import scala.util.Try

import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.util.bytes2Hex

object CliMnemonic extends ZIOAppDefault {
  override val run = PrismCli.cliApp.run(List("mnemonic", "new"))
} //REMOVE FIXME

object MnemonicCommand {
  def newWallet = CardanoWalletConfig(
    mnemonic = MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq,
    passphrase = ""
  )

  val mnemonicCommand: Command[Subcommand.MnemonicSubcommand] =
    Command("mnemonic")
      .subcommands(
        Command("new", Staging.options).map { (setup) => Subcommand.MnemonicCreate(setup) },
        Command("seed", Staging.options ++ walletOpt.optional).map { (setup, mWallet) =>
          Subcommand.MnemonicSeed(setup, mWallet)
        },
      )

  def program(cmd: Subcommand.MnemonicSubcommand): ZIO[Any, Nothing, Unit] = cmd match {
    case Subcommand.MnemonicCreate(setup: Setup) =>
      (for {
        words <- ZIO.succeed(MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq)
        _ <- updateState(s => s.copy(wallet = Some(CardanoWalletConfig(mnemonic = words, passphrase = ""))))
        _ <- Console.printLine(words.mkString(" ")).orDie
      } yield ()).provideLayer(setup.layer)

    case Subcommand.MnemonicSeed(setup, mWallet) => {
      val (info, wallet) = mWallet.orElse(setup.mState.flatMap(_.wallet)) match
        case Some(wallet) => (s"Lodding wallett: $wallet", wallet)
        case None         => { val tmp = newWallet; (s"Generateing new wallet: $tmp", tmp) }
      (for {
        _ <- ZIO.log(info)
        seed = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)
        _ <- updateState(e => e.copy(wallet = Some(wallet), seed = Some(seed)))
        _ <- Console.printLine(bytes2Hex(seed)).orDie
      } yield ()).provideLayer(setup.layer)
    }
  }
}
