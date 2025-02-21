# DID Comm Protocols


## Official Protocols

[didcomm.org](https://didcomm.org/search/)


- DONE - `Action Menu 2.0` - https://didcomm.org/action-menu/2.0
- DONE - `BasicMessage 2.0` - https://didcomm.org/basicmessage/2.0
- DONE - `DiscoverFeatures 2.0` - https://didcomm.org/discover-features/2.0
- TODO - `MediaSharing 1.0` - https://didcomm.org/media-sharing/1.0
- DONE - `MediatorCoordination 2.0` - https://didcomm.org/mediator-coordination/2.0
- DONE - `MediatorCoordination 3.0` - https://didcomm.org/mediator-coordination/3.0
  - Compare [specs](https://github.com/decentralized-identity/didcomm.org/tree/main) with previous version
    - `diff site/content/protocols/mediator-coordination/3.0 site/content/protocols/mediator-coordination/2.0/`
  - Compare code with previous version
    - `diff did/shared/src/main/scala/fmgp/did/comm/protocol/mediatorcoordination3/MediatorCoordination.scala did/shared/src/main/scala/fmgp/did/comm/protocol/mediatorcoordination2/MediatorCoordination.scala`
    - `diff did/shared/src/main/scala/fmgp/did/comm/protocol/mediatorcoordination3/Recipient.scala did/shared/src/main/scala/fmgp/did/comm/protocol/mediatorcoordination2/Keylist.scala`
- DONE - `OutOfBand 2.0` - https://didcomm.org/out-of-band/2.0
- DONE - `Pickup 3.0` - https://didcomm.org/pickup/3.0
- TODO - `QuestionAnswer 1.0` - https://didcomm.org/question-answer/1.0
- TODO - `Receipts 1.0` - https://didcomm.org/receipts/1.0
- DONE - `ReportProblem 2.0` - https://didcomm.org/report-problem/2.0 - See [specs](https://identity.foundation/didcomm-messaging/spec/#problem-reports)
- NOT_ATM - `Routing 2.0`- https://didcomm.org/routing/2.0
  - Also see https://didcomm.org/book/v2/routing
- DONE - `TrustPing 2.0` - https://didcomm.org/trust-ping/2.0/
- TODO - `UserProfile 1.0` - https://didcomm.org/user-profile/1.0
- NOT_ATM - `Data-agreement 1.0`:
  - NOT_TODO_(DIDCommV1?) - `data-agreement-context-decorator` - https://didcomm.org/data-agreement-context-decorator/1.0/
  - NOT_ATM - `data-agreement-negotiation` - https://didcomm.org/data-agreement-negotiation/1.0/
  - NOT_TODO_(+-DIDCommV1?) - `data-agreement-proofs` - https://didcomm.org/data-agreement-proofs/1.0/
  - NOT_ATM - `data-agreement-termination` - https://didcomm.org/data-agreement-termination/1.0/
  - NOT_ATM - `data-disclosure-agreement` - https://didcomm.org/data-disclosure-agreement/1.0/

## Ideas for new Protocols
- Create new protocol `Authenticate / Authorization`
- Create new protocol `DidResolver`
- Create new protocol `PreSetValue`
- Create new protocol `PseudoRandom`
  - https://www.stat.berkeley.edu/~stark/Java/Html/sha256Rand.htm
  - https://www.rfc-editor.org/rfc/rfc3797
