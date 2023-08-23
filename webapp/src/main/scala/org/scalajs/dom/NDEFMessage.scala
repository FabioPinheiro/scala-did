package org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/** The [[NDEFMessage]] interface of the Web NFC API represents the content of an NDEF message that has been read from
  * or could be written to an NFC tag. An instance is acquired by calling the NDEFMessage() constructor or from the
  * NDEFReadingEvent.message property, which is passed to the reading event.
  *
  * @param records
  *   The records property of NDEFMessage interface represents a list of NDEFRecords present in the NDEF message.
  */
@js.native
@JSGlobal
class NDEFMessage extends js.Object {

  /** Returns the list of NDEF records contained in the message. */
  var records: FrozenArray[NDEFRecord] = js.native
}
