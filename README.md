# Cardano PRISM CLI

`cardano-prism` manages [PRISM DIDs](https://github.com/input-output-hk/prism-did-method-spec)
on Cardano. It includes a CIP-30 browser-wallet webapp, bundled into the
runnable CLI jar.

This is an application repository. It consumes released Scala-DID artifacts
from Maven Central (`0.1.1` initially); it does not build against a local
Scala-DID checkout or `publishLocal` artifacts.

## Build

Requirements: JDK 17, sbt, Node.js, and npm.

```sh
npm --prefix cardano-prism-cip30-webapp ci
sbt cardanoPrismCli/assembly
jar tf cardano-prism-cli/target/scala-3.3.8/cardano-prism.jar | grep 'cip30/bundle.js'
java -jar cardano-prism-cli/target/scala-3.3.8/cardano-prism.jar --help
```

The assembly task performs the production Scala.js link, runs the CIP-30
esbuild pipeline, and embeds `cip30/bundle.js` and its source map in the jar.

`cardanoPrismCli` is published on application-release tags; the CIP-30 webapp
is bundled into that artifact and is not published independently.

## Documentation

The CLI guide is in [cardano-prism-cli/README.md](cardano-prism-cli/README.md).
Library documentation is maintained at <https://doc.did.fmgp.app/>.
