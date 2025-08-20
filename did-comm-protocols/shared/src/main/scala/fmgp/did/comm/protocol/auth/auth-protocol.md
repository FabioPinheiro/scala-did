# Auth Protocol

This specification defines a DIDComm v2 protocol that allows a user to authenticate (prove control of a DID) and optionally authorize access to resources in an application (e.g., website, mobile app, or WSS backend). The protocol is transport-agnostic and supports both out-of-band bootstrapping and targeted, per-session requests.

The protocol also aims to bridge the gap between Web2 and Web3. It enables authentication through self-sovereign identities (SSIs) using DIDs, while requiring only minimal changes to existing Web2 applications that already rely on OAuth-style flows.

## PIURI
  
- `https://lace.io/auth/1.0/request`
  - **Invitation to authenticate and authorize** 
    - If this is an out-of-band message it MUST be `signed(plaintext)`
    - If the message is encrypted it MUST be `authcrypt(plaintext)`
  - Ex: can be used to authenticate a WebSocket Secure (WSS), or session cookies in a website.

- `https://lace.io/auth/1.0/msg`
  - **Reply to request**
  - for register opetations we can use `signed(plaintext)`; `anoncrypt(sign(plaintext))`; `authcrypt(sign(plaintext))`
    -  this small search for accountability. A way for the server to prove that login was requested. This may enable specific use cases. E.g. the service provider may be required to keep records in specific use cases.
  - for unregister operation we can use `authcrypt(plaintext)`, by default this encrypted mode will not be accepted.

### Roles

- Auth Requester
  - **Agent server side** - The DIDComm agent operated by the relying party (RP) that generates an authentication request for any or a specified DID to create a client session..=
    Usually a well-known agent that provides a service.
  - **Application server side** - The RP’s web/app backend that manages session state and consumes the authentication result to correlates session with DIDs.
- Client
  - **Application client side** - The user agent (typically a browser or native app) initiating the sign‑in flow with the RP.
  - **Agent client side(e.g. Identity Wallet)** - The user’s DID wallet or agent. It receives authentication requests, displays them to the user, produces signed responses (proofs), and communicates them back to the agent in the server.

## Terminology

- **Authentication** — The process of verifying identity (in this protocol, proving control of a DID).
- **Authorization** — The process of determining what resources a user is allowed to access.

### Messagem Flow Diagram (Example)

```mermaid
flowchart TD
  subgraph Auth Requester
    ServerAgent(Agent service provider)
    Server
  end
  subgraph Auth Replier
    Client
    DIDWalletAgent(DIDWallet - User Agent)
  end


  ServerAgent -->|1 - Create a unique AuthRequest| Server
  Server -->|2 - Display OOB - request QR code / deeplink| Client
  Client -->|3 - Scan QR code| DIDWalletAgent
  DIDWalletAgent -->|4 - Send AuthResponse message| ServerAgent

  ServerAgent -.->|5.a - Login info for request id & DID if successful| Server
  ServerAgent -.->|5.b - Ack + outcome| DIDWalletAgent
  Server -.->|6 - Bind DID to session| Client
```


#### Example of Sequence Diagram

```mermaid
sequenceDiagram
  participant DIDWallet
  participant Client
  participant Server
  participant ServerAgent

  Note over Client,Server: Communication via HTTPS

  Client->>+Server: GET / (Ask for a WebApp)
  Server-->>-Client: Deliver WebApp

   Note over Client,Server: Create a session
  alt WebSocket Secure (WSS)
    Client->>+Server: upgrade to WWS
    Server-->>-Client: WSS stablish
  else HTTPS with cookies
    Client->>+Server: request session cookie
    Server-->>-Client: set unique session cookie
  end

rect rgb(0, 100, 0)
  opt
    Client->>+Server: request Out-of-Band Msg or a deep link
  end
  
    Server->>+ServerAgent: Generate request for this session
    ServerAgent-->>-Server: Unique request (Msg signed)
  Server->>-Client: Send Out-of-Band request (or deep link)
  end
  
  rect rgb(0, 100, 0)
    alt Open request in Wallet
      Client->> DIDWallet: Bootstrap Wallet (by open oob url)
    else
      Client->>DIDWallet: QR code Scann (of a OOB message)
    else
      Client->>DIDWallet: QR code Scann (of a deep link)
      DIDWallet->>+Server: HTTPS request the Out-of-Band Msg
      Server-->-DIDWallet: recives Out-of-Band Msg
    end
    DIDWallet->>+ServerAgent: Resolve Challenge by Reply to request
  end
  DIDWallet->>+ServerAgent: Resolve Challenge by Reply to request

  ServerAgent->>Server: Infor of login for request id awith DID
  Server->>Server: Match unique id request with the session and store DID of client

  Server->>Client: ...
```

## Example of a state machine of the participants

```mermaid
---
title: State Machine (Requester perspective)
---
stateDiagram
    idle: Waiting for new message
    note left of idle
       This agent contains a internal state to keep track of sessions, challenge and scope for each sessions, and DID associated with the session (after login).
    end note

    session: Session created
    VerifyIntegrity: Verify Message Integrity
    VerifyChallenge: Verify Session Challenge
    [*] --> idle
    idle --> NewSession: New session request
    NewSession --> idle
    state NewSession {
        [*] --> session: Generate and store OOB with seccion id
        session --> [*]: Send/display OOB invitation 
    }
    idle --> Verify: Auth DIDComm message
    Verify --> idle
    state Verify {
        state if_state <<choice>>
        state Success <<fork>>
        [*] --> VerifyIntegrity: parse message
        VerifyIntegrity --> VerifyChallenge: Verified
        VerifyIntegrity --> Error: fail to resolve DID
        VerifyIntegrity --> VerifyIntegrity: resolve DID when needed
        VerifyChallenge --> if_state
        if_state --> Success: valied challenge & updated internal state
        if_state --> Error: fail to resolve Challenge
        Success --> [*]: Login efects (inform backend server & client application)
        Success --> [*]: Send message to DID acknowledging the successful login
        Error --> [*]: Try to send an error message to sender DID
    }

```


## Notes

- The `Client` will most likely be a browser. But the application webpage running on it is developed/chosen by the one that is trying to authenticate.
- The `DIDWallet` represents the identity wallet. It can be a mobile application or a browser extension, etc. It's something that belongs to the user that will be authenticated. User is the owner and one that chooses its wallet.
  - There is a need to define a well known standard API to allow the communication between the client application (website/DApp) to the identity wallet. Something similar to [CIP-30](https://cips.cardano.org/cip/CIP-30) for a QR code with a OOB message.
- The communication between the Agents (`DIDWallet` and `ServerAgent`) is designed to be Done over DIDComm V2. It can be done directly or indirectly or a mix.
  - The typical scenario that mimics the OAuth flow is for the challenge/request OOB message (request to authenticate) to be sent from the `ServerAgent` to the `DIDWallet` indirectly via the website/DApp then show via a QR code or via browser extension bridge. The reply would be typical sent directly from the `DIDWallet` to the `ServerAgent` using the for DIDCommService of the requester.