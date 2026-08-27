# Library split execution

This file records the Stage 4 work on `split/lib`. The branch began at
`f8f4b435d` (`build: split plan - lib & prism`), the completed Stage 2
library/docs/CLI boundary. It now contains only Scala-DID library and
documentation projects.

## Actions taken

- Removed the PRISM application boundary in full:
  `cardano-prism-cli/` and `cardano-prism-cip30-webapp/`.
- Removed all CLI/CIP-30 build definitions, root aggregation, JVM test aliases,
  resource-generation/esbuild tasks, assembly merge strategies, and now-unused
  BuildInfo, assembly, ZIO CLI, ReactiveMongo runtime, Laminar, Waypoint, and
  uPickle settings.
- Removed the now-unused `sbt-buildinfo` and `sbt-assembly` plugins. The
  remaining cross-project, Scala.js, ScalablyTyped, ScalaPB, documentation, and
  publication plugins are still required by library projects or documentation.
- Removed the in-repository CLI guide, replaced it with the final external CLI
  repository link, and updated the README dependency graph and agent guidance.
- Enabled publication of `did-method-web` by replacing its old
  `publish / skip := true` configuration with the standard library publication
  configuration.
- Removed the unused documentation-resource generator that belonged to the
  former demo assembly.

The root build now aggregates only the published library cross-projects and
`docs`. It contains no application, Vite, Docker, CIP-30, or assembly task.

## Execution-record review

The plan and records are collected in this directory:

| File | Status and consistency assessment |
| --- | --- |
| `SPLIT_PLAN.md` | Original staged migration plan, moved here unchanged. |
| `SPLIT_PLAN_EXECUTION_LIB_PRISM.md` | Completed Stage 2 record. Its former uncommitted-state wording was corrected: the recorded work is committed in `f8f4b435d`. Its Sandbox removal and retained CLI boundary are the correct parent state for this Stage 4 extraction. |
| `SPLIT_PLAN_EXECUTION_SANDBOX.md` | Verbatim record from `2142bb2b957dae212d920962a05731956bb3b221`. Its ownership is complementary to this branch. Its `did:web` fallback, `/doc` redirect shape, `1.0-SNAPSHOT` version, CI `npm install`, Docker smoke test, and clean-cache Maven-only proof remain application/cutover follow-ups. |
| `SPLIT_PLAN_EXECUTION_PRISM.md` | Verbatim record from `9e15e259ca48e20467eb9b1da375f1bd95657eb6`. Its application boundary is complementary to this branch and it records the required CLI bundle/assembly validation. Its statement that the extraction was uncommitted is stale: that SHA is the committed `split/prism` extraction. |

## Shared prerequisites still open

- The `v0.1.1` Stage 0 baseline still needs proof that all required JVM and
  Scala.js artifacts, including `did-method-web`, are available through Maven,
  using dedicated clean Coursier/Ivy caches and with the complete POM closure.
  Enabling `did-method-web` publication here permits the cleaned-library
  release, but cannot retroactively make a missing `0.1.1` artifact available.
- Final cutover remains conditional on the Sandbox and PRISM branches passing
  their recorded exit conditions and on the Sandbox follow-ups listed above.

## Validation completed

- `npm ci --ignore-scripts`
- `SKIP_INTEGRATION_TEST=true sbt -batch 'scalafmtCheckAll;testAllNoDB;docAll;siteAll'`
- `git diff --check`

The SBT command passed library JVM/Scala.js tests and generated both API and
site documentation. ScalablyTyped and Scaladoc emitted existing dependency/link
warnings, but no command failed.

## Exit-condition assessment

The source and build boundary for Stage 4 is satisfied: this checkout has no
Sandbox/demo, CLI, CIP-30, Vite, Docker, or assembly source/build work. The
remaining release-baseline and cross-repository prerequisites above must be
completed before the migration's final cutover.
