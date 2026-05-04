package fmgp.did.method.prism.cli

import zio.*

/** Best-effort cross-platform browser launcher. Failures are logged and swallowed — the URL is
  * always also printed to the console so the user can fall back to a manual click.
  */
object BrowserOpener:

  def openUrl(url: String): UIO[Unit] =
    val osName = scala.util.Properties.osName.toLowerCase
    val cmd =
      if osName.contains("mac") then Seq("open", url)
      else if osName.contains("win") then Seq("cmd", "/c", "start", "", url)
      else Seq("xdg-open", url)
    ZIO
      .attempt(scala.sys.process.Process(cmd).run())
      .unit
      .catchAll(t => ZIO.logWarning(s"Could not open browser ($cmd): ${t.getMessage}"))
