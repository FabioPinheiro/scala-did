# 🤖 Agent Guide: Working in the Scala DID Repository

This document provides non-obvious context, conventions, and commands necessary for an AI Agent to effectively navigate and modify this codebase. Focus on *why* things are done, not *what* the code looks like in isolation.

## 🧭 Project Overview & Architecture

This repository is a highly modular, polyglot project focused on Decentralized Identifiers (DIDs) and DID Communications (DIDComm).

*   **Core Structure**: The project is divided into numerous sub-modules (e.g., `did`, `did-comm-protocols`, `did-method-prism`, `did-method-web`, `cardano-prism-cli`).
*   **Polyglot Nature**: It supports JVM (Scala/Java), JavaScript (Scala.js/TypeScript via Vite), and CLI tooling.
*   **Interoperability**: Modules are interconnected using `crossProject` in `build.sbt`, linking JVM and JS components.
*   **Architectural Flow**: The system primarily revolves around DID resolution, DID document management, and secured DIDComm message exchange.
*   **Key Dependencies**: ZIO is the primary asynchronous programming framework. Crypto operations rely on a mix of BouncyCastle and NimbusJoseJwt.

## 🛠️ Essential Commands

| Command | Description | Notes |
| :--- | :--- | :--- |
| `sbt testJVM` | Runs all JVM unit/integration tests. | Includes `didCommProtocolsJVM/test`, etc. |
| `sbt testJS` | Runs all JavaScript/Scala.js tests. | |
| `sbt testAll` | Runs all tests across both JVM and JS platforms. | |
| `sbt docAll` | Generates API documentation using Mdoc. | |
| `sbt siteAll` | Generates the documentation website using Laika. | |
| `sbt assemblyAll` | Builds documentation AND the frontend website. | A composite command. |
| `sbt live` | Starts services for local development. | Note: Requires `fastPackAll` to run first. |
| `sbt ciJobLib` | Compiles and tests the core JVM libraries. | Used for CI pipelines. |
| `sbt ciJobFrontend` | Installs NPM dependencies and builds the frontend assets. | Must run before `sbt live`. |

## ⚙️ Code Patterns & Conventions

*   **DID Parsing**: Specific DID methods (e.g., `did:prism:`) require strict, method-specific regex patterns and parsing logic (short form vs. long form). Relying on generic DID parsing is a common failure point.
*   **Asynchronicity**: Core logic heavily leverages ZIO's effect system. When modifying data flow, be mindful of effect handling patterns like `.mapOrFail`, `ZIO.succeed`, and error propagation.
*   **Serialization**: Serialization is managed via ZIO JSON (`JsonDecoderExtension.scala`). Ensure any new data structures correctly implement or integrate with the existing decoder/encoder patterns.
*   **Dependency Management**: Dependencies are managed via a centralized `D` object in `build.sbt`, which also tracks external NPM dependencies (`NPM` object).
*   **CLI Entry Points**: CLI commands are organized by module (e.g., `cardano-prism-cli/src/main/scala/fmgp/did/method/prism/cli/`). Follow this structure for adding new commands.

## ⚠️ Critical Gotchas & Non-Obvious Knowledge

1.  **Assembly Conflicts**: The `assemblyMergeStrategy` in `build.sbt` is heavily customized. This is due to dependency conflicts (e.g., BouncyCastle versions, Protobuf versions) between different modules. Any change to core dependencies *must* be checked against this merge strategy.
2.  **Vulnerability Remediation**: Modules like `did-imp` contain explicit comments noting CVEs (e.g., CVE-2023-2976). Fixing these requires careful dependency updates as documented in the build file.
3.  **Frontend Build Order**: Running `sbt live` without first running `sbt ciJobFrontend` will fail because the necessary JavaScript assets are not built/installed.
4.  **Progressive Disclosure**: When implementing a new DID method or protocol, the specific regex and parsing logic are the most critical pieces of knowledge, as generic DID logic will fail.
5.  **TODOs**: Be aware of existing `TODO`s in files like `DIDPrism.scala` related to long-form DID support and event processing; these represent known incomplete features.

## 🧪 Testing Approach

*   **Unit/Integration**: Tests are primarily located in `*Suite.scala` files within the respective module's `shared/src/test/scala/fmgp/did/` directory.
*   **Test Tags**: Tests are sometimes tagged (e.g., `IntregrationTest`). Commands like `sbt testJVM` allow filtering these tags.
*   **Test Dependencies**: Some modules require specific external tools or dependencies (e.g., `did-method-prism/shared` requires `scalus`). Ensure dependencies are correctly configured before running tests for that module.