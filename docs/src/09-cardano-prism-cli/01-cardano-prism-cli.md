# Cardano PRISM CLI

The `cardano-prism` CLI is a tool for managing [PRISM DIDs](https://github.com/input-output-hk/prism-did-method-spec) on the Cardano blockchain. It supports key management, DID creation and resolution, VDR (Verifiable Data Registry) operations, and blockchain submission via the Blockfrost API.

## Install

### Install with Coursier

Install [Coursier](https://get-coursier.io/) (a pure Scala artifact fetching tool), then set up the alias:

```bash
alias cardano-prism='cs launch app.fmgp::cardano-prism-cli:0.1.0-M42 -M fmgp.did.method.prism.cli.PrismCli --'

# Or with fewer JVM warnings
alias cardano-prism='cs launch --java-opt --sun-misc-unsafe-memory-access=allow app.fmgp::cardano-prism-cli:0.1.0-M42 -M fmgp.did.method.prism.cli.PrismCli --'

cardano-prism --help
```

### Install with Homebrew

A brew tap (`homebrew-fmgp`) is available for installing `cardano-prism`:

```bash
brew install FabioPinheiro/fmgp/cardano-prism
cardano-prism --help
```

## Configuration File

The CLI uses a JSON configuration file (staging state) to persist keys, wallets, and Blockfrost API tokens between commands.

**Default location:** `~/.cardano-prism-config.json`

### Create the config file

```bash
cardano-prism config-file --create
```

### View the current config

```bash
cardano-prism config-file
```

The config file stores:
- SSI and Cardano wallet mnemonics
- Derived private keys (Secp256k1, Ed25519, X25519)
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

Key usages: `Master`, `Issuing`, `Keyagreement`, `Authentication`, `Revocation`, `Capabilityinvocation`, `Capabilitydelegation`, `Vdr`

### Derive a Secp256k1 key

The Master key **must** be of type `secp256k1`.

```bash
# cardano-prism key secp256k1 [--label <name>] <DID-index> <keyUsage> <key-index>
cardano-prism key sepc256k1 0 Master 0
# Saves key as "key-0-Master-0"

cardano-prism key sepc256k1 --label my-master-key 0 Master 0
```

### Derive an Ed25519 key

```bash
# cardano-prism key Ed25519 [--label <name>] <DID-index> <keyUsage> <key-index>
cardano-prism key Ed25519 0 Issuing 0
# Saves key as "key-0-Issuing-0"

cardano-prism key Ed25519 0 Authentication 0
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

```bash
# cardano-prism did create-deterministic [-S <id>=<endpoint>] <keyID>...
cardano-prism did create-deterministic key-0-Master-0

# With a DIDComm service endpoint
cardano-prism did create-deterministic -S e1=https://did.example.com key-0-Master-0

# With multiple keys and a service
cardano-prism did create-deterministic -S e1=https://did.example.com key-0-Master-0 key-0-Issuing-0
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

Submit hex-encoded PRISM signed events to the Cardano blockchain. Requires both a Cardano wallet (for fees) and a Blockfrost token for the target network.

```bash
# cardano-prism cardano submit --network <network> <event-hex>...
cardano-prism cardano submit --network preprod <create-event-hex> <update-event-hex>
```

On success, outputs a transaction hash and a link to Cardanoscan.

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

```bash
# 1. Create the config file
cardano-prism config-file --create

# 2. Generate a new SSI mnemonic and save it
cardano-prism mnemonic new -s

# 3. Derive a Master key (secp256k1 required for master)
cardano-prism key sepc256k1 0 Master 0
# => saves as "key-0-Master-0"

# 4. Derive additional keys (optional)
cardano-prism key Ed25519 0 Issuing 0
# => saves as "key-0-Issuing-0"

# 5. Generate a new Cardano payment wallet (for transaction fees)
cardano-prism mnemonic new -s --wallet-type ada

# 6. Save the Blockfrost API token for preprod
cardano-prism cardano blockfrost-token -s preprod preprod<YOUR_TOKEN>

# 7. Create the deterministic DID
cardano-prism did create-deterministic \
  -S endpoint1=https://my-didcomm-endpoint.example.com \
  key-0-Master-0 key-0-Issuing-0
# => prints the DID and the create/update SignedPrismEvent hex values

# 8. Submit events to the blockchain
cardano-prism cardano submit --network preprod <event1-hex> <event2-hex>
# => prints the transaction hash
```

After the transaction is confirmed, you can resolve the DID:

```bash
cardano-prism did resolve-prism --network preprod did:prism:<hash>
```



## Example to create a DID
cardano-prism mnemonic new -s
cardano-prism key sepc256k1 0 Master 0
cardano-prism key Ed25519 0 Issuing 0
cardano-prism did create-deterministi -S e1=https://did.fabiopinheiro.com/ key-0-Master-0 key-0-Issuing-0
cardano-prism cardano blockfrost-token -s preprod preprod9EGSSMf6oWb81qoi8eW65iWaQuHJ1HwB
cardano-prism cardano submit --network preprod 0a066d617374657212473045022100b32b3dfc1fb47dc102038c1cbc1571b955f0ee7bab27e8b9626f8da62c50a4d6022050dfa98afdfe7503dbe58ed9ae20addb6d52a182521cd67e9d4bb6b79629b0f41a400a3e0a3c123a0a066d617374657210014a2e0a09736563703235366b31122103ebe0934672da51ca01da94d278376a204e0e73a8d235c290bc2d5f1a629f8aec 0a066d6173746572124730450221008c036641c84c182aba386e004803684b069004e192b860542a23f16de023351102201e66c385924c88ac26aeaea76140df6fcb3d8de926091b72b983bf864305a8801afa0112f7010a20aab8d14fb60c035ec589195b407978d6ec1316e183ba6fbb920a0a7ff03264221240616162386431346662363063303335656335383931393562343037393738643665633133313665313833626136666262393230613061376666303332363432321a4b1a490a470a09656e64706f696e74311210444944436f6d6d4d6573736167696e671a287b22757269223a2268747470733a2f2f6469642e666162696f70696e686569726f2e636f6d2f227d1a440a420a400a0f6b65792d302d49737375696e672d3010024a2b0a0745643235353139122018a25f3a42089ae9d41f2ea40549bf045c12021923a4b760487d0237e0e36e40


SSI: did:prism:aab8d14fb60c035ec589195b407978d6ec1316e183ba6fbb920a0a7ff0326422
create SignedPrismEvent:
0a066d617374657212473045022100b32b3dfc1fb47dc102038c1cbc1571b955f0ee7bab27e8b9626f8da62c50a4d6022050dfa98afdfe7503dbe58ed9ae20addb6d52a182521cd67e9d4bb6b79629b0f41a400a3e0a3c123a0a066d617374657210014a2e0a09736563703235366b31122103ebe0934672da51ca01da94d278376a204e0e73a8d235c290bc2d5f1a629f8aec
update SignedPrismEvent:
0a066d6173746572124730450221008c036641c84c182aba386e004803684b069004e192b860542a23f16de023351102201e66c385924c88ac26aeaea76140df6fcb3d8de926091b72b983bf864305a8801afa0112f7010a20aab8d14fb60c035ec589195b407978d6ec1316e183ba6fbb920a0a7ff03264221240616162386431346662363063303335656335383931393562343037393738643665633133313665313833626136666262393230613061376666303332363432321a4b1a490a470a09656e64706f696e74311210444944436f6d6d4d6573736167696e671a287b22757269223a2268747470733a2f2f6469642e666162696f70696e686569726f2e636f6d2f227d1a440a420a400a0f6b65792d302d49737375696e672d3010024a2b0a0745643235353139122018a25f3a42089ae9d41f2ea40549bf045c12021923a4b760487d0237e0e36e40


https://preprod.cardanoscan.io/transaction/f404044f00cae9146d3e9f451e7e26eb2aba80ce6f0fe9a50c57b48d0e0ad9fa?tab=metadata
https://neoprism-preprod.patlo.dev/explorer
https://neoprism-preprod.patlo.dev/resolver?did=did:prism:aab8d14fb60c035ec589195b407978d6ec1316e183ba6fbb920a0a7ff0326422
https://neoprism.patlo.dev/explorer