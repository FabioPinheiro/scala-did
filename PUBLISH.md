# Publishing Cardano PRISM CLI

`cardanoPrismCli` is the only publishable SBT project in this repository. The
CIP-30 webapp is bundled into its artifact and remains non-published.

1. Set the intended application version and create a matching `v*` tag.
2. Push the tag after the destination repository's GitHub Actions and Sonatype
   publication secrets are configured.
3. The `publish` CI job runs `sbt ci-release`, publishing the CLI artifact with
   its embedded `cip30/bundle.js` and source map.

This is an application release. It is independent of Scala-DID library
publication; the CLI consumes released Scala-DID Maven artifacts.
