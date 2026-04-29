# cardano-prism-cip30-webapp

Scala.js + esbuild webapp that drives the CIP-30 browser-wallet flow for `cardano-prism cardano submit-cip30`.
The Scala source compiles via sbt, and esbuild bundles the linker output into a single minified ES-module `dist/bundle.js`.
The CLI embeds that bundle as a classpath resource at `cip30/bundle.js` and serves it.


## Build (driven by sbt)

The pipeline is wired into `cardanoPrismCip30Webapp/cip30Bundle`:

```sh
sbt cardanoPrismCip30Webapp/cip30Bundle
```

That runs `fullLinkJS` (production-optimized Scala.js), `npm install`, and `node build.js`. The same task is chained into `cardano-prism-cli`'s resource generators, so:

```sh
sbt cardanoPrismCli/Compile/packageBin
```

produces a jar that contains `cip30/bundle.js` ready to serve.

## Manual build

```sh
sbt cardanoPrismCip30Webapp/fullLinkJS
npm install
CIP30_SCALAJS_DIR=target/scala-3.3.7/cardano-prism-cip30-webapp-opt npm run build
```

Output: `dist/bundle.js` (+ `dist/bundle.js.map`).

If `CIP30_SCALAJS_DIR` is unset, `build.js` falls back to `target/scala-3.3.7/cardano-prism-cip30-webapp-opt/` — adjust if the Scala version in `build.sbt` changes.

## Inspect the jar

The `cip30Bundle` task is wired into `cardanoPrismCli`'s resource generators, so the bundle ends up at `cip30/bundle.js` inside any jar built from that project.
Two ways to package the CLI:

```sh
# Library jar — only this project's classes/resources, needs deps on the
# classpath at runtime. Useful for publishing to a Maven repo.
sbt cardanoPrismCli/Compile/packageBin
# -> cardano-prism-cli/target/scala-3.3.7/cardano-prism-cli_3-0.1.0-SNAPSHOT.jar

# Fat jar — all dependencies merged into one runnable jar. Optional; only needed if you want a single-file distribution.
sbt cardanoPrismCli/assembly
# Run: java -jar cardano-prism-cli/target/scala-3.3.7/cardano-prism.jar cardano submit-cip30 0a066d617374657212473045022100b32b3dfc1fb47dc102038c1cbc1571b955f0ee7bab27e8b9626f8da62c50a4d6022050dfa98afdfe7503dbe58ed9ae20addb6d52a182521cd67e9d4bb6b79629b0f41a400a3e0a3c123a0a066d617374657210014a2e0a09736563703235366b31122103ebe0934672da51ca01da94d278376a204e0e73a8d235c290bc2d5f1a629f8aec
```

To list the contents of either jar (sanity-check that `cip30/bundle.js` is
actually packaged):

```sh
jar tf cardano-prism-cli/target/scala-3.3.7/cardano-prism-cli_3-0.1.0-SNAPSHOT.jar | grep cip30
# cip30/bundle.js
# cip30/bundle.js.map
```
