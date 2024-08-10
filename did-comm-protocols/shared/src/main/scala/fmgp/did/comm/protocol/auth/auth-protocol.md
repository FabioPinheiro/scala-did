# Auth Protocol

This can be an out-of-band style protocol but can also be dedicated to a user.

This protocol is used a authenticate and or authorization an user (DID) or operations.

- **Authentication** - verifies DID
- **Authorization** - determines what resources a user can access

## PIURI
  
- `https://fmgp.app/auth/1.0/request`
  - **Invitation to authenticate and authorize** 
    - If this is an out-of-band message it MUST me `signed-(plaintext)`
    - If the message is encrypt it MUST be `authcrypt(plaintext)`
  - Ex: can be used to autenticate a WebSocket Secure (WSS)

- `https://fmgp.app/auth/1.0/reply`
  - **Mediate Reply**
  - for register opetations we can use `signed(plaintext)`; `anoncrypt(sign(plaintext))`; `authcrypt(sign(plaintext))`
  - for unregister operation we can use `authcrypt(plaintext)`, by default this encrypted mode will not be accepted.

### Roles

- Server
  - The DID Comm agent (server) that invite to create a session
- Client
  - The DID Comm agent that want to start the session (by login with is DID).

### Messagem Flow Diagram (Example)

```mermaid
flowchart TD
  subgraph Auth Replier
    Client
    DIDWalletAgent(DIDWallet Agent)
  end
  subgraph Auth Requester
    ServerAgent(Server Agent)
    Server
  end

  ServerAgent -->|Create a unique AuthRequest| Server
  Server -->|Show OOB - request| Client
  Client -->|Scan QRcode| DIDWalletAgent
  DIDWalletAgent -->|Send Message - AuthReply| ServerAgent
```

#### Example of Sequence Diagram

```mermaid
sequenceDiagram
  participant DIDWallet
  participant Client
  participant Server
  participant ServerAgent

  Note over Client,Server: Communication via HTTPS

  Client->>+Server: Ask for a website
  Server-->>-Client: Deliver WebApp


  Note over Client,Server: Communication via WebSocket Secure (WSS)

  Client->>+Server: Open a WWS (create a session)
  Server-->>-Client: WSS stablish

  rect rgb(0, 100, 0)
    Server->>+ServerAgent: Generate request for this session
    ServerAgent-->>-Server: Unique request (Msg signed)
  end
  Server->>Client: Send Out-of-Band request
  rect rgb(0, 100, 0)
    alt Open request in Wallet
      Client->> DIDWallet: Bootstrap Wallet (by open oob url)
    else
      Client->>DIDWallet: QR code Scann
    end
    DIDWallet->>+ServerAgent: Resolve Challenge by Reply to request
  end

  ServerAgent->>Server: Infor of login for request id awith DID
  Server->>Server: Match unique id request with the session and store DID of client

  Server->>Client: ...
```
