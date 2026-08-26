# 🤖 Agent Guide: Working in the Scala DID Repository

This document provides non-obvious context, conventions, and commands necessary for an AI Agent to effectively navigate and modify this codebase. Focus on *why* things are done, not *what* the code looks like in isolation.

## 🧭 Project Overview & Architecture

This repository is a highly modular, polyglot project focused on Decentralized Identifiers (DIDs) and DID Communications (DIDComm).

*   **Core Structure**: The project is divided into library modules (e.g., `did`, `did-comm-protocols`, `did-method-prism`, and `did-method-web`).
*   **Cross-platform Nature**: The libraries support JVM (Scala/Java) and Scala.js.
*   **Interoperability**: Modules are interconnected using `crossProject` in `build.sbt`, linking JVM and JS components.
*   **Architectural Flow**: The system primarily revolves around DID resolution, DID document management, and secured DIDComm message exchange.
*   **Key Dependencies**: ZIO is the primary asynchronous programming framework. Crypto operations rely on a mix of BouncyCastle and NimbusJoseJwt.

## 🛠️ Essential Commands

| Command | Description | Notes |
| :--- | :--- | :--- |
| `sbt testJVM` | Runs all JVM unit/integration tests. | Includes `didCommProtocolsJVM/test`, etc. |
| `sbt testJS` | Runs all JavaScript/Scala.js tests. | |
| `sbt testAll` | Runs all tests across both JVM and JS platforms. | |
| `sbt testAllNoDB` | Runs all no DB related tests across both JVM and JS platforms. | |
| `sbt docAll` | Generates API documentation using Mdoc. | |
| `sbt siteAll` | Generates the documentation website using Laika. | |
| `sbt ciJobLib` | Compiles and tests the core JVM libraries. | Used for CI pipelines. |

## ⚙️ Code Patterns & Conventions

*   **DID Parsing**: Specific DID methods (e.g., `did:prism:`) require strict, method-specific regex patterns and parsing logic (short form vs. long form). Relying on generic DID parsing is a common failure point.
*   **Asynchronicity**: Core logic heavily leverages ZIO's effect system. When modifying data flow, be mindful of effect handling patterns like `.mapOrFail`, `ZIO.succeed`, and error propagation.
*   **Serialization**: Serialization is managed via ZIO JSON (`JsonDecoderExtension.scala`). Ensure any new data structures correctly implement or integrate with the existing decoder/encoder patterns.
*   **Dependency Management**: Dependencies are managed via a centralized `D` object in `build.sbt`, which also tracks external NPM dependencies (`NPM` object).

## ⚠️ Critical Gotchas & Non-Obvious Knowledge

1.  **Vulnerability Remediation**: Modules like `did-imp` contain explicit comments noting CVEs (e.g., CVE-2023-2976). Fixing these requires careful dependency updates as documented in the build file.
2.  **Progressive Disclosure**: When implementing a new DID method or protocol, the specific regex and parsing logic are the most critical pieces of knowledge, as generic DID logic will fail.
3.  **TODOs**: Be aware of existing `TODO`s in files like `DIDPrism.scala` related to long-form DID support and event processing; these represent known incomplete features.

## 🧪 Testing Approach

*   **Unit/Integration**: Tests are primarily located in `*Suite.scala` files within the respective module's `shared/src/test/scala/fmgp/did/` directory.
*   **Test Tags**: Tests are sometimes tagged (e.g., `IntregrationTest`). Commands like `sbt testJVM` allow filtering these tags.
*   **Test Dependencies**: Some modules require specific external tools or dependencies (e.g., `did-method-prism/shared` requires `scalus`). Ensure dependencies are correctly configured before running tests for that module.