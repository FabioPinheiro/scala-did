# Staged three-repository migration plan

## Goal

Perform the migration in stages, entirely on branches in this repository. Do
not change `master` during the extraction work. Only after every branch is
validated are the branches pushed to their final repositories as `master`.

Final dependency direction:

```text
scala-did libraries -> published Maven artifacts -> scala-did-sandbox
                                             \--> cardano-prism-cli
```

Final repositories:

1. **`scala-did`**: reusable libraries and documentation; publishes Maven
   artifacts.
2. **`scala-did-sandbox`**: DIDComm Sandbox/demo application.
3. **`cardano-prism-cli`**: Cardano PRISM CLI plus its embedded CIP-30 browser
   bundle.

The Sandbox and CLI must build from released artifacts only: no local
`scala-did` checkout and no `publishLocal` output.

## Final ownership

### `scala-did`

Keep and publish:

- `did`
- `did-comm-protocols`
- `did-framework`
- `did-imp`
- `multiformats`
- `did-method-peer`
- `did-method-prism`
- `did-method-web`
- `did-uniresolver`
- `docs` and its deployment

The final library repository has no Sandbox, demo, CLI, or CIP-30 application
projects.

### `scala-did-sandbox`

Move:

- `did-example`
- `serviceworker`
- `webapp`
- `backend`
- `vite`
- `vite.config.js`
- `backend/Dockerfile` and `backend/fly.toml`
- Sandbox CI, Docker, and deployment configuration

`did-experiments` is optional. It has no current main-application consumers;
delete it once confirmed unused. If retained temporarily, it is a
non-published Sandbox module.

### `cardano-prism-cli`

Move together:

- `cardano-prism-cli`
- `cardano-prism-cip30-webapp`

They are one application boundary: the CLI builds the CIP-30 Scala.js/esbuild
bundle and embeds `cip30/bundle.js` and its source map in its jar. Move the
detailed CLI guide currently under `docs/src/09-cardano-prism-cli/` here, or
replace it in library docs with a stable link to the CLI repository.

Each final repository owns its own `build.sbt`, `project/` definition,
package manifest(s), and lockfile(s). These files are reduced independently;
they are not copied as a shared set.

## Artifact baseline and versions

`v0.1.1` is the exact pre-split release and tag, called `M`. It is created and
published once from the current `master`.

Before creating `M`:

- remove `notYetPublishedConfigure` from `did-method-web`, which currently
  sets `publish / skip := true`;
- publish JVM and Scala.js artifacts for every application-facing module;
- prove external JVM and Scala.js consumers using clean, dedicated
  Coursier/Ivy caches, no local checkout, and no `publishLocal` resolver;
- verify the complete Maven/POM closure, including transitive
  `did-comm-protocols` and `multiformats` artifacts;
- prove Scala.js linking with explicit npm runtime dependencies. npm
  dependencies are application-build settings, not Maven-transitive metadata.

The Sandbox consumes the released modules used by `did-example` and `backend`:
`did`, `did-imp`, `did-framework`, `did-method-peer`, `did-method-prism`,
`did-method-web`, and `did-uniresolver`.

The CLI consumes released JVM `did`, `did-method-prism`, `did-method-peer`,
and `did-uniresolver`; its CIP-30 project consumes Scala.js
`did-method-prism`. Their transitive artifacts must resolve from Maven.

Versions after the split are independent:

- Sandbox application: `1.0`, initially pinned to Scala-DID `0.1.1`.
- CLI application: `1.0`, initially pinned to Scala-DID `0.1.1`.
- Cleaned library: `v1.1.0`.

The applications upgrade to `1.1.0` independently after its release.

## Branch topology and shared history

Do **not** use `git filter-repo`; it rewrites commit IDs.

Freeze `master` at `M` while the following branches are built and validated:

```text
... existing scala-did history ...
                |
                M  <- v0.1.1; master is frozen
               / \
 split/sandbox   split/lib_and_prism
                    /             \
          split/prism             split/lib
```

- `split/sandbox` is created directly from `M` and extracts the Sandbox.
- Once `split/sandbox` is validated, `split/lib_and_prism` is created directly
  from `M` and removes the Sandbox while retaining the CLI/CIP-30 projects.
- `split/prism` is created from validated `split/lib_and_prism` and extracts
  CLI/CIP-30.
- `split/lib` is created from `split/lib_and_prism` after `split/prism` is
  validated and removes CLI/CIP-30, leaving the final library/docs repository.
- Each branch is an ordinary deletion/rebuild commit history; existing commits
  are not rewritten.
- All final repositories contain `M` and every earlier commit with identical
  SHA, author, timestamp, `git log`, and `git blame` data. Application
  repositories intentionally retain historical files that existed before
  their extraction commit.

## Stage 0: publish and verify `M`

1. Start from a clean `master`.
2. Make the `did-method-web` publishing change and any required release fixes.
3. Tag that exact commit `v0.1.1` (`M`) and push the tag to the original
   repository. Its existing tag workflow publishes the library release.
4. Wait for Central visibility.
5. Run the external proof build for both Sandbox-shaped and CLI-shaped
   consumers:
   - JVM compile;
   - Scala.js compile/link;
   - clean dedicated Coursier/Ivy caches;
   - required npm installs and link verification;
   - resolved dependency/POM inspection proving no local fallback.

**Exit condition:** `v0.1.1` is a usable released baseline for both
applications.

## Stage 1: Sandbox extraction on branch `split/sandbox`

1. Create `split/sandbox` from `M`. Do all Sandbox changes on this branch; do
   not merge it into `master`.
2. Retain only Sandbox-owned paths and a self-contained root build.
3. Replace Scala-DID source-project `dependsOn` entries with versioned
   `app.fmgp` Maven dependencies using one `scalaDidVersion` setting and
   platform-appropriate JVM/Scala.js declarations.
4. Retain only local Sandbox edges:

   ```text
   webapp      -> did-example JS + serviceworker
   backend     -> did-example JVM
   did-example -> published Scala-DID artifacts
   ```

   If `did-experiments` is retained, change it to depend on published `did`,
   retain its direct ZIO Prelude/test dependencies, and compile/test it here.
5. Retain the Scala.js module initializers, Vite Scala.js configuration,
   `externalNpm`, and the root npm manifest/lockfile required by Vite and
   consumed Scala.js libraries.
6. Remove `docs`, Mdoc/Laika/Unidoc settings/plugins,
   `project/ManualSettings.scala`, dynamically loaded `sbt-ci-release`, and
   Maven publication settings from this build.
7. Remove `docs/target` resource directories and the unused Laika dependency
   from `backend`; its assembly packages only `vite/dist` and Sandbox-owned
   resources.
8. Replace `DocsApp` generated-file serving with compatibility redirects:

   | Existing route | Destination |
   |---|---|
   | `/doc/...` | `https://doc.did.fmgp.app/...` |
   | `/api/...` | `https://doc.did.fmgp.app/api/...` |

   Preserve path suffixes and query strings. Test `/doc`, `/doc/x`, `/api`,
   and `/api/x`. Decide `/apis/...` explicitly: deploy it in library docs or
   deprecate it.
9. Correct the Docker Scala 3 artifact path and define its CI build context.
10. Run branch validation: clean `npm ci`, JVM/Scala.js compile/tests,
    serviceworker/webapp linking, Vite production build, demo assembly, Docker
    build, and server/container smoke tests.

**Exit condition:** branch `split/sandbox` builds, packages, and runs using
only released Scala-DID artifacts and never needs `docs/target`.

## Stage 2: library-after-Sandbox branch `split/lib_and_prism`

1. After `split/sandbox` passes, create `split/lib_and_prism` from `M`.
2. Remove the Sandbox-owned paths: `did-example`, `did-experiments` if still
   present, `serviceworker`, `webapp`, `backend`, `vite`, and `vite.config.js`.
   Retain `cardano-prism-cli` and `cardano-prism-cip30-webapp` for this stage.
3. Reduce the root npm manifest/lockfile only after a clean Scala.js library
   build. Retain npm dependencies needed by published Scala.js libraries and
   ScalablyTyped; remove Vite/PWA/UI-only dependencies.
4. Remove Sandbox projects, frontend aliases/tasks, Vite work, demo assembly,
   and demo runtime settings from the build and CI.
5. Keep CLI/CIP-30 build settings, CI, and documentation until Stage 3 is
   complete.
6. Validate the library and its remaining CLI/CIP-30 build from this branch.

**Exit condition:** `split/lib_and_prism` is a library/docs/CLI candidate
with no Sandbox source or build dependency.

## Stage 3: CLI extraction on branch `split/prism`

1. Create `split/prism` from validated `split/lib_and_prism`. Do not merge it
   into `master`.
2. Retain `cardano-prism-cli` and `cardano-prism-cip30-webapp` as two local
   projects in a self-contained application build.
3. Replace source dependencies with versioned released Scala-DID Maven
   dependencies. The CLI JVM project consumes `did`, `did-method-prism`,
   `did-method-peer`, and `did-uniresolver`; CIP-30 consumes Scala.js
   `did-method-prism`.
4. Preserve the CLI resource generator that links CIP-30, runs esbuild, and
   copies `dist/bundle.js` and its source map below `cip30/` in the jar.
5. Preserve the CIP-30 ESM module initializer and esbuild pipeline. Commit a
   reproducible `cardano-prism-cip30-webapp/package-lock.json`: it is currently
   ignored, so remove that ignore rule or deliberately force-add the lockfile.
   Use `npm ci` in CLI CI. Keep and prove required npm packages such as
   `@noble/curves`, `@noble/hashes`, and `js-sha256`.
6. Keep only CLI application tooling: Scala.js, assembly, BuildInfo,
   formatting, and test/runtime tooling. Do not retain cross-project,
   ScalablyTyped, ScalaPB/protoc, documentation, or library-publication
   plugins unless the reduced build demonstrates a requirement.
7. Move the detailed CLI guide into this branch or establish the final external
   documentation URL.
8. Validate in a clean checkout:
   - JVM compile/test;
   - CIP-30 `npm ci`, `fullLinkJS`, and esbuild bundle;
   - CLI assembly;
   - assertion that the jar contains `cip30/bundle.js`;
   - CLI smoke commands.

**Exit condition:** `split/prism` produces a runnable CLI jar with its
embedded CIP-30 bundle using only released Scala-DID artifacts.

## Stage 4: final library branch `split/lib`

1. After `split/prism` passes, create `split/lib` from `split/lib_and_prism`.
2. Remove `cardano-prism-cli` and `cardano-prism-cip30-webapp`.
3. Remove all CLI/CIP-30 build configuration: project definitions, bundle
   tasks, BuildInfo settings, assembly settings and merge strategies, and
   CLI application runtime settings.
4. Remove now-unused plugins, including `sbt-buildinfo` and `sbt-assembly`.
   `sbt-revolver` is removable with the Sandbox demo. Evaluate other plugins
   independently rather than retaining them by default.
5. Update library README, dependency graphs, documentation links, CI, and the
   CLI guide link.
6. Validate library JVM/Scala.js compile/test, documentation generation, and
   artifact publication tasks without any application build work.

**Exit condition:** `split/lib` is only the library/documentation repository.

## Stage 5: prepare branch CI and final workflows

During the staged work, validate branches through pull requests and/or manual
workflow dispatch. Do not alter `master` or release from an extraction branch.

Before final repository pushes:

- `split/sandbox` contains Sandbox CI for `master`/pull requests: formatting,
  `npm ci`, application tests, Scala.js linking, Vite build, demo assembly,
  Docker build, and server smoke tests. It has no Maven publication or docs
  deployment job.
- `split/prism` contains CLI CI for `master`/pull requests: formatting, JVM
  tests, CIP-30 `npm ci`, Scala.js linking, esbuild, assembly, jar-content
  assertion, and CLI smoke tests. Application releases are separate from
  Scala-DID Maven publication.
- `split/lib` contains library/docs CI: formatting, library JVM/Scala.js tests,
  library-required npm work, documentation deployment, and Maven publication.
  It contains no Sandbox, Vite, Docker, CLI, CIP-30, or assembly work.
- Update dependency-submission configuration in every branch for its remaining
  modules.

## Stage 6: final repository cutover

Only after `split/sandbox`, `split/prism`, and `split/lib` pass their exit conditions:

1. Create empty GitHub repositories `scala-did-sandbox` and
   `cardano-prism-cli`, without initializing files.
2. Disable GitHub Actions in both destinations and do not configure library
   publication or documentation-deployment secrets.
3. Push `v0.1.1` to both destinations as the shared historical baseline tag.
   This is a new GitHub tag-push event, and the workflow stored at `M` matches
   `v*`; Actions must remain disabled so it cannot run copied release/docs
   jobs.
4. Push `split/sandbox` to `scala-did-sandbox` as `master` and `split/prism`
   to `cardano-prism-cli` as `master`. These are the final branch names in
   their respective repositories; a local branch rename is unnecessary when
   using explicit push refspecs.
5. Enable Actions in each destination and run the already-rewritten workflow
   from `master` or `workflow_dispatch`.
6. Fast-forward the original repository's frozen `master` to `split/lib` (or
   merge it only if an explicitly approved change made fast-forward
   impossible). The original repository is the final `scala-did` repository.
7. Release cleaned Scala-DID **`v1.1.0`** from original `master`.
8. Publish Sandbox `1.0` and CLI `1.0` application releases through their
   respective repositories, update installation channels, and update all
   cross-repository links.

## Final acceptance criteria

- exactly three repositories exist with the ownership above;
- all three contain `v0.1.1` (`M`) and every earlier commit with identical
  hashes;
- `scala-did` builds, tests, documents, and publishes without Sandbox, CLI,
  CIP-30, Vite, Docker, or assembly work;
- the Sandbox builds, packages, and deploys using only released Scala-DID
  artifacts;
- the CLI builds its embedded CIP-30 bundle and runnable jar using only
  released Scala-DID artifacts;
- deleting either application checkout does not prevent the library from
  building, and deleting the library checkout does not prevent either
  application from building.

## Post-migration: `did-extras`

After the boundaries are stable:

1. Add a published cross-platform `did-extras` module in `scala-did`.
2. Move `OpsRPC` into it.
3. Refactor `HardcodeResolver` into fixture-independent resolver
   infrastructure.
4. Keep example DID documents and mappings in Sandbox `did-example`:

   ```text
   did-example -> did-extras
   ```

   `did-extras` must not depend on Sandbox fixtures.
5. Publish `did-extras` and upgrade the Sandbox independently.
