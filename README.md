# Scala-DID Sandbox

The Scala-DID Sandbox is a DIDComm demo application with a Scala.js webapp,
service worker, Vite frontend, and JVM demo server.

It consumes Scala-DID exclusively as Maven artifacts. The temporary baseline is
configured once in [`build.sbt`](build.sbt) as `scalaDidVersion = "0.1.1"`.

## Build

```sh
npm ci
sbt compile test
sbt fullPackAll
npm run build
sbt backend/assembly
docker build -f backend/Dockerfile backend
```

The resulting server jar is
`backend/target/scala-3.3.8/scala-did-demo-server.jar`.

Library documentation is served by [doc.did.fmgp.app](https://doc.did.fmgp.app).
The demo redirects its former `/doc/...` and `/api/...` routes there; `/apis/...`
is deliberately retired.
