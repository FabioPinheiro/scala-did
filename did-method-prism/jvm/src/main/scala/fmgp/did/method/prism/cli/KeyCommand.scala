package fmgp.did.method.prism.cli

import scala.jdk.CollectionConverters._
import scala.util.Try

import zio.*
import zio.cli.*

import fmgp.util.bytes2Hex
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey

object KeyCommand {

  val derivationPathOpt = Options
    .text("derivation-path")
    .withDefault("m/551'/21325'/0'/8'/0'") // CIP-1852 m / purpose' / coin_type' / account' / role / index
    .??(
      "Key's derivation-path from Seed. From more info see https://cips.cardano.org/cip/CIP-1852."
        + "\n" + "TODO: Make a CIP fpr propose  'm / purpose / ssi-method / ssi-index / key-usage / key-index'."
        + "\n" + "      Something like m/551'/21325'/0'/8'/0'  551-> looks like 'ssi'; 21325->Prism label; 8-> protobuf's KeyUsage for VDR-KEY."
        + "\n" + "      Or with 'purpose' 737369 -> 01110011 01110011 01101001 -> 'ssi' (0x737369)"
        + "\n" + "      Or with 'purpose' 26980  ->          01101001 01100100 -> 'id'    (0x6964)"
    )

  val command: Command[Subcommand] =
    Command(
      "key",
      Options.none
        ++ Staging.options
        ++ walletOpt.optional
        ++ derivationPathOpt
        ++ Options.text("label").??("Key label/name. key will be save staging with that name.").optional
      // ++ Options.integer("depth").map(_.toInt).withDefault(0)
      // ++ Options.integer("childIndex").map(_.toInt).withDefault(0)
    )
      .withHelp("Make a private Secp256k1 key")
      .map { case (setup, mWallet, derivationPath, keyLabel) =>
        Subcommand.Mnemonic2Key(setup, mWallet, derivationPath, keyLabel)
      }

  def program(cmd: Subcommand.Mnemonic2Key): ZIO[Any, Nothing, Unit] = cmd match {
    case Subcommand.Mnemonic2Key(setup, mWallet, derivationPath, keyLabel) =>
      val (info, wallet) = mWallet.orElse(setup.mState.flatMap(_.ssiWallet)) match
        case Some(wallet) => (s"Lodding wallett: $wallet", wallet)
        case None         => { val tmp = MnemonicCommand.newWallet; (s"Generateing new wallet: $tmp", tmp) }
      (for {
        _ <- ZIO.log(info)
        _ <- forceStateUpdateAtEnd
        seed = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)
        _ <- ZIO.log(s"derivationPath=$derivationPath keyLabel=$keyLabel seed='${bytes2Hex(seed)}'")
        hdkey <- Try(HDKey(seed, 0, 0).derive(derivationPath)) // "m/1852'/21325'/0'/8'/0'"
          .map(ZIO.succeed(_))
          .recover { ex =>
            ex.printStackTrace();
            ZIO.fail(ex)
          }
          .get
          .orDie // FIXME
        _ <- ZIO.log("seed: ------------------ " + bytes2Hex(seed))
        _ <- ZIO.log("getChainCode: ---------- " + bytes2Hex(hdkey.getChainCode()))
        key = hdkey.getKMMSecp256k1PrivateKey()
        _ <- ZIO.log("PrivateKey raw --------- " + bytes2Hex(key.getRaw()))
        _ <- ZIO.log("PrivateKey encoded ----- " + bytes2Hex(key.getEncoded()))
        _ <- ZIO.log("PublicKey raw ---------- " + bytes2Hex(key.getPublicKey().getRaw()))
        _ <- ZIO.log("PublicKey Compressed --- " + bytes2Hex(key.getPublicKey().getCompressed()))
        _ <- ZIO.log("PublicKey CurvePoint X - " + bytes2Hex(key.getPublicKey().getCurvePoint().getX()))
        _ <- ZIO.log("PublicKey CurvePoint Y - " + bytes2Hex(key.getPublicKey().getCurvePoint().getY()))
        _ <- updateState { stagingState =>
          val keys = stagingState.secp256k1PrivateKey.+(
            (
              keyLabel.getOrElse(s"key${stagingState.secp256k1PrivateKey.size}"),
              Key(seed = seed, derivationPath = derivationPath, key = key)
            )
          )
          stagingState.copy(ssiWallet = Some(wallet), seed = Some(seed), secp256k1PrivateKey = keys)
        }
        _ <- Console.printLine(bytes2Hex(key.getEncoded())).orDie
      } yield ()).provideLayer(setup.layer)

  }
}
