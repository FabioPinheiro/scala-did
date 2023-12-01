# Setup environment

Setup an environment for quick start

The Library is published to sonatype maven:

## scala-cli on docker

Start an isolated environment to experiment with some code samples (3/5 mins)

`docker run --rm  -it --entrypoint /bin/sh virtuslab/scala-cli`

```shell
scala-cli repl \
  --dependency app.fmgp::did::@VERSION@ \
  --dependency app.fmgp::did-imp::@VERSION@ \
  --dependency app.fmgp::did-method-peer::@VERSION@ \
  --repo https://oss.sonatype.org/content/repositories/releases

# For snapshots use
# --repo https://oss.sonatype.org/content/repositories/snapshots
# For releases use
# --repo https://oss.sonatype.org/content/repositories/releases
```

## SBT setup

To install the library on `sbt`, you can use the following lines to your `build.sbt`:

```scala sbt
 libraryDependencies += "app.fmgp" %% "did" % @VERSION@
 libraryDependencies += "app.fmgp" %% "did-imp" % @VERSION@ // for the DIDComm implementation
 libraryDependencies += "app.fmgp" %% "did-resolver-peer" % @VERSION@ // for hash utils
```

In a crossProject for the JSPlatform and JVMPlatform this shoud use this instead:

```scala sbt
 libraryDependencies += "app.fmgp" %%% "did" % @VERSION@
 libraryDependencies += "app.fmgp" %%% "did-imp" % @VERSION@ // for the DIDComm implementation
 libraryDependencies += "app.fmgp" %%% "did-resolver-peer" % @VERSION@ // for hash utils
```

## Coursier Download

```shell
coursier fetch app.fmgp:did_3:@VERSION@ -r sonatype:snapshots
# -r https://oss.sonatype.org/content/repositories/snapshots

coursier fetch app.fmgp:did_3:@VERSION@ -r sonatype:public
# -r https://oss.sonatype.org/content/repositories/releases
```
