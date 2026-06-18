package fmgp.did.method.prism.vdr

import zio.*
import zio.json.*

import java.nio.file.{Files, Path}

import fmgp.did.method.prism.cardano.EventCursor

/** Cursor file companion: reads and writes `<dir>/.cursor`.
  *
  * The file contains a single JSON object `{"b":<int>,"o":<int>}` representing the latest `(b, o)` of an event written
  * to the folder. It allows the exporter to resume incrementally instead of rebuilding from scratch.
  */
object CursorFile {

  /** Filename — leading `.` makes it sort first in lexicographic listings (and hides it from default `ls`). */
  val Name: String = ".cursor"

  def path(dir: Path): Path = dir.resolve(Name)

  /** Reads the cursor file. Returns `None` if it does not exist. Fails if it exists but is unreadable or malformed. */
  def read(dir: Path): ZIO[Any, Throwable, Option[EventCursor]] =
    ZIO
      .attemptBlockingIO {
        val p = path(dir)
        if (Files.exists(p)) Some(Files.readString(p)) else None
      }
      .flatMap {
        case None          => ZIO.none
        case Some(content) =>
          content.trim.fromJson[EventCursor] match
            case Right(c)    => ZIO.some(c)
            case Left(error) =>
              ZIO.fail(new RuntimeException(s"Failed to parse '${path(dir)}': $error (content='$content')"))
      }

  /** Writes the cursor file (full overwrite). Creates the parent directory if missing. */
  def write(dir: Path, cursor: EventCursor): ZIO[Any, Throwable, Unit] =
    ZIO.attemptBlockingIO {
      Files.createDirectories(dir)
      Files.writeString(path(dir), cursor.toJson)
      ()
    }
}
