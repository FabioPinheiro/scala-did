package fmgp.did.method.prism.cli

import zio.cli.*
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.did.method.prism.cardano.CardanoWalletConfig

// Conventions https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html
// https://www.gnu.org/software/libc/manual/html_node/Argp.html

val blockfrostConfigOpt =
  Options
    .text("token")
    .optional
    .??("Blockfrost's API token")
    .map(_.map(token => BlockfrostConfig(token = token)))

val networkFlag =
  Options
    .enumeration[CardanoNetwork]("network")(CardanoNetwork.values.toSeq.map(e => (e.name, e)): _*)
    .withDefault(CardanoNetwork.Mainnet)

val networkOnlineFlag =
  Options
    .enumeration[CardanoNetwork]("network")(
      Seq(CardanoNetwork.Mainnet, CardanoNetwork.Preprod, CardanoNetwork.Preview).map(e => (e.name, e)): _*
    )

val networkArgs =
  Args.enumeration[CardanoNetwork]("network")(
    CardanoNetwork.values.filterNot(_ == CardanoNetwork.Testnet).toSeq.map(e => (e.name, e)): _*
  )

val mnemonicWords = Options
  .text("mnemonic")
  .mapOrFail(mnemonicPhrase =>
    CardanoWalletConfig.fromMnemonicPhrase(mnemonicPhrase, passphrase = "") match
      case Right(v)    => Right(v)
      case Left(error) => Left(ValidationError(ValidationErrorType.InvalidValue, HelpDoc.p(error)))
  )

val mnemonicPass = Options.text("mnemonic-passphrase").withDefault("")

val walletOpt = (mnemonicWords ++ mnemonicPass).map { (walletWithNoPass, passphrase) =>
  walletWithNoPass.copy(passphrase = passphrase)
}

enum WalletType { case SSI extends WalletType; case Cardano extends WalletType }

val walletTypeOpt: Options[WalletType] = Options
  .enumeration[WalletType]("wallet-type")(("ssi", WalletType.SSI), ("cardano", WalletType.Cardano))

val didArg =
  Args
    .text("DID")
    .mapOrFail(str =>
      DIDPrism.fromString(str) match
        case Left(error)     => Left(HelpDoc.p(s"Fail to parse Arg: $error"))
        case Right(didPrisn) => Right(didPrisn)
    )

val indexerWorkDirOpt =
  Options
    .directory("work-directory", exists = Exists.Yes)
    .??("Indexer file system path to be used as a state storage")
    .mapOrFail(e => Right(e))

val indexerWorkDirAgr =
  Args
    .directory("work-directory", exists = Exists.Yes)
    .??("Indexer file system path to be used as a state storage")
    .mapOrFail(e => Right(e))
