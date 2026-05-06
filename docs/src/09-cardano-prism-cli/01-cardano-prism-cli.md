# Cardano PRISM CLI

The `cardano-prism` CLI is a tool for managing [PRISM DIDs](https://github.com/input-output-hk/prism-did-method-spec) on the Cardano blockchain. It supports key management, DID creation/resolution, VDR (Verifiable Data Registry) operations, and two ways of submitting to the chain — either directly via the Blockfrost API, or by handing the transaction off to a [CIP-30](https://cips.cardano.org/cip/CIP-30) browser wallet (Lace, Eternl, Nami, Yoroi, …). It also ships with a local browser playground for inspecting / simulating PRISM events without writing to the chain.
The CLI is structured around modules: `mnemonic`, `key`, `did`, `cardano`, `website`, `indexer`, and `vdr`.

## Install

### Install with Coursier

Install [Coursier](https://get-coursier.io/) (a pure Scala artifact fetching tool), then set up the alias:

```bash
alias cardano-prism='cs launch app.fmgp::cardano-prism-cli:0.1.0-M46 -M fmgp.did.method.prism.cli.PrismCli --'

# Or with fewer JVM warnings
alias cardano-prism='cs launch --java-opt --sun-misc-unsafe-memory-access=allow app.fmgp::cardano-prism-cli:0.1.0-M46 -M fmgp.did.method.prism.cli.PrismCli --'

cardano-prism --help
```

### Install with Homebrew (deprecated)

A brew tap (`homebrew-fmgp`) is available for installing `cardano-prism`:

```bash
brew install FabioPinheiro/fmgp/cardano-prism
cardano-prism --help
```

## Configuration File

The CLI uses a JSON configuration file. **Default location:** `~/.cardano-prism-config.json`

```bash
# Create the config file
cardano-prism config-file --create

# View the current config
cardano-prism config-file
```

The config file stores:
- SSI and Cardano wallet mnemonics
- Blockfrost API tokens
- Derived private keys (Secp256k1, Ed25519, X25519) and randomly generated keys
- Blockfrost API tokens per network

Most commands automatically load the config file. Use the `-s` flag to enable automatic saving of state changes at the end of a command.

---

## Mnemonic Management (`mnemonic`)

Mnemonics are BIP-39 seed phrases used to deterministically derive keys for both the SSI (DID) wallet and the Cardano (ADA) payment wallet.

### Generate a new mnemonic

```bash
# Generate and save a new SSI wallet mnemonic (saves with -s flag)
cardano-prism mnemonic new -s

# Generate a new ADA wallet mnemonic
cardano-prism mnemonic new -s --wallet-type ada

# Import an existing mnemonic
cardano-prism mnemonic new -s --mnemonic "word1 word2 ... word24"
```

### Display the seed (hex)

```bash
# Show seed from stored SSI wallet
cardano-prism mnemonic seed

# Show seed from a specific mnemonic
cardano-prism mnemonic seed --mnemonic "word1 word2 ... word24"
```

### Derive a Cardano address

```bash
# Show address for the stored Cardano wallet (mainnet)
cardano-prism mnemonic address

# Show address for a specific network
cardano-prism mnemonic address --network preprod
```

---

## Key Management (`key`)

Keys are derived from the SSI wallet mnemonic using the [Identus HD key derivation](https://hyperledger-identus.github.io/docs/adrs/decisions/2023-05-16-hierarchical-deterministic-key-generation-algorithm) (based on CIP-1852).

The derivation path follows the pattern: `m/29'/29'/<DID-index>'/<key-usage>'/<key-index>'`

Key usages: `Master`, `Issuing`, `Keyagreement`, `Authentication`, `Revocation`, `Capabilityinvocation`, `Capabilitydelegation`, Master

Ex:
- Secp256k1 - for `Master` and `Vdr`
- Ed25519 - `Issuing` (for issuing CVs) and `Authentication`  (for login)
- X25519 - `Keyagreement` (for communication)

### Derive a Secp256k1 key

The Master and VDR keys **MUST** be of type `secp256k1`.

```bash
# cardano-prism key secp256k1 [--label <name>] <DID-index> <keyUsage> <key-index>
cardano-prism key sepc256k1 0 Master 0 # Saves key as "key-0-Master-0"

cardano-prism key sepc256k1 --label my-master-key 0 Master 0
```

### Derive an Ed25519 key

```bash
# cardano-prism key Ed25519 [--label <name>] <DID-index> <keyUsage> <key-index>
cardano-prism key Ed25519 0 Issuing 0 # Saves key as "key-0-Issuing-0"

cardano-prism key Ed25519 0 Authentication 0
```

### Derive an X25519 key

```bash
# cardano-prism key X25519 [--label <name>] <DID-index> <keyUsage> <key-index>
cardano-prism key X25519 0 Keyagreement 0 # Saves key as "key-0-KeyAgreement-0"
```

### Generate a random Ed25519 key

Generates a random Ed25519 private key (not derived from a mnemonic) and saves it to the config.

```bash
# cardano-prism key random-Ed25519 --label <name>
cardano-prism key random-Ed25519 --label my-ed25519-key
```

### Generate a random X25519 key

Generates a random X25519 private key (not derived from a mnemonic) and saves it to the config.

```bash
# cardano-prism key random-X25519 --label <name>
cardano-prism key random-X25519 --label my-x25519-key
```

### Derive a key from a custom derivation path

```bash
# cardano-prism key derivation-path [--mnemonic <phrase>] [--derivation-path <path>] [--label <name>]
cardano-prism key derivation-path --derivation-path "m/29'/29'/0'/1'/0'" --label my-key
```

---

## DID Management (`did`)

### Create a deterministic DID (recommended)

Creates a PRISM DID following the [deterministic PRISM DID generation proposal](https://github.com/input-output-hk/prism-did-method-spec/blob/main/extensions/deterministic-prism-did-generation-proposal.md). Requires at least the Master key. Outputs the DID and the encoded PRISM events to submit to the blockchain.

Each key argument is `label:Usage`. Valid usage tokens: `Master`, `Issuing`, `Keyagreement`, `Authentication`, `Revocation`, `Capabilityinvocation`, `Capabilitydelegation`, `Vdr`. Exactly one key must declare `:Master`.

```bash
# cardano-prism did create-deterministic [-S <id>=<endpoint>] <keyID:Usage>...
cardano-prism did create-deterministic key-0-Master-0:Master

# With a DIDComm service endpoint
cardano-prism did create-deterministic -S e1=https://did.example.com key-0-Master-0:Master

# With multiple keys and a service
cardano-prism did create-deterministic -S e1=https://did.example.com key-0-Master-0:Master key-0-Issuing-0:Issuing
```

**Example output:**
```
SSI: did:prism:d2ffaf9f8d7fa754d0e008649d29f3de8ac63699b263d4080b3450ac3c0ebdec
create SignedPrismEvent:
0a0e6b65792d302d4d61737465722d30...
update SignedPrismEvent:
0a0e6b65792d302d4d61737465722d30...
```

The output events are hex-encoded protobuf payloads that must be submitted to the Cardano blockchain (see [Submit Events](#submit-events)).

### Create a DID for VDR

Creates a DID with a master key and optional VDR key.

```bash
cardano-prism did create-for-vdr --master key-0-Master-0 --vdr key-0-Vdr-0
```

### Resolve a DID

```bash
# Resolve from the PRISM VDR (GitHub-hosted)
cardano-prism did resolve-prism did:prism:<hash>
cardano-prism did resolve-prism --network preprod did:prism:<hash>

# Resolve from a custom endpoint
cardano-prism did resolve did:prism:<hash> https://my-resolver.example.com/

# Resolve using the Universal Resolver (https://dev.uniresolver.io/)
cardano-prism did resolve-universal did:prism:<hash>
cardano-prism did resolve-universal --endpoint https://my-uniresolver.example.com/ did:prism:<hash>

# Resolve using NeoPrism
cardano-prism did resolve-neoprism did:prism:<hash>
cardano-prism did resolve-neoprism --network preprod did:prism:<hash>
```

---

## Cardano / Blockfrost (`cardano`)

Commands for interacting with the Cardano blockchain via the [Blockfrost API](https://blockfrost.io/).

### Save a Blockfrost API token

```bash
# cardano-prism cardano blockfrost-token [-s] <network> <token>
cardano-prism cardano blockfrost-token -s preprod preprod<YOUR_API_TOKEN>
cardano-prism cardano blockfrost-token -s mainnet mainnet<YOUR_API_TOKEN>
```

Networks: `mainnet`, `preprod`, `preview`

### Check wallet balance

```bash
# Check ADA balance of the stored Cardano wallet
cardano-prism cardano address --network preprod
```

### Submit Events

Submit hex-encoded PRISM signed events to the Cardano blockchain. This path requires:
- a Cardano payment wallet stored in the config (covers transaction fees), and
- a Blockfrost API token saved for the target network.

```bash
# cardano-prism cardano submit --network <network> <event-hex>...
cardano-prism cardano submit --network preprod <create-event-hex> <update-event-hex>
```

On success, prints the transaction hash and links to Cardanoscan and NeoPrism.

If you'd rather not store a wallet mnemonic or Blockfrost token, see [Submit Events via a browser wallet (CIP-30)](#submit-events-via-a-browser-wallet-cip-30) below.

---

## Website (`website`)

Local-website driven flows. The CLI starts an HTTP server on `localhost`, auto-opens your default browser, and serves a small Laminar SPA bundled into the CLI. Decoupled from Blockfrost — useful when you'd rather sign and submit through your existing browser wallet than store a mnemonic and a Blockfrost token in the config, or when you just want to inspect / simulate PRISM events visually.

The SPA has three pages, reachable directly via URL fragment:

- **Home** (`#/`) — overview with quick links to Submit and Simulate.
- **Submit** (`#/submit`) — the CIP-30 wallet submission flow (only meaningful when the CLI has injected events).
- **Simulate** (`#/simulate`) — paste raw events or a Cardano metadata-map and see the resulting `SSI` and DID Document, all client-side.

Three subcommands pick the landing page:

| Command | Lands on | Lifecycle |
| --- | --- | --- |
| `cardano-prism website open` | Home | runs until Ctrl-C |
| `cardano-prism website simulate` | Simulate | runs until Ctrl-C |
| `cardano-prism website submit-cip30 <events>` | Submit | exits after the wallet posts the tx hash |

All three accept `--port <integer>` (default `8088`).

### Open the playground

```bash
# cardano-prism website open [--port <integer>]
cardano-prism website open
cardano-prism website open --port 9090
```

The browser opens at `http://localhost:8088/#/`. From the Home page you can navigate to Simulate or Submit. The server stays up until you hit Ctrl-C.

### Simulate events from text

Opens directly on the Simulate page. Paste hex into the textarea, press **Simulate**, and the page runs the events through `PrismStateInMemory` in the browser and renders the resulting SSI plus DID Document JSON.

```bash
# cardano-prism website simulate [--port <integer>]
cardano-prism website simulate
```

The textarea accepts (one item per line, mixing allowed):

- `SignedPrismEvent` protobuf hex — what `did create-deterministic` prints to stdout.
- `PrismObject` protobuf hex — its inner block events are extracted.
- Cardano metadata-map CBOR hex — the `{ 21325: { v, c } }` blob CardanoScan shows on a PRISM transaction's metadata tab. Useful for inspecting an existing on-chain DID.

The server stays up until Ctrl-C.

### Submit Events via a browser wallet (CIP-30)

Submit PRISM events through a CIP-30 browser wallet (Lace, Eternl, Nami, Yoroi, …). The browser builds the Cardano transaction, the wallet signs it, and the wallet's own backend submits it. **No Blockfrost token is needed** and **no Cardano wallet mnemonic is read from the config** — the only thing the CLI knows is the events you pass in.

```bash
# cardano-prism website submit-cip30 [--port <integer>] <event-hex>...
cardano-prism website submit-cip30 <create-event-hex> <update-event-hex>

# Custom port (default is 8088)
cardano-prism website submit-cip30 --port 9090 <create-event-hex>
```

Flow:

1. The CLI starts a local HTTP server and prints `Open http://localhost:8088 in your browser to submit.`, then opens the browser automatically.
2. The page lands on Submit, lists the DID(s) that will be created, shows a "Simulated state" preview (same client-side simulation as the Simulate page), and presents a wallet picker.
3. Click **Submit**. The wallet asks you to approve the transaction.
4. As soon as the wallet returns a transaction hash, the CLI prints the hash + a CardanoScan link and exits.

The network the transaction lands on is whichever network your wallet is currently set to (mainnet / preprod / preview).

---

## Indexer (`indexer`)

The indexer continuously fetches PRISM events from the Cardano blockchain and indexes them locally, enabling offline DID resolution.

### File system mode (in-memory indexer)

Reads and writes state to the local file system. Suitable for lightweight deployments and CI/CD workflows.

```bash
# cardano-prism indexer in-memory [--token <BLOCKFROST_APIKEY>] <work-directory>
cardano-prism indexer in-memory --token preprod<YOUR_API_TOKEN> ./prism-vdr/preprod
```

See a real-world example: [indexer.yml](https://github.com/FabioPinheiro/prism-vdr/blob/main/.github/workflows/indexer.yml)

### MongoDB mode (database mode)

Stores state in a MongoDB database. Suitable for production deployments.

```bash
# cardano-prism indexer mongodb --token <BLOCKFROST_APIKEY> <mongodb-connection>
cardano-prism indexer mongodb \
  --token preprod<YOUR_API_TOKEN> \
  'mongodb+srv://user:password@cluster0.example.mongodb.net/indexer'
```

### Export from MongoDB to per-`ref` files

Dumps the events stored in a MongoDB-backed indexer into one file per `ref` on disk. The on-disk layout matches what the file system indexer produces (`<output-folder>/<ref-hex>`, one JSON-encoded event per line, sorted by `(b, o)`), so any tool already consuming `events/<ref>` files works unchanged.

A `.cursor` file (JSON `{"b":N,"o":M}`) is written into the output folder after each successful run, recording the latest `(b, o)` exported. Subsequent runs read this cursor and only fetch events with `(b, o) > cursor`, appending them to the existing files. Use `--from-scratch` to ignore the cursor and rebuild every file.

```bash
# cardano-prism indexer export [--from-scratch] <mongodb-connection> <output-folder>

# First run (or any run with no .cursor) — full rebuild
# Subsequent runs — incremental, appends only new events since the last .cursor
cardano-prism indexer export \
  'mongodb+srv://readonly:readonly@cluster0.example.mongodb.net/indexer' \
  ./prism-vdr/preprod/events

# Force a full rebuild
cardano-prism indexer export --from-scratch \
  'mongodb+srv://readonly:readonly@cluster0.example.mongodb.net/indexer' \
  ./prism-vdr/preprod/events
```

A read-only MongoDB connection string is sufficient.

---

## VDR — Verifiable Data Registry (`vdr`)

VDR commands manage anchored data entries on the Cardano blockchain, linked to a PRISM DID via a VDR key.

All VDR commands require:
- `--network` — target Cardano network (`mainnet`, `preprod`, `preview`)
- `--work-directory` — local indexer directory
- `--owner` — PRISM DID that owns the entry
- `--vdr` — label of the VDR private key (default: `vdr`)

### Create a bytes entry

```bash
cardano-prism vdr create-bytes \
  --network preprod \
  --work-directory ./prism-vdr/preprod \
  --owner did:prism:<hash> \
  --vdr key-0-Vdr-0 \
  <hex-encoded-bytes>
```

### Create an IPFS entry

```bash
cardano-prism vdr create-ipfs \
  --network preprod \
  --work-directory ./prism-vdr/preprod \
  --owner did:prism:<hash> \
  <IPFS-CID>
```

### Update a bytes entry

```bash
cardano-prism vdr update-bytes \
  --network preprod \
  --work-directory ./prism-vdr/preprod \
  <vdr-entry-ref> \
  <new-hex-encoded-bytes>
```

### Update an IPFS entry

```bash
cardano-prism vdr update-ipfs \
  --network preprod \
  --work-directory ./prism-vdr/preprod \
  <vdr-entry-ref> \
  <new-IPFS-CID>
```

### Deactivate an entry

```bash
cardano-prism vdr deactivate \
  --network preprod \
  --work-directory ./prism-vdr/preprod \
  <vdr-entry-ref>
```

### Fetch an entry

```bash
cardano-prism vdr fetch \
  --network preprod \
  --work-directory ./prism-vdr/preprod \
  <vdr-entry-ref>
```

### Generate proof for an entry

```bash
cardano-prism vdr proof \
  --network preprod \
  --work-directory ./prism-vdr/preprod \
  <vdr-entry-ref>
```

---

## End-to-End Example: Create and Publish a DID

This walkthrough creates a PRISM DID with one master key, one issuing key, and a DIDComm service endpoint, then anchors it on Cardano preprod.

### 1. Set up keys and a DID

```bash
# Create the config file (one-time)
cardano-prism config-file --create

# Generate and save an SSI wallet mnemonic
cardano-prism mnemonic new -s
cardano-prism mnemonic new -s uniform uniform index inner limb joy weapon business slim truck seed order call monster mad tattoo any hospital finger tourist jar video east earn

# Master key — must be secp256k1
cardano-prism key sepc256k1 2 Master 0 # => saves as "key-2-Master-0"
cardano-prism key random-Ed25519 --label iss
cardano-prism key random-Ed25519 --label auth
cardano-prism key random-X25519 --label comm

# the keys Ex:
# "ssiPrivateKeys" : {
#    "key-2-Master-0" : {
#      "KeySecp256k1" : {
#        "derivationPath" : "m/29'/29'/2'/1'/0'",
#        "key" : "8f60bf555957fbbf612186aa39d0d0eac6f44db51133dbfd43bb8bab9de91a55"
#      }
#    }
#   "iss" : {
#     "kty" : "OKP",
#     "crv" : "Ed25519",
#     "d" : "eEkeNXtrON4FnDjboznLl1kP4QJx9KMVxZ19mUnc3eY",
#     "x" : "lHFqJEvTZ39HA3cS1T_tKO2jQ4-bNKJlLQl5MmJf8bw"
#   },
#   "auth" : {
#     "kty" : "OKP",
#     "crv" : "Ed25519",
#     "d" : "jcRFhJLb37C7qAGEIOoVI5TJ7BmCbTjG4ss-J4YuqHk",
#     "x" : "PvkqmJr98yeivaf9nJb8sQJSao0wJNKyg1ksI0llpbg"
#   },
#   "comm" : {
#     "kty" : "OKP",
#     "crv" : "X25519",
#     "d" : "_6FVXdgJ-4LQYaQZzYITJbRIhgtOy2cMNKq3_CWWno0",
#     "x" : "RBY7U6cbUzdQmXM6VymSwFzEeGpG6ZRMhzWP115hpCo"
#   }
# }


# Build the deterministic DID (with a DIDComm service endpoint)
cardano-prism did create-deterministic -S e1=https://kyc.fabiopinheiro.com/ key-2-Master-0:Master iss:Issuing auth:Authentication comm:Keyagreement


# Output:
#SSI: did:prism:2563f86affd3dc321ad927df9bcb2bb5d01c243b62cc835f598d9f51995a16e9
#create SignedPrismEvent:
#0a066d61737465721247304502210090524b702a3a5c7e1080343bd50d1991174e68d77d664847b7332e81623acb8602206c12ab66fcd2b104293e4fcb26d56c1d183fd02bc4591494db495cbdb9afb9f11a400a3e0a3c123a0a066d617374657210014a2e0a09736563703235366b3112210298ae0c9adfabc96890a274158eae1c20ba19d24d4b0a73dac0683e5aaf225677
#update SignedPrismEvent:
#0a066d617374657212473045022100cd892633b167309956cd839967d176027c38d45a7b3bbb13a4124a88419fc03f02204be70ec7f68d6b1015d7d1cf1553ca8bc8423d39f705fe72005ca70643112fcf1adc0212d9020a202563f86affd3dc321ad927df9bcb2bb5d01c243b62cc835f598d9f51995a16e91240323536336638366166666433646333323161643932376466396263623262623564303163323433623632636338333566353938643966353139393561313665391a441a420a400a0265311210444944436f6d6d4d6573736167696e671a287b22757269223a2268747470733a2f2f6b79632e666162696f70696e686569726f2e636f6d2f227d1a390a370a350a046175746810044a2b0a074564323535313912203ef92a989afdf327a2bda7fd9c96fcb102526a8d3024d2b283592c234965a5b81a380a360a340a0369737310024a2b0a0745643235353139122094716a244bd3677f47037712d53fed28eda3438f9b34a2652d097932625ff1bc1a380a360a340a04636f6d6d10034a2a0a06583235353139122044163b53a71b53375099733a572992c05cc4786a46e9944c87358fd75e61a42a

cardano-prism website submit-cip30  --port 8888 0a066d61737465721247304502210090524b702a3a5c7e1080343bd50d1991174e68d77d664847b7332e81623acb8602206c12ab66fcd2b104293e4fcb26d56c1d183fd02bc4591494db495cbdb9afb9f11a400a3e0a3c123a0a066d617374657210014a2e0a09736563703235366b3112210298ae0c9adfabc96890a274158eae1c20ba19d24d4b0a73dac0683e5aaf225677 0a066d617374657212473045022100cd892633b167309956cd839967d176027c38d45a7b3bbb13a4124a88419fc03f02204be70ec7f68d6b1015d7d1cf1553ca8bc8423d39f705fe72005ca70643112fcf1adc0212d9020a202563f86affd3dc321ad927df9bcb2bb5d01c243b62cc835f598d9f51995a16e91240323536336638366166666433646333323161643932376466396263623262623564303163323433623632636338333566353938643966353139393561313665391a441a420a400a0265311210444944436f6d6d4d6573736167696e671a287b22757269223a2268747470733a2f2f6b79632e666162696f70696e686569726f2e636f6d2f227d1a390a370a350a046175746810044a2b0a074564323535313912203ef92a989afdf327a2bda7fd9c96fcb102526a8d3024d2b283592c234965a5b81a380a360a340a0369737310024a2b0a0745643235353139122094716a244bd3677f47037712d53fed28eda3438f9b34a2652d097932625ff1bc1a380a360a340a04636f6d6d10034a2a0a06583235353139122044163b53a71b53375099733a572992c05cc4786a46e9944c87358fd75e61a42a

# Verify 
# https://cardanoscan.io/transaction/419b1ba119beb855c8229e17940cc5c01cede9431a3db6b51691984e183293cb?tab=utxo
# https://neoprism.patlo.dev/resolver?did=did:prism:2563f86affd3dc321ad927df9bcb2bb5d01c243b62cc835f598d9f51995a16e9
cardano-prism did resolve-prism --network mainnet did:prism:2563f86affd3dc321ad927df9bcb2bb5d01c243b62cc835f598d9f51995a16e9
cardano-prism did resolve-neoprism --network mainnet did:prism:2563f86affd3dc321ad927df9bcb2bb5d01c243b62cc835f598d9f51995a16e9
```