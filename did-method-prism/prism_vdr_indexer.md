# PRISM VDR Indexer

## DID PRISM

https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md

## Indexer Architecture
```mermaid
flowchart TD
  Cardano@{ shape: cyl, label: "Cardano Transactions" }
  Blocks@{ shape: docs, label: "Prism Blocks" }
  Operations@{ shape: processes, label: "Prism Operations" }

  OpCreate@{ shape: rect, label: "Create Operation" }
  OpUpdate@{ shape: rect, label: "Update Operation" }
  OpDeactivate@{ shape: rect, label: "Deactivate Operation" }
  OpOther@{ shape: rect, label: "Other Operations ..." }

  OpCreateD@{ shape: diamond, label: "If SSI state is Empty\n and based on itself" }
  OpUpdateD@{ shape: diamond, label: "Decision\n Based on the\n SSI state" }
  OpDeactivateD@{ shape: diamond, label: "Decision\n Based on the\n SSI state"}


  SSI@{ shape: notch-rect, label: "SSI" }
  DID@{ shape: notch-rect, label: "DID Document" }

  SSI_Op@{ shape: bow-rect, label: "SSI Operations Storage" }
  SSI_State@{ shape: bow-rect, label: "SSI State Storage" }
  DID_DB@{ shape: bow-rect, label: "DID Documents Storage" }
  Transactions@{ shape: bow-rect, label: "Transactions Storage" }
  RawBlocks@{ shape: bow-rect, label: "Raw Blocks" }
  

  Cardano --Filter by Metadata with Label 21325--> Blocks
  Blocks --> Operations
  Blocks --> RawBlocks
  Operations -.-> OpCreate & OpUpdate & OpDeactivate & OpOther

  OpCreate --Create Entry--> OpCreateD
  OpCreateD --> SSI

  OpUpdate --> OpUpdateD
  OpUpdateD --Update Entry--> SSI

  OpDeactivate --> OpDeactivateD
  OpDeactivateD --Update Entry--> SSI
  

  SSI --> DID
  DID --> DID_DB
  SSI ---> SSI_State
  SSI ---> SSI_Op
  Operations -.-> SSI_Op

  Cardano --------> Transactions

```


rm cardano-21325
rm prism-operations
rm -rf diddoc
rm -rf opid
rm -rf ops
rm -rf ssi

mkdir diddoc
mkdir opid
mkdir ops
mkdir ssi