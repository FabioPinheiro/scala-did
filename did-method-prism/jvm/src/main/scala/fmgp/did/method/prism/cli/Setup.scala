package fmgp.did.method.prism.cli

import zio.*
import zio.cli.*
import zio.json.*
import java.nio.file.Path
import fmgp.did.method.prism.cardano.CardanoWalletConfig
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey
import fmgp.util.hex2bytes
import fmgp.util.bytes2Hex

case class StagingState(
    loadStateFileByDefault: Boolean = true,
    updateStateFileByDefault: Boolean = false,
    wallet: Option[CardanoWalletConfig] = None,
    seed: Option[Array[Byte]] = None,
    // secp256k1Keys: Map[String, Array[Byte]]
    secp256k1PrivateKey: Map[String, KMMECSecp256k1PrivateKey] = Map(),
    // key.getEncoded()).getEncoded()
    test: String = "",
)
object StagingState {

  given decoder: JsonDecoder[StagingState] = {
    given JsonDecoder[Array[Byte]] = JsonDecoder.string.map(hex => hex2bytes(hex))
    given JsonDecoder[KMMECSecp256k1PrivateKey] =
      JsonDecoder.string.map(hex => KMMECSecp256k1PrivateKey.Companion.secp256k1FromByteArray(hex2bytes(hex)))
    DeriveJsonDecoder.gen[StagingState]
  }
  given encoder: JsonEncoder[StagingState] = {
    given JsonEncoder[Array[Byte]] = JsonEncoder.string.contramap(bytes => bytes2Hex(bytes))
    given JsonEncoder[KMMECSecp256k1PrivateKey] = JsonEncoder.string.contramap(key => bytes2Hex(key.getEncoded()))
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
            (updateStateFile || newState.updateStateFileByDefault) match
              case false => ZIO.unit // *> ZIO.log("updateStateFile = false")
              case true  => ZIO.writeFile(path, newState.toJsonPretty).orDie // *> ZIO.log("StagingState close")
        }
      }
    }
}

def updateState(f: (StagingState) => StagingState): ZIO[Ref[Setup], Nothing, Unit] =
  ZIO.serviceWithZIO[Ref[Setup]](ref =>
    ref.updateSome {
      // case s @ Setup(stagingPath, updateStateFile, Left(stagingError)) => None
      case s @ Setup(stagingPath, updateStateFile, Right(state)) =>
        Setup(stagingPath, updateStateFile, Right(f(state)))
    }
  )

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
