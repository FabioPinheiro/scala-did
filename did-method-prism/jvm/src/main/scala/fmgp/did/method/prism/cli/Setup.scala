package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import java.nio.file.Path
import fmgp.crypto.Secp256k1PrivateKey
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.did.method.prism.cardano.CardanoNetwork
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex

case class Key(seed: Array[Byte], derivationPath: String, key: Secp256k1PrivateKey)
object Key {
  given decoder: JsonDecoder[Key] = {
    given JsonDecoder[Array[Byte]] = Utils.decoderArrayByte
    given JsonDecoder[Secp256k1PrivateKey] =
      JsonDecoder.string.map(hex => Utils.secp256k1FromRaw(hex))
    DeriveJsonDecoder.gen[Key]
  }
  given encoder: JsonEncoder[Key] = {
    given JsonEncoder[Array[Byte]] = Utils.encoderArrayByte
    given JsonEncoder[Secp256k1PrivateKey] = JsonEncoder.string.contramap(key => bytes2Hex(key.rawBytes))
    DeriveJsonEncoder.gen[Key]
  }
}

object Utils {
  def decoderArrayByte: JsonDecoder[Array[Byte]] = JsonDecoder.string.map(hex => hex2bytes(hex))
  def encoderArrayByte: JsonEncoder[Array[Byte]] = JsonEncoder.string.contramap(bytes => bytes2Hex(bytes))

  def secp256k1FromRaw(hex: String) = Secp256k1PrivateKey(hex2bytes(hex))
}
case class StagingState(
    loadStateFileByDefault: Boolean = true,
    updateStateFileByDefault: Boolean = false,
    ssiWallet: Option[CardanoWalletConfig] = None,
    cardanoWallet: Option[CardanoWalletConfig] = None,
    seed: Option[Array[Byte]] = None,
    secp256k1PrivateKey: Map[String, Key] = Map.empty,
    blockfrostMainnet: Option[BlockfrostConfig] = None,
    blockfrostTestnet: Option[BlockfrostConfig] = None,
    blockfrostPreprod: Option[BlockfrostConfig] = None,
    blockfrostPreview: Option[BlockfrostConfig] = None,
    test: String = "",
) {
  secp256k1PrivateKey.get("")
  def blockfrost(network: CardanoNetwork): Option[BlockfrostConfig] = network match
    case CardanoNetwork.Mainnet => this.blockfrostMainnet
    case CardanoNetwork.Testnet => this.blockfrostTestnet
    case CardanoNetwork.Preprod => this.blockfrostPreprod
    case CardanoNetwork.Preview => this.blockfrostPreview
}
object StagingState {

  given decoder: JsonDecoder[StagingState] = {
    given JsonDecoder[Array[Byte]] = Utils.decoderArrayByte
    given decoder: JsonDecoder[BlockfrostConfig] = DeriveJsonDecoder.gen[BlockfrostConfig]
    DeriveJsonDecoder.gen[StagingState]
  }
  given encoder: JsonEncoder[StagingState] = {
    given JsonEncoder[Array[Byte]] = Utils.encoderArrayByte
    given encoder: JsonEncoder[BlockfrostConfig] = DeriveJsonEncoder.gen[BlockfrostConfig]
    DeriveJsonEncoder.gen[StagingState]
  }
}

case class Setup(
    stagingPath: Path,
    updateStateFile: Boolean,
    staging: Either[String, StagingState],
) {
  private def acquireRelease = Setup.acquireRelease(stagingPath.toString(), updateStateFile = updateStateFile)
  def layer = ZLayer.scoped { acquireRelease }

  def mState = staging.toOption

  def stateLen[T](f: (StagingState) => Option[T]): Option[T] = staging.toOption.flatMap(f(_))

}
object Setup {
  given decoderPath: JsonDecoder[Path] = JsonDecoder.string.map(s => Path.of(s))
  given encoderPath: JsonEncoder[Path] = JsonEncoder.string.contramap(e => e.toString())
  given decoder: JsonDecoder[Setup] = DeriveJsonDecoder.gen[Setup]
  given encoder: JsonEncoder[Setup] = DeriveJsonEncoder.gen[Setup]

  def home = java.lang.System.getProperty("user.home")
  final val defaultCOnfigPath = s"$home/.cardano-prism-config.json" // "staging.json"
  def acquireRelease(path: String = defaultCOnfigPath, updateStateFile: Boolean = true) =
    ZIO.acquireRelease {
      val ref = ZIO
        .readFile(path)
        .orDie
        .map(_.fromJson[StagingState])
        .map(state => Setup(Path.of(path), updateStateFile, state))
        .flatMap(Ref.make(_))
      ref
    } { ref =>
      ref.get.flatMap { setup =>
        setup.staging match {
          case Left(error) => ZIO.log(s"StagingState parsing error: $error")
          case Right(newState) =>
            (setup.updateStateFile || newState.updateStateFileByDefault) match
              case false => ZIO.unit // *> ZIO.log("updateStateFile = false")
              case true  => ZIO.writeFile(path, newState.toJsonPretty).orDie *> ZIO.log("StagingState close")
        }
      }
    }
}

def updateState(f: (StagingState) => StagingState): ZIO[Ref[Setup], Nothing, Unit] =
  ZIO.serviceWithZIO[Ref[Setup]](_.updateSome {
    // case s @ Setup(stagingPath, updateStateFile, Left(stagingError)) => None
    case s @ Setup(stagingPath, updateStateFile, Right(state)) =>
      Setup(stagingPath, updateStateFile, Right(f(state)))
  })

def forceStateUpdateAtEnd: ZIO[Ref[Setup], Nothing, Unit] =
  ZIO.serviceWithZIO[Ref[Setup]](_.update(_.copy(updateStateFile = true)))

def stateLen[T](f: (StagingState) => Option[T]): ZIO[Ref[Setup], Nothing, Option[T]] =
  ZIO.serviceWithZIO[Ref[Setup]](_.get.map(_.stateLen(f(_))))
