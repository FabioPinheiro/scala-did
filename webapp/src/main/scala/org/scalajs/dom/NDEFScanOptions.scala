package org.scalajs.dom

import scala.scalajs.js

@js.native
trait NDEFScanOptions extends js.Object {

  /** An AbortSignal that allows the current write operation to be canceled. */
  def `signal`: js.UndefOr[AbortSignal] = js.native
}
