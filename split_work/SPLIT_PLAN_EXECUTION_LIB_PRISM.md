# Library + PRISM split execution

This file records the Stage 2 work on `split/lib_and_prism`. This branch is a
library/docs/CLI candidate: it removes the Sandbox while deliberately retaining
`cardano-prism-cli` and `cardano-prism-cip30-webapp` for the later
`split/prism` extraction.

## State

- Branch: `split/lib_and_prism`
- Started from: `907f5deb3` (`build: add SPLIT_PLAN.md`)
- Completed in: `f8f4b435d` (`build: split plan - lib & prism`)

The Stage 2 extraction changes below were committed in `f8f4b435d`. This
record is retained as the Stage 2 hand-off to the subsequent PRISM and library
extractions.

## Actions taken

### Removed Sandbox ownership

The following tracked Sandbox paths were deleted:

- `did-example/`
- `did-experiments/`
- `serviceworker/`
- `webapp/`
- `demo/`
- `vite/`
- `vite.config.js`

`demo/` is the source path in this branch. `backend/` is not a tracked source
path here; the Sandbox branch owns its renamed backend application source.

### Reduced the build and tooling

- Removed `didExample`, `didExperiments`, `serviceworker`, `webapp`, and
  `demo` project definitions and their root aggregation.
- Removed Sandbox-only aliases and tasks: `assemblyAll`, `live`,
  `ciJobFrontend`, `fastPackAll`, `fullPackAll`, `installFrontend`, and
  `buildFrontend`.
- Removed Vite-only ScalablyTyped configuration and the demo assembly/runtime
  configuration, including generated docs/Vite resource directories.
- Removed the unused demo Laika dependency and obsolete `sbt-revolver`,
  `sbt-gzip`, `sbt-jsdependencies`, Scala.js bundler, jsdom, Scalafix, and
  conditional dependency-submission plugins. `sbt-assembly` remains because
  the retained CLI uses it.
- Retained all library projects, docs configuration, `cardanoPrismCli`,
  `cardanoPrismCip30Webapp`, and the CLI resource generator that embeds the
  CIP-30 bundle in the CLI jar.

### Reduced npm and CI configuration

- Regenerated the root `package-lock.json` after removing Vite/PWA/UI-only
  dependencies. The remaining root packages are `@noble/curves`, `jose`,
  `js-sha256`, and `typescript`, which are required by retained Scala.js
  libraries and ScalablyTyped.
- Changed library CI and dependency-submission setup from `npm install` to
  `npm ci`.
- Removed serviceworker/webapp linking, Vite production build, and demo
  assembly from CI.
- Removed deleted project IDs from dependency-submission ignores.

### Updated documentation

- Removed README links and dependency-graph nodes for the deleted Sandbox
  modules.
- Removed Sandbox-only commands and module descriptions from documentation.
- Updated the documentation site to describe the DIDComm Sandbox as an
  external application, rather than content of this repository.

## Validation completed

- `npm ci`
- `sbt clean testJS`
- `SKIP_INTEGRATION_TEST=true sbt testJVMNoDB`
- `sbt "scalafmtCheckAll;docAll;siteAll"`
- `sbt "scalafmtSbtCheck;scalafmtCheckAll;cardanoPrismCli/assembly"`
- `sbt cardanoPrismCli/assembly`
- Verified the assembled CLI jar contains `cip30/bundle.js` and
  `cip30/bundle.js.map`.
- `git diff HEAD --check`
- Confirmed no tracked Sandbox source/build paths remain and the retained CLI
  paths are still tracked.

An unfiltered `sbt testJVM` was also attempted. It fails only in
`PrismStateMongoDBSuite` because no local MongoDB primary is available. The
CI-style filtered JVM validation above passes.

## Relationship to `split/sandbox`

The Sandbox execution record at commit
`2142bb2b957dae212d920962a05731956bb3b221` is complementary to this work:

- It retains the Sandbox application (with its backend source), fixtures,
  service worker, webapp, and Vite pipeline; this branch removes the
  corresponding baseline source paths and build edges.
- It removes library/docs/CLI source projects; this branch retains those
  projects and removes the Sandbox-only build and CI work.
- Its `backend` name replaces the baseline `demo` application path. There is
  no tracked `backend` source in this branch, so there is no overlapping
  application source ownership.

The following cross-branch or plan-level issues remain before the staged
migration can be considered complete:

1. **Stage 0 artifact baseline is not yet proven.** The Sandbox record says
   `did-method-web` `0.1.1` is unavailable and accepts a Uniresolver fallback.
   This conflicts with the plan's Stage 0 requirement to publish that module
   for both JVM and Scala.js consumers. This branch still has
   `did-method-web` configured with `publish / skip := true`; the Stage 0
   publishing correction must be made on the release baseline, not treated as
   complete here.
2. **Sandbox documentation redirects do not match the stated destination
   shape.** The Sandbox implementation appends the complete request path, so
   `/doc/x` redirects to `https://doc.did.fmgp.app/doc/x`; the plan requires
   `https://doc.did.fmgp.app/x`. The Sandbox execution record already flags
   this for correction.
3. **Sandbox versioning differs from the plan.** Its record reports
   `1.0-SNAPSHOT`, while the plan specifies an initial application version of
   `1.0`; this needs an explicit release-policy decision.
4. **External release proof is still outstanding.** Dedicated clean
   Coursier/Ivy-cache consumers, Maven/POM-closure inspection, and the
   no-`publishLocal` proof remain Stage 0 work.
5. **CLI extraction remains Stage 3 work.** `split/prism` must replace its
   source-project dependencies with Maven artifacts and commit/use a CIP-30
   `package-lock.json` with `npm ci`. This branch intentionally preserves the
   current local CLI/CIP-30 build until that extraction.

## Exit-condition assessment

The Sandbox-removal portion of Stage 2 is satisfied: this branch has no
tracked Sandbox source or build dependency and retains the library/docs/CLI
boundary. Full migration readiness remains conditional on the unresolved Stage
0 artifact baseline and the later Stage 3 CLI extraction.
