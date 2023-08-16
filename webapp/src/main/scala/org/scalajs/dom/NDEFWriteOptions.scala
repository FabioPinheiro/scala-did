package org.scalajs.dom

import scala.scalajs.js

@js.native
trait NDEFWriteOptions extends js.Object {

  /** A boolean value specifying whether or not existing records should be overwritten, if such exists. */
  def `overwrite`: Boolean = js.native

  /** An AbortSignal that allows the current write operation to be canceled. */
  def `signal`: js.UndefOr[AbortSignal] = js.native
}
