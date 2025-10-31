package fmgp.did.method.prism.mongo

import fmgp.did.method.prism.EventHash
import fmgp.did.method.prism.proto.*

case class EventWithRootRef(rootRef: EventHash, event: MySignedPrismEvent[OP])
