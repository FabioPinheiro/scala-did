package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import java.nio.file.Path
import scalus.crypto.ed25519.given
import fmgp.crypto.*
import fmgp.did.method.prism.BlockfrostConfig
import fmgp.did.method.prism.BlockfrostRyoConfig
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import fmgp.did.method.prism.cardano.PublicCardanoNetwork
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex

case class StagingState(
    loadStateFileByDefault: Boolean = true,
    updateStateFileByDefault: Boolean = false,
    ssiWallet: Option[CardanoWalletConfig] = None,
    cardanoWallet: Option[CardanoWalletConfig] = None,
    // seed: Option[Array[Byte]] = None,
    ssiPrivateKeys: Map[String, DerivedKey | PrivateKey] = Map.empty,
    blockfrostMainnet: Option[BlockfrostConfig] = None,
    blockfrostTestnet: Option[BlockfrostConfig] = None,
    blockfrostPreprod: Option[BlockfrostConfig] = None,
    blockfrostPreview: Option[BlockfrostConfig] = None,
) {
  def blockfrost(network: PublicCardanoNetwork): Option[BlockfrostConfig] = network match
    case PublicCardanoNetwork.Mainnet => this.blockfrostMainnet
    case PublicCardanoNetwork.Testnet => this.blockfrostTestnet
    case PublicCardanoNetwork.Preprod => this.blockfrostPreprod
    case PublicCardanoNetwork.Preview => this.blockfrostPreview
}
object StagingState {

  given decoder: JsonDecoder[StagingState] = {
    given JsonDecoder[DerivedKey | PrivateKey] = // unionDecoder[DerivedKey, PrivateKey]
      DerivedKey.decoder <> PrivateKey.decoder.widen[DerivedKey | PrivateKey]
    given JsonDecoder[Array[Byte]] = DerivedKey.decoderArrayByte
    given decoder: JsonDecoder[BlockfrostConfig] = DeriveJsonDecoder.gen[BlockfrostConfig]
    given ryoDecoder: JsonDecoder[BlockfrostRyoConfig] = DeriveJsonDecoder.gen[BlockfrostRyoConfig]
    DeriveJsonDecoder.gen[StagingState]
  }
  given encoder: JsonEncoder[StagingState] = {
    given JsonEncoder[DerivedKey | PrivateKey] = new JsonEncoder[DerivedKey | PrivateKey] {
      def unsafeEncode(eab: DerivedKey | PrivateKey, indent: Option[Int], out: zio.json.internal.Write): Unit =
        eab match {
          case a: DerivedKey => JsonEncoder[DerivedKey].unsafeEncode(a, indent, out)
          case b: PrivateKey => JsonEncoder[PrivateKey].unsafeEncode(b, indent, out)
        }
    }
    given JsonEncoder[Array[Byte]] = DerivedKey.encoderArrayByte
    given encoder: JsonEncoder[BlockfrostConfig] = DeriveJsonEncoder.gen[BlockfrostConfig]
    given ryoEncoder: JsonEncoder[BlockfrostRyoConfig] = DeriveJsonEncoder.gen[BlockfrostRyoConfig]
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
          case Left(error)     => ZIO.log(s"StagingState parsing error: $error")
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
