package fmgp.did.method.prism.cli

import scala.jdk.CollectionConverters.*
import scala.util.Try

import zio.*
import zio.cli.*

import fmgp.util.bytes2Hex
import fmgp.crypto.Secp256k1PrivateKey
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.apollo.derivation.HDKey
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.cardano.DIDExtra
import fmgp.did.method.prism.proto.PrismKeyUsage
import fmgp.did.method.prism.cli.CMD.KeyEd25519FromMnemonic
import fmgp.did.method.prism.cli.CMD.Keysepc256k1FromMnemonic
import fmgp.util.hex2bytes

object KeyCommand {

  val derivationPathOpt = Options
    .text("derivation-path")
    .withDefault("m/29'/29'/0'/1'/0'") // CIP-1852 m / purpose' / coin_type' / account' / role / index
    .??(
      "Key's derivation-path from Seed. From more info see https://cips.cardano.org/cip/CIP-1852. \n" +
        "The purpose' and method' constants are based on the decision in https://hyperledger-identus.github.io/docs/adrs/decisions/2023-05-16-hierarchical-deterministic-key-generation-algorithm."
    )

  val didIndexAgr = Args.integer.??("DID account index")
  val keyIndexAgr = Args.integer.??("Account key index")
  val keyUsageAgr = Args
    .enumeration[PrismKeyUsage]("keyUsage")(
      ("Master", PrismKeyUsage.MasterKeyUsage),
      ("Issuing", PrismKeyUsage.IssuingKeyUsage),
      ("Keyagreement", PrismKeyUsage.KeyAgreementKeyUsage),
      ("Authentication", PrismKeyUsage.AuthenticationKeyUsage),
      ("Revocation", PrismKeyUsage.RevocationKeyUsage),
      ("Capabilityinvocation", PrismKeyUsage.CapabilityinvocationKeyUsage),
      ("Capabilitydelegation", PrismKeyUsage.CapabilitydelegationKeyUsage),
      ("Vdr", PrismKeyUsage.VdrKeyUsage),
    )
    .??("Account key index")

  val command: Command[CMD] =
    Command("key", Options.none ++ ConfigCommand.optionsDefualt)
      .subcommands(
        Command(
          "Ed25519",
          Options.text("label").??("Key label/name. key will be save staging with that name.").optional,
          didIndexAgr ++ keyUsageAgr ++ keyIndexAgr
        )
          .map { case (mLabel, (didIndex, keyUsage, keyIndex)) =>
            (setup: Setup) =>
              val wallet = setup.stateLen(_.ssiWallet).get // FIXME
              CMD.KeyEd25519FromMnemonic(
                setup = setup,
                wallet,
                didIndex = didIndex.toInt,
                keyUsage = keyUsage,
                keyIndex = keyIndex.toInt,
                keyLabel = mLabel.getOrElse(s"key-$didIndex-${keyUsage.name}-$keyIndex")
                // getOrElse(s"key${stagingState.privateKeys.size}"
              )
          },
        Command(
          "sepc256k1",
          Options.text("label").??("Key label/name. key will be save staging with that name.").optional,
          didIndexAgr ++ keyUsageAgr ++ keyIndexAgr
        )
          .map { case (mLabel, (didIndex, keyUsage, keyIndex)) =>
            (setup: Setup) =>
              val wallet = setup.stateLen(_.ssiWallet).get // FIXME
              CMD.Keysepc256k1FromMnemonic(
                setup = setup,
                wallet,
                didIndex = didIndex.toInt,
                keyUsage = keyUsage,
                keyIndex = keyIndex.toInt,
                keyLabel = mLabel.getOrElse(s"key-$didIndex-${keyUsage.name}-$keyIndex"),
                // getOrElse(s"key${stagingState.privateKeys.size}"

              )
          },
        Command(
          "derivation-path",
          Options.none
            ++ walletOpt.optional
            ++ derivationPathOpt
            ++ Options.text("label").??("Key label/name. key will be save staging with that name.").optional
        )
          .withHelp("Make a private Secp256k1 key")
          .map { case (mWallet, derivationPath, keyLabel) =>
            (setup: Setup) => CMD.Mnemonic2Key(setup, mWallet, derivationPath, keyLabel)
          },
        Command(
          "did-deterministic",
          Options.none ++ walletOpt.optional,
          Args.integer("DID index").??("index of the DID (starts in 0)")
        )
          .withHelp("Make Test Vector for the Deterministic PRISM DID Generation Proposal")
          .map { case (mWallet, index) =>
            (setup: Setup) => CMD.Mnemonic2Key2SSITestVector(setup, mWallet, index.toInt)
          }
      )
      .map { case (setup, f) => f(setup) }

  def program(cmd: CMD.KeyCMD): ZIO[Any, Nothing, Unit] = cmd match {
    case KeyEd25519FromMnemonic(setup, ssiWallet, didIndex, keyUsage, keyIndex, keyLabel) =>
      (for {
        _ <- ZIO.unit
        path = Cip0000.didPath(didIndex, keyUsage, keyIndex)
        key = ssiWallet.ed25519DerivePrism(didIndex, keyUsage, keyIndex)
        keyBytes = key.extendedKey.extendedSecretKey
        _ <- forceStateUpdateAtEnd
        _ <- updateState { stagingState =>
          stagingState.copy(ssiPrivateKeys =
            stagingState.ssiPrivateKeys.+(keyLabel -> KeyEd25519(derivationPath = path, key = key))
          )
        }
        text = s"Key Ed25519 From Mnemonic with path $path '${bytes2Hex(keyBytes)}'"
        _ <- ZIO.log(text)
        _ <- Console.printLine(text)
      } yield ()).provideLayer(cmd.setup.layer).orDie

    case Keysepc256k1FromMnemonic(setup, ssiWallet, didIndex, keyUsage, keyIndex, keyLabel) =>
      (for {
        _ <- ZIO.unit
        path = Cip0000.didPath(didIndex, keyUsage, keyIndex)
        key = ssiWallet.secp256k1DerivePrism(didIndex, keyUsage, keyIndex)
        // key.compressedPublicKey
        keyBytes = key.rawBytes
        _ <- forceStateUpdateAtEnd
        _ <- updateState { stagingState =>
          stagingState.copy(ssiPrivateKeys =
            stagingState.ssiPrivateKeys.+(keyLabel -> KeySecp256k1(derivationPath = path, key = key))
          )
        }
        text = s"Key sepc256k1 From Mnemonic with path $path '${bytes2Hex(keyBytes)}'"
        _ <- ZIO.log(text)
        _ <- Console.printLine(text)
      } yield ()).provideLayer(cmd.setup.layer).orDie

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
          val keys = stagingState.ssiPrivateKeys.+(
            (
              keyLabel.getOrElse(s"key${stagingState.ssiPrivateKeys.size}"),
              Key(derivationPath = derivationPath, key = Secp256k1PrivateKey(key.getEncoded()))
            )
          )
          stagingState.copy(ssiWallet = Some(wallet), ssiPrivateKeys = keys)
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
