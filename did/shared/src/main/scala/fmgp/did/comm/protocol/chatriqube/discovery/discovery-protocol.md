# Discovery Protocol


- **Registry** - A DID that provide other service to users (DIDs)
- **User** - The user (DID) that is trying to use the register service

## PIURI

- `https://decentriqube.com/discovery/1/ask_introduction`
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
  A -->|request| B
  B -->|answer| A
  A -->|handshake| B
```

#### Example of Sequence Diagram

```mermaid
sequenceDiagram
  participant UserA
  participant Registry
  
  rect rgb(0, 100, 0)
    UserA ->>+ Registry: enroll
    Registry-->>-UserA: account | ProblemReport
  end

  rect rgb(0, 100, 0)
    Note over UserA,Registry: Assume now the UserA is enrolled
    UserA ->>+ Registry: set_id
    Registry-->>-UserA: account | ProblemReport
  end

```

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
    Registry -->>- UserB: (*) forward_request (with 'request')
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
  R -->|forward_request| B
```

- `ask_introduction`
  - from (enroll DID)
  - to (registry)
  - attachment:
    - (signed `https://decentriqube.com/discovery/1/request`)

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
    - `subjectType` - (Email | Domain | Discord | Tel)
    - `subject`

- `answer` (introduction request)
  - from
  - to
  - thid (id of the `request` its also the challenge)
  - pthid (id of the `forward_request`)


- `handshake`
  - from
  - to