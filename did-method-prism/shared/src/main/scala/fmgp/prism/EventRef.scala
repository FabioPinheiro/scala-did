package fmgp.prism

import zio._
import zio.json._
import fmgp.did.DIDDocument

case class EventRef(b: Int, o: Int, opHash: String)

object EventRef {
  given JsonDecoder[EventRef] = DeriveJsonDecoder.gen[EventRef]
  given JsonEncoder[EventRef] = DeriveJsonEncoder.gen[EventRef]
}

object EventRefOrdering extends Ordering[EventRef] {
  def compare(e1: EventRef, e2: EventRef) = e1.b.compare(e2.b) match
    case 0 =>
      e1.o.compare(e2.o) match
        case 0 => if (e1.opHash == e2.opHash) 0 else ??? // TODO make Exception type (it should never happen)
    case x => x
}
