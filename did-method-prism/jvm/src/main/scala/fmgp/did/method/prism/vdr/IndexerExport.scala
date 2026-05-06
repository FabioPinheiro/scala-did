package fmgp.did.method.prism.vdr

import zio.*
import zio.json.*
import zio.stream.*

import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption.*

import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.EventCursor
import fmgp.did.method.prism.proto.*

/** IndexerExport
  *
  * Dumps every event from a [[PrismStateRead]] into one file per `ref`. The on-disk format matches what
  * `Indexer.indexerJobFS` produces in `<workdir>/events/<ref>`: one JSON-encoded
  * [[fmgp.did.method.prism.proto.MySignedPrismEvent]] per line, sorted by `(b, o)`.
  *
  * Supports incremental export via a `.cursor` file in the export folder.
  */
object IndexerExport {

  /** Export events into per-ref files in `exportDir`.
    *
    * Behavior:
    *   - If `fromScratch=true` OR `<exportDir>/.cursor` is missing: rebuild every file with `TRUNCATE_EXISTING`.
    *   - Otherwise: read the cursor and only fetch events with `(b, o) > cursor` (via [[PrismStateRead.getEventsAfter]]),
    *     appending each new event to its `<rootRef-hex>` file (sorted by `(b, o)`).
    *
    * The latest `(b, o)` seen is written back to `<exportDir>/.cursor` after a successful run.
    *
    * @return
    *   the number of events written (full rebuild = total event count; incremental = newly appended event count).
    */
  def exportEventsToFiles(
      exportDir: Path,
      fromScratch: Boolean,
  ): ZIO[PrismStateRead, Throwable, Long] = for {
    _ <- ZIO.attemptBlockingIO(Files.createDirectories(exportDir))
    existingCursor <- CursorFile.read(exportDir)
    written <-
      if (fromScratch || existingCursor.isEmpty) fullRebuild(exportDir)
      else incrementalAppend(exportDir, existingCursor.get)
    state <- ZIO.service[PrismStateRead]
    finalCursor <- state.cursor
    _ <- CursorFile.write(exportDir, finalCursor)
    _ <- ZIO.log(s"Cursor written: $finalCursor")
  } yield written

  private def fullRebuild(exportDir: Path): ZIO[PrismStateRead, Throwable, Long] = for {
    _ <- ZIO.log(s"Full rebuild into '$exportDir'")
    ssiCount <- exportSSIEventsFull(exportDir)
    vdrCount <- exportVDREventsFull(exportDir)
    total = ssiCount + vdrCount
    _ <- ZIO.log(s"Full rebuild complete: $total events ($ssiCount SSI + $vdrCount VDR)")
  } yield total

  private def incrementalAppend(
      exportDir: Path,
      from: EventCursor,
  ): ZIO[PrismStateRead, Throwable, Long] = for {
    state <- ZIO.service[PrismStateRead]
    _ <- ZIO.log(s"Incremental export from cursor=$from")
    newEvents <- state.getEventsAfter(from)
    _ <- ZIO.log(s"Found ${newEvents.size} new events since $from")
    grouped = newEvents
      .groupBy(_.rootRef)
      .view
      .mapValues(_.map(_.event).sortBy(e => (e.b, e.o)))
    _ <- ZIO.foreachDiscard(grouped) { case (rootRef, eventsForRef) =>
      val target = exportDir.resolve(rootRef.hex)
      appendEventsFile(target, eventsForRef)
    }
  } yield newEvents.size.toLong

  // ---- Full-rebuild helpers (mirror indexerJobFS structure) ----

  private def exportSSIEventsFull(exportDir: Path): ZIO[PrismStateRead, Throwable, Long] = for {
    state <- ZIO.service[PrismStateRead]
    ssiRefs <- state.ssi2eventsRef
    written <- ZStream
      .fromIterable(ssiRefs.keys)
      .mapZIO { did =>
        for {
          events <- state.getEventsForSSI(did)
          target = exportDir.resolve(did.specificId)
          _ <- writeEventsFile(target, events, options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
        } yield events.size.toLong
      }
      .runFold(0L)(_ + _)
  } yield written

  private def exportVDREventsFull(exportDir: Path): ZIO[PrismStateRead, Throwable, Long] = for {
    state <- ZIO.service[PrismStateRead]
    vdrRefs <- state.vdr2eventsRef
    written <- ZStream
      .fromIterable(vdrRefs.keys)
      .mapZIO { ref =>
        for {
          events <- state.getEventsForVDR(ref)
          target = exportDir.resolve(ref.hex)
          _ <- writeEventsFile(target, events, options = Set(WRITE, TRUNCATE_EXISTING, CREATE))
        } yield events.size.toLong
      }
      .runFold(0L)(_ + _)
  } yield written

  // ---- Shared writers ----

  private def appendEventsFile(
      target: Path,
      events: Seq[MySignedPrismEvent[OP]],
  ): ZIO[Any, Throwable, Unit] =
    writeEventsFile(target, events, options = Set(WRITE, APPEND, CREATE))

  private def writeEventsFile[E <: OP](
      target: Path,
      events: Seq[MySignedPrismEvent[E]],
      options: Set[java.nio.file.OpenOption],
  ): ZIO[Any, Throwable, Unit] =
    ZStream
      .fromIterable(events)
      .run {
        ZSink
          .fromFileName(name = target.toString, options = options)
          .contramapChunks[MySignedPrismEvent[E]](_.flatMap { spo =>
            s"${(spo: MySignedPrismEvent[OP]).toJson}\n".getBytes
          })
      }
      .unit
}
