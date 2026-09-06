# PRISM split execution

This file records the Stage 3 work on `split/prism`. The branch is now the
Cardano PRISM CLI application: it retains the CLI and its embedded CIP-30
browser bundle, consumes Scala-DID from Maven Central, and contains no
Sandbox/demo or Scala-DID library source projects.

## State

- Branch: `split/prism`
- Starting commit: `f8f4b435d` (`build: split plan - lib & prism`), the same
  commit as `split/lib_and_prism` when this work started.
- Scala-DID baseline: `0.1.1`
- Application version: `1.0`
- The changes recorded here are currently working-tree changes and have not
  been committed by this work.

## Actions taken

### Removed non-PRISM ownership

Deleted all remaining Scala-DID library/docs paths and Sandbox residue:

- libraries: `did`, `did-comm-protocols`, `did-framework`, `did-imp`,
  `multiformats`, `did-method-peer`, `did-method-prism`, `did-method-web`, and
  `did-uniresolver`;
- documentation and the interoperability check;
- any remaining ignored Sandbox/demo paths (`backend`, `did-example`,
  `did-experiments`, `serviceworker`, `webapp`, and `vite`);
- root library npm manifest/lockfile, library documentation configuration,
  Scala Steward configuration, and the Stage 2 execution record.

The detailed CLI guide was moved from
`docs/src/09-cardano-prism-cli/01-cardano-prism-cli.md` to
`cardano-prism-cli/README.md`.

### Created a standalone CLI build

- Replaced the root build with two local application projects only:
  `cardanoPrismCli` and `cardanoPrismCip30Webapp`.
- Replaced all Scala-DID source `dependsOn` edges with `app.fmgp` Maven
  dependencies at the single `scalaDidVersion = "0.1.1"` setting:

  ```text
  cardanoPrismCli          -> did, did-method-prism, did-method-peer, did-uniresolver (JVM)
  cardanoPrismCip30Webapp  -> did-method-prism (Scala.js)
  ```

- Kept the CLI resource generator: it production-links the CIP-30 Scala.js
  project, invokes esbuild, and packages `cip30/bundle.js` and
  `cip30/bundle.js.map` in `cardano-prism.jar`.
- Reduced SBT plugins to Scala.js, Scalafmt, BuildInfo, assembly, and CLI
  publication. Removed cross-project, ScalablyTyped, ScalaPB/protoc,
  documentation, coverage, and library-only plugin configuration.
- Retained only CLI application dependencies and the assembly merge strategies
  required by the Maven-resolved runtime closure.
- Corrected a CLI for-comprehension binding in `DIDCommand.scala` that no
  longer typechecked when the PRISM API is consumed from its published jar.

### Made the CIP-30 bundle reproducible

- Committed `cardano-prism-cip30-webapp/package-lock.json` and removed its
  ignore rule.
- Changed the bundle task and CI to use `npm ci`, not `npm install`.
- Kept the required browser npm dependencies (`@noble/curves`,
  `@noble/hashes`, `js-sha256`, and esbuild) in the CIP-30 package manifest and
  lockfile.

### Reworked application documentation and CI

- Replaced the root README with CLI-specific build instructions and the
  library-documentation link.
- Replaced library/docs CI with CLI formatting, compile/test, CIP-30
  install/link/bundle, assembly, embedded-resource assertion, and
  `version`/`--help` smoke commands. A tag-triggered application-release job
  publishes only `cardanoPrismCli` through `sbt ci-release`.
- Reduced dependency submission to the two remaining application projects.

## Validation completed

- `npm --prefix cardano-prism-cip30-webapp ci`
- `sbt scalafmt`
- `sbt -v "compile;test"`
- `COURSIER_CACHE=<fresh> sbt -Dsbt.ivy.home=<fresh> -batch "clean;cardanoPrismCli/compile"`
- `sbt -v cardanoPrismCli/assembly`
- Verified `cip30/bundle.js` and `cip30/bundle.js.map` in the assembled jar
  with `unzip -Z1`.
- `java -jar cardano-prism-cli/target/scala-3.9.0/cardano-prism.jar version`
  (`1.0`)
- `java -jar cardano-prism-cli/target/scala-3.9.0/cardano-prism.jar --help`
- `git diff --check`

The clean-cache JVM compile and assembled CLI consumed the Maven `0.1.1`
artifacts; no local Scala-DID source project remains in the build. The broader
Stage 0 proof is still a cutover prerequisite: it must also cover the
Sandbox-shaped consumer, Maven/POM closure inspection, and the required
Scala.js consumer proof under dedicated caches.

## Review of the other execution records

### `SPLIT_PLAN_EXECUTION_LIB_PRISM.md`

The Stage 2 ownership description is consistent with this extraction: it
removes Sandbox paths while retaining the CLI/CIP-30 pair, which is exactly the
starting boundary used here. Its stated remaining Stage 3 work is now addressed
by this file.

Its **State** section is stale: it says the Stage 2 changes were uncommitted
working-tree changes recorded from `907f5deb3`. In this checkout they are
already committed as `f8f4b435d`, and `split/prism` initially pointed at that
same commit as `split/lib_and_prism`. The record should therefore be treated as
an account of Stage 2 actions, not as the current commit status.

### Sandbox record (`2142bb2b957dae212d920962a05731956bb3b221`)

The Sandbox ownership is complementary: it retains the Sandbox application
while this branch contains neither Sandbox source nor build edges. Its explicit
`/apis` retirement (`410 Gone`) is a valid decision for the plan's request to
decide that route.

The following plan-level items remain inconsistent or incomplete in the
Sandbox record and are not masked by this PRISM extraction:

1. It accepts a `did:web` Uniresolver fallback because `did-method-web` 0.1.1
   was unavailable, but Stage 0 requires publishing and proving that JVM and
   Scala.js artifact before cutover.
2. It says `/doc/x` redirects to `https://doc.did.fmgp.app/doc/x`; the plan
   requires stripping the `/doc` prefix (`https://doc.did.fmgp.app/x`).
3. It uses `npm install` in CI, while the split plan requires a reproducible
   `npm ci` application build.
4. It records application version `1.0-SNAPSHOT`, whereas the plan specifies
   the initial application version as `1.0`; release policy needs an explicit
   decision.
5. Its Docker/container smoke test and the clean dedicated Maven-resolution
   proof remain pending, as the record itself notes.

## Exit-condition assessment

The Stage 3 branch boundary and local CLI packaging condition are satisfied:
the checkout contains only the PRISM CLI/CIP-30 application projects, the jar
has the embedded browser bundle, and the build resolves Scala-DID through Maven
coordinates. Migration readiness still depends on the shared Stage 0 release
baseline proof and the pending Sandbox fixes above.
