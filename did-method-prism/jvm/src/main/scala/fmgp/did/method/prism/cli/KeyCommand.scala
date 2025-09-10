package fmgp.did.method.prism.cli

import scala.jdk.CollectionConverters._
import scala.util.Try

import zio.*
import zio.cli.*

import fmgp.util.bytes2Hex
import fmgp.crypto.Secp256k1PrivateKey
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey
import fmgp.did.method.prism.cardano.DIDExtra

object KeyCommand {

  val derivationPathOpt = Options
    .text("derivation-path")
    .withDefault("m/29'/29'/0'/1'/0'") // CIP-1852 m / purpose' / coin_type' / account' / role / index
    .??(
      "Key's derivation-path from Seed. From more info see https://cips.cardano.org/cip/CIP-1852. \n" +
        "The purpose' and method' constants are based on the decision in https://hyperledger-identus.github.io/docs/adrs/decisions/2023-05-16-hierarchical-deterministic-key-generation-algorithm."
    )

  val command: Command[CMD] =
    Command(
      "key",
      Options.none
        ++ ConfigCommand.optionsDefualt
        ++ walletOpt.optional
        ++ derivationPathOpt
        ++ Options.text("label").??("Key label/name. key will be save staging with that name.").optional
    )
      .withHelp("Make a private Secp256k1 key")
      .map { case (setup, mWallet, derivationPath, keyLabel) =>
        CMD.Mnemonic2Key(setup, mWallet, derivationPath, keyLabel)
      }
      .orElse(
        Command(
          "deterministic-did",
          Options.none
            ++ ConfigCommand.optionsDefualt
            ++ walletOpt.optional,
          Args.integer("DID index").??("index of the DID (starts in 0)")
        )
          .withHelp("Make Test Vector for the Deterministic PRISM DID Generation Proposal")
          .map { case ((setup, mWallet), index) =>
            CMD.Mnemonic2Key2SSITestVector(setup, mWallet, index.toInt)
          }
      )

  def program(cmd: CMD.KeyCMD): ZIO[Any, Nothing, Unit] = cmd match {
    case CMD.Mnemonic2Key(setup, mWallet, derivationPath, keyLabel) =>
      val (info, wallet) = mWallet.orElse(setup.mState.flatMap(_.ssiWallet)) match
        case Some(wallet) => (s"Lodding wallett: $wallet", wallet)
        case None         => { val tmp = MnemonicCommand.newWallet; (s"Generateing new wallet: $tmp", tmp) }
      (for {
        _ <- ZIO.log(info)
        _ <- forceStateUpdateAtEnd
        seed = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)
        _ <- ZIO.log(s"derivationPath=$derivationPath keyLabel=$keyLabel seed='${bytes2Hex(seed)}'")
        hdkey <- Try(HDKey(seed, 0, 0).derive(derivationPath))
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
              Key(seed = seed, derivationPath = derivationPath, key = Secp256k1PrivateKey(key.getEncoded()))
            )
          )
          stagingState.copy(ssiWallet = Some(wallet), seed = Some(seed), secp256k1PrivateKey = keys)
        }
        _ <- Console.printLine(bytes2Hex(key.getEncoded())).orDie
      } yield ()).provideLayer(setup.layer)
    case CMD.Mnemonic2Key2SSITestVector(setup, mWallet, index) =>
      val (info, wallet) = mWallet.orElse(setup.mState.flatMap(_.ssiWallet)) match
        case Some(wallet) => (s"Lodding wallett: $wallet", wallet)
        case None         => { val tmp = MnemonicCommand.newWallet; (s"Generateing new wallet: $tmp", tmp) }
      (for {
        _ <- Console.printLine("# Deterministic PRISM DID")
        seed = MnemonicHelper.Companion.createSeed(wallet.mnemonic.asJava, wallet.passphrase)
        _ <- Console.printLine(s"mnemonic= ${wallet.mnemonic.mkString(" ")}")
        _ <- Console.printLine(s"seed='${bytes2Hex(seed)}'")
        master0derivationPath = s"m/29'/29'/$index'/1'/0'"
        master1derivationPath = s"m/29'/29'/$index'/1'/1'"
        vdr0derivationPath = s"m/29'/29'/$index'/8'/0'"
        _ <- Console.printLine(s"derivationPath=$master0derivationPath")
        masterkey <- Try(HDKey(seed, 0, 0).derive(master0derivationPath))
          .map(ZIO.succeed(_))
          .recover { ex =>
            ex.printStackTrace();
            ZIO.fail(ex)
          }
          .get
        key = masterkey.getKMMSecp256k1PrivateKey()
        _ <- Console.printLine("PrivateKey=" + bytes2Hex(key.getRaw()))
        _ <- Console.printLine("PublicKey=" + bytes2Hex(key.getPublicKey().getCompressed()))

        _ <- Try(HDKey(seed, 0, 0).derive(master1derivationPath))
          .map(ZIO.succeed(_))
          .recover { ex =>
            ex.printStackTrace();
            ZIO.fail(ex)
          }
          .get
          .flatMap { k =>
            Console.printLine(s"derivationPath=$master1derivationPath") *>
              Console.printLine("PrivateKey=" + bytes2Hex(k.getKMMSecp256k1PrivateKey().getRaw()))
          }
        _ <- Try(HDKey(seed, 0, 0).derive(vdr0derivationPath))
          .map(ZIO.succeed(_))
          .recover { ex =>
            ex.printStackTrace();
            ZIO.fail(ex)
          }
          .get
          .flatMap { k =>
            Console.printLine(s"derivationPath=$vdr0derivationPath") *>
              Console.printLine("PrivateKey=" + bytes2Hex(k.getKMMSecp256k1PrivateKey().getRaw()))
          }
        (didPrism, signedPrismEvent) = DIDExtra.createDID(
          masterKeys = Seq(("master", Secp256k1PrivateKey(key.getRaw()))),
          vdrKeys = Seq.empty,
        )
        _ <- Console.printLine(s"SSI: ${didPrism.string}")
        _ <- Console.printLine(s"Protobuf: ${bytes2Hex(signedPrismEvent.toByteArray)}")
        _ <- Console.printLine("You can you inspect the Protobuf in https://protobuf-decoder.netlify.app/")
      } yield ()).orDie
  }
}
