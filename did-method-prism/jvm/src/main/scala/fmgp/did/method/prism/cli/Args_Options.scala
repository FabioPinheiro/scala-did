package fmgp.did.method.prism.cli

import zio.cli.*
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.did.method.prism.cardano.CardanoWalletConfig

// Conventions https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html
// https://www.gnu.org/software/libc/manual/html_node/Argp.html

val blockfrostTokenOpt =
  Options.text("blockfrost-token").optional.??("Blockfrost's API token/key")

val networkFlag =
  Options
    .enumeration[CardanoNetwork]("network")(CardanoNetwork.values.toSeq.map(e => (e.name, e)): _*)
    .withDefault(CardanoNetwork.Mainnet)

val mnemonicWords = Options.text("mnemonic")

val mnemonicPass = Options.text("mnemonic-passphrase").withDefault("")

val walletOpt = (mnemonicWords ++ mnemonicPass).mapOrFail((mnemonicPhrase, passphrase) =>
  CardanoWalletConfig.fromMnemonicPhrase(mnemonicPhrase, passphrase) match
    case Right(v)    => Right(v)
    case Left(error) => Left(ValidationError(ValidationErrorType.InvalidValue, HelpDoc.p(error)))
)

val didArg =
  Args
    .text("DID")
    .mapOrFail(str =>
      DIDPrism.fromString(str) match
        case Left(error)     => Left(HelpDoc.p(s"Fail to parse Arg: $error"))
        case Right(didPrisn) => Right(didPrisn)
    )

val indexerWorkDirAgr =
  Args
    .directory("work-directory", exists = Exists.Yes)
    .??("Indexer file system path to be used as a state storage")
    .mapOrFail(e => Right(e))
