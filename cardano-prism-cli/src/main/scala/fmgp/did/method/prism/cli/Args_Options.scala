package fmgp.did.method.prism.cli

import zio.cli.*
import fmgp.did.method.prism.DIDPrism
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.cardano.PublicCardanoNetwork
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.did.method.prism.proto.PrismKeyUsage
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

val networkMainnePreprodFlag = {
  type AUX = PublicCardanoNetwork.Mainnet.type | PublicCardanoNetwork.Preprod.type
  Options
    .enumeration[AUX]("network")(
      Seq[AUX](PublicCardanoNetwork.Mainnet, PublicCardanoNetwork.Preprod).map(e => (e.name, e))*
    )
    .withDefault(PublicCardanoNetwork.Mainnet)
}

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

val exportFolderArg =
  Args
    .directory("output-folder", exists = Exists.Either)
    .??("Folder to write one file per ref. Created if missing. Files are named '<ref-hex>'.")

val fromScratchFlag =
  Options
    .boolean("from-scratch")
    .??("Ignore any existing '.cursor' file and rebuild the folder from scratch (full overwrite).")

private val keyUsageByName: Map[String, PrismKeyUsage] = Map(
  "Master" -> PrismKeyUsage.MasterKeyUsage,
  "Issuing" -> PrismKeyUsage.IssuingKeyUsage,
  "Keyagreement" -> PrismKeyUsage.KeyAgreementKeyUsage,
  "Authentication" -> PrismKeyUsage.AuthenticationKeyUsage,
  "Revocation" -> PrismKeyUsage.RevocationKeyUsage,
  "Capabilityinvocation" -> PrismKeyUsage.CapabilityinvocationKeyUsage,
  "Capabilitydelegation" -> PrismKeyUsage.CapabilitydelegationKeyUsage,
  "Vdr" -> PrismKeyUsage.VdrKeyUsage,
)

val keysLabels =
  Args
    .text("keyID:Usage")
    .atLeast(1)
    .mapOrFail { entries =>
      val parsed = entries.map { entry =>
        entry.split(":", 2) match
          case Array(label, usageStr) =>
            keyUsageByName.get(usageStr) match
              case Some(u) => Right((label, u))
              case None    =>
                Left(
                  s"Unknown PrismKeyUsage '$usageStr' in '$entry'. " +
                    s"Valid: ${keyUsageByName.keys.toSeq.sorted.mkString(", ")}."
                )
          case _ =>
            Left(
              s"Key reference '$entry' is missing a usage. " +
                s"Format: 'label:Usage' (e.g. 'master:Master' 'iss:Issuing' 'auth:Authentication' 'comm:Keyagreement')."
            )
      }
      parsed.collectFirst { case Left(err) => err } match
        case Some(err) => Left(HelpDoc.p(err))
        case None      => Right(parsed.collect { case Right(v) => v })
    }
    .??(
      "Keys to use, one per arg, formatted 'label:Usage' " +
        "(e.g. 'master:Master' 'iss:Issuing' 'auth:Authentication' 'comm:Keyagreement'). " +
        s"Valid usage values: ${keyUsageByName.keys.toSeq.sorted.mkString(", ")}. " +
        "Exactly one key must declare the 'Master' usage and resolve to a secp256k1 key."
    )
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
