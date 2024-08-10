# Discovery Protocol


- **Registry** - A DID that provide other service to users (DIDs)
- **User** - The user (DID) that is trying to use the register service

## PIURI

- `https://decentriqube.com/discovery/1/ask_introduction`
- `https://decentriqube.com/discovery/1/introduction_status`
- `https://decentriqube.com/discovery/1/forward_request`
- `https://decentriqube.com/discovery/1/request`
- `https://decentriqube.com/discovery/1/answer`
- `https://decentriqube.com/discovery/1/handshake`


### Flow:

```mermaid
flowchart TD
  A[User A & A']
  B[User B & B']
  R[Registry]
  A -->|ask_introduction| R
  R -->|forward_request| B
  R -->|introduction_status| A
  A -->|request| B
  B -->|answer| A
  A -->|handshake| B
```

#### Example of Sequence Diagram

```mermaid
sequenceDiagram
  participant UserA'
  participant UserA
  participant Registry
  participant UserB
  participant UserB'

  Note over UserB: Assume UserB is enrolled

  rect rgb(0, 100, 0)
    UserA ->>+ Registry: ask_introduction (with 'request' from UserA)
    Registry -->> UserB: (*) forward_request (with 'request')
    Registry -->>- UserA: introduction_status
    UserB' -->>+ UserA': (**) answer
    UserA' -->>- UserB': handshake
  end
```
- `*` - If the conditions verify
- `**` - If the user decides to reply

## message structure

```mermaid
flowchart TD
  A[User A]
  B[User B]
  R[Registry]
  A -->|ask_introduction| R
  R -->|introduction_status| A
  R -->|forward_request| B
```

- `ask_introduction`
  - from (enroll DID)
  - to (registry)
  - attachment:
    - (signed `https://decentriqube.com/discovery/1/request`)

- `introduction_status`
  - from (registry)
  - to (to the one ask_introduction)
  - thid
  - body:
    - forward_request_sent: Boolean

- `forward_request`
  - from
  - to
  - thid
  - attachment:
    - (signed `https://decentriqube.com/discovery/1/request`)


```mermaid
flowchart TD
  A[User A']
  B[User B']
  A -->|request| B
  B -->|answer| A
  A -->|handshake| B
```

- `request` (singed message)
  - from (new DID)
  - body:
    - `subject_type` - (Email | Domain | Discord | Tel)
    - `subject`

- `answer` (introduction request)
  - from
  - to
  - thid (id of the `request` its also the challenge)
  - pthid (id of the `forward_request`)


- `handshake`
  - from
  - to