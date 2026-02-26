package fmgp.did.method.prism.cli

import zio.cli.*
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.cardano.PublicCardanoNetwork
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.did.DID
import fmgp.did.DIDSubject

// Conventions https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html
// https://www.gnu.org/software/libc/manual/html_node/Argp.html

val blockfrostConfig =
  Options
    .text("token")
    .??("Blockfrost's API token")
    .map(token => BlockfrostConfig(token = token))

val networkFlag =
  Options
    .enumeration[PublicCardanoNetwork]("network")(PublicCardanoNetwork.values.toSeq.map(e => (e.name, e))*)
    .withDefault(PublicCardanoNetwork.Mainnet)

val networkOnlineFlag =
  Options
    .enumeration[PublicCardanoNetwork]("network")(
      Seq(PublicCardanoNetwork.Mainnet, PublicCardanoNetwork.Preprod, PublicCardanoNetwork.Preview).map(e =>
        (e.name, e)
      )*
    )

val networkArgs =
  Args.enumeration[PublicCardanoNetwork]("network")(
    PublicCardanoNetwork.values.filterNot(_ == PublicCardanoNetwork.Testnet).toSeq.map(e => (e.name, e))*
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

enum WalletType { case SSIWallet extends WalletType; case AdaWallet extends WalletType }

val walletTypeOpt: Options[WalletType] = Options
  .enumeration[WalletType]("wallet-type")(("ssi", WalletType.SSIWallet), ("ada", WalletType.AdaWallet))

val didPrismArg: Args[DIDPrism] =
  Args
    .text("DID:PRISM")
    .mapOrFail(str =>
      DIDPrism.fromString(str) match
        case Left(error)     => Left(HelpDoc.p(s"Fail to parse Arg: $error"))
        case Right(didPrisn) => Right(didPrisn)
    )

val didArg: Args[DID] =
  Args
    .text("DID")
    .mapOrFail(str =>
      DIDSubject.either(str) match
        case Left(error) => Left(HelpDoc.p(s"Fail to parse Arg: $error"))
        case Right(did)  => Right(did.toDID)
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

val indexerDBConnectionAgr =
  Args
    .text("mongodb-connection")
    .mapOrFail(e => Right(e))
    .??(
      "Indexer MongoDB connection to be used as a state storage. Ex: 'mongodb+srv://user:password@cluster0.bgnyyy1.mongodb.net/indexer'"
    )

val keysLabels = Args.text("keyID").atLeast(1).??("Label/name of keys to be used")
// val TEST = Options.keyValueMap(Options.Single("aa", Vector.empty, PrimType.Text))
val didCommServiceEndpoints =
  Options
    .keyValueMap("services")
    .alias("S")
    .optional
    .map {
      case Some(map) => map
      case None      => Map.empty
    }
    .??("DIDComm service -S 'id=endpoint'")
  // .map
// (Args.text("serviceId") ++ Args.text("didCommEndpoint")).repeat.??("DIDComm service id + endpoint")
