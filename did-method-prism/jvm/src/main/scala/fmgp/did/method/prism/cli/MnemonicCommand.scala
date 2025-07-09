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
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey

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
        Command("new", Staging.options).map { (setup) => Subcommand.MnemonicCreate(setup) },
        Command("seed", Staging.options ++ walletOpt.optional).map { (setup, mWallet) =>
          Subcommand.MnemonicSeed(setup, mWallet)
        },
      )
      .orElse(
        Command(
          "key",
          Staging.options ++ walletOpt.optional ++ Options.integer("depth") ++ Options.integer("childIndex")
        )
          .withHelp("Make a private Secp256k1 key")
          .map { case (setup, mWallet, depth, childIndex) =>
            Subcommand.Mnemonic2Key(setup, mWallet, depth.toInt, childIndex.toInt)
          },
      )

  def program(cmd: Subcommand.MnemonicSubcommand): ZIO[Any, Nothing, Unit] = cmd match {
    case Subcommand.MnemonicCreate(setup: Setup) =>
      (for {
        words <- ZIO.succeed(MnemonicHelper.Companion.createRandomMnemonics().asScala.toSeq)
        _ <- updateState(s => s.copy(wallet = Some(CardanoWalletConfig(mnemonic = words, passphrase = ""))))
        _ <- Console.printLine(words.mkString(" ")).orDie
      } yield ()).provideLayer(setup.layer)

    case MnemonicSeed(setup, mWallet) => {
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

    case Mnemonic2Key(setup, mWallet, depth, childIndex) =>
      val (info, wallet) = mWallet.orElse(setup.mState.flatMap(_.wallet)) match
        case Some(wallet) => (s"Lodding wallett: $wallet", wallet)
        case None         => { val tmp = newWallet; (s"Generateing new wallet: $tmp", tmp) }
      (for {
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
        _ <- updateState { e =>
          val newMap = e.secp256k1PrivateKey.+((s"key${e.secp256k1PrivateKey.size}", key))
          e.copy(wallet = Some(wallet), seed = Some(seed), secp256k1PrivateKey = newMap)
        }
        _ <- Console.printLine(bytes2Hex(key.getEncoded())).orDie
      } yield ()).provideLayer(setup.layer)

  }

}
