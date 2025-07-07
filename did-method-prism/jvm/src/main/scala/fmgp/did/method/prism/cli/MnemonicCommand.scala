package fmgp.did.method.prism.cli

import zio.*
import zio.cli.Command
import scala.jdk.CollectionConverters._

import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import fmgp.did.method.prism.cli.Subcommand
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.did.method.prism.cli.Subcommand.MnemonicSeed
import fmgp.util.bytes2Hex
import fmgp.util.hex2bytes
import fmgp.did.method.prism.cli.Subcommand.Mnemonic2Key
import org.hyperledger.identus.apollo.derivation.HDKey
import zio.cli.Options

//val seed = MnemonicHelper.Companion.createRandomSeed("")
// val pk = HDKey(seed, 0, 0).getKMMSecp256k1PrivateKey()

object CliMnemonic extends ZIOAppDefault {
  override val run = PrismCli.cliApp.run(List("mnemonic", "new"))
} //REMOVE FIXME

object MnemonicCommand {
  private def newWallet = CardanoWalletConfig(
    mnemonic = MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq,
    passphrase = ""
  )

  val mnemonicCommand: Command[Subcommand.MnemonicSubcommand] =
    Command("mnemonic")
      .subcommands(
        Command("new").map { _ => Subcommand.MnemonicCreate() },
        Command("seed", walletOpt.optional).map { mWallet => Subcommand.MnemonicSeed(mWallet) },
      )
      .orElse(
        Command("key", walletOpt.optional ++ Options.integer("depth") ++ Options.integer("childIndex"))
          .withHelp("Make a private Secp256k1 key")
          .map { case (mWallet, depth, childIndex) => Subcommand.Mnemonic2Key(mWallet, depth.toInt, childIndex.toInt) },
      )
    //
    //   .subcommands(createCommand, updateCommand, deactivateCommand, resolveCommand)

  def mnemonicProgram(cmd: Subcommand.MnemonicSubcommand) = cmd match {
    case Subcommand.MnemonicCreate() =>
      for {
        words <- ZIO.succeed(MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq)
        _ <- Console.printLine(words.mkString(" "))
      } yield ()
    case MnemonicSeed(mWallet) => {
      val (info, wallet) = mWallet match
        case Some(wallet) => (s"Lodding wallett: $wallet", wallet)
        case None         => { val tmp = newWallet; (s"Generateing new wallet: $tmp", tmp) }
      for {
        _ <- ZIO.log(info)
        seed = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)
        _ <- Console.printLine(bytes2Hex(seed))
      } yield ()
    }
    case Mnemonic2Key(mWallet, depth, childIndex) =>
      val (info, wallet) = mWallet match
        case Some(wallet) => (s"Lodding wallett: $wallet", wallet)
        case None         => { val tmp = newWallet; (s"Generateing new wallet: $tmp", tmp) }
      for {
        _ <- ZIO.log(info)
        seed = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)
        _ <- ZIO.log(s"depth=$depth childIndex=$childIndex seed='${bytes2Hex(seed)}'")
        key = HDKey(seed, 0, 0).getKMMSecp256k1PrivateKey() // FIXME 0 0 as arg
        _ <- ZIO.log("PrivateKey raw --------- " + bytes2Hex(key.getRaw()))
        _ <- ZIO.log("PrivateKey encoded ----- " + bytes2Hex(key.getEncoded()))
        _ <- ZIO.log("PublicKey raw ---------- " + bytes2Hex(key.getPublicKey().getRaw()))
        _ <- ZIO.log("PublicKey Compressed --- " + bytes2Hex(key.getPublicKey().getCompressed()))
        _ <- ZIO.log("PublicKey CurvePoint X - " + bytes2Hex(key.getPublicKey().getCurvePoint().getX()))
        _ <- ZIO.log("PublicKey CurvePoint Y - " + bytes2Hex(key.getPublicKey().getCurvePoint().getY()))
        _ <- Console.printLine(bytes2Hex(key.getEncoded()))
      } yield ()

  }

}
