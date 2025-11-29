package fmgp.did.method.prism

import zio.*
import zio.json.*
import fmgp.did.DIDDocument

case class EventRef(b: Int, o: Int, eventHash: EventHash)

object EventRef {
  given JsonDecoder[EventRef] = DeriveJsonDecoder.gen[EventRef]
  given JsonEncoder[EventRef] = DeriveJsonEncoder.gen[EventRef]
}

object EventRefOrdering extends Ordering[EventRef] {
  def compare(e1: EventRef, e2: EventRef) = e1.b.compare(e2.b) match
    case 0 =>
      e1.o.compare(e2.o) match
        case 0 => if (e1.eventHash == e2.eventHash) 0 else ??? // TODO make Exception type (it should never happen)
    case x => x
}
