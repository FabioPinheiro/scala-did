package fmgp.did.method.prism

import fmgp.did.method.prism.proto.*

case class EventWithRootRef(rootRef: EventHash, event: MySignedPrismEvent[OP])
