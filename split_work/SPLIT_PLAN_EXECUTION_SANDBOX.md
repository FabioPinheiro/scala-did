# Sandbox split execution

This file records the work completed on `split/sandbox` while extracting the
Scala-DID Sandbox application. The branch keeps the Sandbox application and
consumes Scala-DID through Maven; it does not retain library source projects.

## Actions taken

- Removed non-Sandbox source modules, documentation, CLI/CIP-30 applications,
  publication configuration, and their related build/workflow configuration.
- Retained the Sandbox application: `did-example`, `serviceworker`, `webapp`,
  `backend`, `vite`, and `vite.config.js`.
- Replaced source-project library edges with versioned `app.fmgp` Maven
  dependencies. The shared baseline is `V.scalaDidVersion = "0.1.1"`.
- Kept only local project edges:

  ```text
  webapp   -> did-example JS + serviceworker
  backend -> did-example JVM
  ```

- Replaced documentation resources embedded in the demo jar with redirects to
  `https://doc.did.fmgp.app`. `/doc` and `/api` preserve suffixes and query
  strings; `/apis` intentionally returns `410 Gone`.
- Corrected the Docker assembly input path for Scala 3.9.0.
- Reworked CI as an application workflow: npm install, formatting, compile and
  test, Scala.js linking, Vite production build, backend assembly, Docker image
  build, and container health-check smoke test.

## Decisions and deferred work

- The Uniresolver fallback for `did:web` is accepted while Maven Central lacks
  a `did-method-web` 0.1.1 artifact.
- The Sandbox remains at `1.0-SNAPSHOT`. Its application versioning policy is
  deferred and will require custom release/version code.

## Follow-up questions

- Confirm the documentation redirect shape. The split plan maps `/doc/x` to
  `https://doc.did.fmgp.app/x`, while the current compatibility route preserves
  the `/doc` prefix and redirects to `https://doc.did.fmgp.app/doc/x`.
- Decide whether the webapp needs browser integration tests. It has no browser
  test sources, so `webapp / Test / test` is disabled; Scala.js linking and the
  Vite production build remain CI checks.
- `sbt-converter` is required by the existing `typings.*` imports and is pinned,
  but its current beta release is resolved through the Sonatype snapshot
  resolver. Revisit this when a Central-only release is practical.
- Before cutover, run the split plan's clean dedicated Coursier/Ivy cache proof
  to demonstrate Maven-only resolution without any local checkout or
  `publishLocal` fallback.
- Docker build and container health checks still need CI validation because the
  local environment has no Docker executable.

## Removed folders and files

### Application/library boundaries

- `cardano-prism-cli/`
- `cardano-prism-cip30-webapp/`
- `did/`
- `did-comm-protocols/`
- `did-experiments/`
- `did-framework/`
- `did-imp/`
- `did-method-peer/`
- `did-method-prism/`
- `did-method-web/`
- `did-uniresolver/`
- `multiformats/`
- `docs/`
- `interoperability_check/`

### Obsolete release/documentation configuration

- `PUBLISH.md`
- `project/ManualSettings.scala`
- `.github/workflows/scala-steward.yml`
- `.scala-steward.conf`

The deleted library projects remain in the repository history, as required by
the split plan; this branch simply no longer checks them out as active source
or build modules.

## Modified files and goals

| Path | Goal |
| --- | --- |
| `build.sbt` | Define the standalone Sandbox build, Maven dependencies, local project graph, Scala.js linking, frontend packaging, backend assembly, and required assembly merge strategies. |
| `project/plugins.sbt` | Retain only Scala.js/cross-project, formatting, BuildInfo, assembly, and ScalablyTyped plugins needed by the Sandbox. |
| `README.md` | Describe the Sandbox application, its Maven baseline, build commands, generated jar, and documentation routing. |
| `package.json` | Rename/version the npm application as `scala-did-sandbox`. |
| `package-lock.json` | Keep the lockfile's root package metadata aligned with `package.json`. |
| `.gitignore` | Remove documentation-specific ignore rules and retain Sandbox build/editor/generated-file ignores. |
| `.github/workflows/ci.yml` | Replace library publication/docs deployment CI with Sandbox build, package, Docker, and smoke-test checks. |
| `.github/workflows/sbt-dependency-submission.yml` | Restrict dependency-submission ignores to Sandbox projects and use `npm ci`. |
| `backend/Dockerfile` | Copy the actual Scala 3.9.0 assembly output. The Docker build context is `backend/`. |
| `backend/src/main/scala/fmgp/did/demo/AppServer.scala` | Register redirect routes rather than resource-backed generated documentation routes. |
| `backend/src/main/scala/fmgp/did/demo/DocsApp.scala` | Implement documentation redirects and retire the obsolete `/apis` endpoint. |
| `backend/src/test/scala/fmgp/did/demo/DocsAppSuite.scala` | Test `/doc`, `/doc/x`, `/api`, `/api/x`, query-string preservation, and `/apis` retirement. |
| `webapp/src/main/scala/fmgp/webapp/Global.scala` | Use the accepted Uniresolver fallback for `did:web` while the published baseline has no `did-method-web` artifact. |

## `build.sbt` structure

### Version and external Scala-DID libraries

`V.scalaDidVersion` is the single version setting for released Scala-DID
artifacts, currently `0.1.1`. `didExample` declares the application-facing
modules using `"app.fmgp" %%% module % V.scalaDidVersion`; `%%%` resolves JVM
artifacts for its JVM project and Scala.js artifacts for its JS project.

The Sandbox does **not** use `dependsOn` for a Scala-DID source checkout.
Maven is the only library source. The `did-method-web` module is not declared
because Maven Central does not provide its `0.1.1` JVM or Scala.js artifact.
The web application uses the existing Uniresolver fallback for `did:web`.

### Local projects

- `didExample` is a JVM/Scala.js cross-project containing Sandbox fixtures and
  examples.
- `serviceworker` is a local Scala.js project with its ESM module initializer.
- `webapp` is a local Scala.js/Vite project. It depends only on
  `didExample.js` and `serviceworker`.
- `backend` is a JVM-only project. It packages the server and Vite output, and
  depends only on `didExample.jvm`.
- `root` aggregates those application projects and is not publishable.

`fullPackAll` links the service worker and webapp with Scala.js full
optimisation. The webapp has no browser test sources, so its `Test / test` task
is intentionally empty; Vite production packaging validates the browser
application.

The remaining assembly merge strategies are required to produce the demo jar
from the released Scala-DID dependency graph. Documentation resource folders,
Mdoc/Laika/Unidoc tasks, Maven publication settings, backend live-reload tasks,
and CLI/CIP-30 tasks are removed.

## SBT plugins

| Plugin | Why it remains |
| --- | --- |
| `sbt-scalajs-crossproject` | Defines the JVM/Scala.js cross-project for `did-example`. |
| `sbt-scalajs` | Compiles and links the service worker and webapp. |
| `sbt-scalafmt` | Provides local and CI formatting checks. |
| `sbt-buildinfo` | Generates the BuildInfo consumed by the webapp. |
| `sbt-assembly` | Packages the demo server jar used by Docker deployment. |
| `sbt-converter` (ScalablyTyped) | Generates Scala.js facades for webapp npm dependencies such as Mermaid, Material Components, QR Scanner, and QR Code. It uses the root npm installation through `externalNpm`. |

Removed plugins covered publishing (`sbt-ci-release`), documentation
(Mdoc/Laika/Unidoc), code coverage, ScalaPB/protoc, dynamically-loaded release
configuration, webpack bundling, live reload, dependency updates, and other
library/CLI-only tooling.

## Validation completed

- `npm ci --ignore-scripts`
- `sbt compile test`
- `sbt fullPackAll`
- `npm run build`
- `sbt backend/assembly`
- `sbt scalafmtCheckAll`
- `git diff --check`

Docker build and container smoke testing were not run locally because the
Docker executable is unavailable. They are included in the Sandbox CI workflow.

## others

pi --session 01a03638-10e8-7696-92b6-4b3a39b2aa2e
