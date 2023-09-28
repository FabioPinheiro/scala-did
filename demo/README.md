# DEMO

## run

**Run Server:**

```sbt
NODE_OPTIONS=--openssl-legacy-provider sbt
serviceworker / fastLinkJS
~ demoJVM/reStart
```

**Run Frontend:**

```shell
npm run build
npm run preview

# OR with live reload

NODE_OPTIONS=--openssl-legacy-provider sbt '~webapp/fastLinkJS' # run on another console
npm run dev
```

## docker

```shell
NODE_OPTIONS=--openssl-legacy-provider sbt assemblyAll
# java -jar jvm/target/scala-3.3.1/scala-did-demo-server.jar
docker build --tag scala_did_demo .
docker run --rm -p 8080:8080 --memory="100m" --cpus="1.0" scala_did_demo
```

```
jar tf /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar | less
jar tvf /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar | sort -rnk 1 | less
java -jar /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar
```

## FLY.IO

- `flyctl auth login`
- `flyctl open /demo`
- `flyctl status -a scala-did-demo`
- `flyctl image show -a scala-did-demo`
- `flyctl logs -c demo/fly.toml`

**deploy with flyctl**

```shell
NODE_OPTIONS=--openssl-legacy-provider sbt assemblyAll
flyctl deploy ./demo/
```

**deploy by pushing docker image**

```shell
NODE_OPTIONS=--openssl-legacy-provider sbt assemblyAll
docker build --tag scala_did_demo ./demo/
docker tag scala_did_demo registry.fly.io/scala-did-demo
# flyctl auth docker
docker push registry.fly.io/scala-did-demo # +- 115.1MB (before was +- 52MB)

flyctl image update -a scala-did-demo
flyctl deploy -i registry.fly.io/scala-did-demo -a scala-did-demo
```

## Others

Sort by file size
`jar tvf demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar | sort -k1nr | less`

Show assets
`jar tvf demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar | sort -k1nr | grep "assets/"`

Show jar size
`du -h demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar`