package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import java.nio.file.Path
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex
import fmgp.did.method.prism.vdr.BlockfrostConfig

case class Key(seed: Array[Byte], derivationPath: String, key: KMMECSecp256k1PrivateKey)
object Key {
  given decoder: JsonDecoder[Key] = {
    given JsonDecoder[Array[Byte]] = Utils.decoderArrayByte
    given JsonDecoder[KMMECSecp256k1PrivateKey] =
      JsonDecoder.string.map(hex => Utils.secp256k1FromRaw(hex))
    DeriveJsonDecoder.gen[Key]
  }
  given encoder: JsonEncoder[Key] = {
    given JsonEncoder[Array[Byte]] = Utils.encoderArrayByte
    given JsonEncoder[KMMECSecp256k1PrivateKey] = JsonEncoder.string.contramap(key => bytes2Hex(key.getEncoded()))
    DeriveJsonEncoder.gen[Key]
  }
}

object Utils {
  def decoderArrayByte: JsonDecoder[Array[Byte]] = JsonDecoder.string.map(hex => hex2bytes(hex))
  def encoderArrayByte: JsonEncoder[Array[Byte]] = JsonEncoder.string.contramap(bytes => bytes2Hex(bytes))

  def secp256k1FromRaw(hex: String) = KMMECSecp256k1PrivateKey.Companion.secp256k1FromByteArray(hex2bytes(hex))
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
)
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
}
object Setup {
  given decoderPath: JsonDecoder[Path] = JsonDecoder.string.map(s => Path.of(s))
  given encoderPath: JsonEncoder[Path] = JsonEncoder.string.contramap(e => e.toString())
  given decoder: JsonDecoder[Setup] = DeriveJsonDecoder.gen[Setup]
  given encoder: JsonEncoder[Setup] = DeriveJsonEncoder.gen[Setup]

  def acquireRelease(path: String = "staging.json", updateStateFile: Boolean = true) =
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
  ZIO.serviceWithZIO[Ref[Setup]](_.get.map(_.staging.toOption.flatMap(f(_))))

def programTest(cmd: Subcommand.Test) = cmd match
  case Subcommand.Test(setup, data) =>
    (for {
      _ <- ZIO.log("programTest")
      ref <- ZIO.service[Ref[Setup]]
      _ <- ZIO.log(setup.toJsonPretty)
      _ <- updateState(s => s.copy(test = data))
      state <- ref.get.map(_.staging)
      _ <- Console.printLine(state.toJsonPretty)
    } yield ()).provideLayer(setup.layer)

def testCommand = Command("test", Staging.options)
  .subcommands(
    Command("staging", Options.text("data")),
  )
  .map((config, data) => Subcommand.Test(config, data))
