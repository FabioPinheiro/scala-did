package fmgp.did.method.prism.cardano

import zio.json._
import scala.deriving.*, scala.compiletime.*

trait PrismBlockIndex {
  def tx: String

  /** Index relative to the Cardano Trasation with PRISM_LABEL */
  def b: Int

  /** Index relative to the Cardano Trasation with PRISM_LABEL */
  def prismBlockIndex: Int = b
}

trait PrismEventIndex extends PrismBlockIndex {

  /** Index relative to the PrismBlock */
  def o: Int

  /** Index relative to the PrismBlock */
  def prismEventIndex: Int = o
}

/** Like EventRef */
case class EventCursor(b: Int, o: Int) extends PrismEventIndex {
  def tx = s"EventCursor:$b;$o" // FIXME
}
object EventCursor {
  def init: EventCursor = EventCursor(-1, -1)
  given Ordering[EventCursor] = Ordering.by(e => (e.b, e.o))
  given decoder: JsonDecoder[EventCursor] = DeriveJsonDecoder.gen[EventCursor]
  given encoder: JsonEncoder[EventCursor] = DeriveJsonEncoder.gen[EventCursor]
}
