# DEMO

## run

```sbt
~ demoJVM/reStart
```

### test

```shell
curl localhost:8080/demo
```

## docker
```shell
NODE_OPTIONS=--openssl-legacy-provider sbt assemblyAll
java -jar jvm/target/scala-3.3.0/scala-did-demo-server.jar
docker build --tag scala_did_demo .
docker run --rm -p 8080:8080 --memory="100m" --cpus="1.0" scala_did_demo
```

```
jar tf /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.3.0/scala-did-demo-server.jar | less
jar tvf /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.3.0/scala-did-demo-server.jar | sort -rnk 1 | less
java -jar /home/fabio/workspace/ScalaDID/demo/jvm/target/scala-3.3.0/scala-did-demo-server.jar
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

**[WIP] deploy by pushing docker image**

```shell
NODE_OPTIONS=--openssl-legacy-provider sbt assemblyAll
docker build --tag scala_did_demo ./demo/
docker tag scala_did_demo registry.fly.io/scala-did-demo
# flyctl auth docker
docker push registry.fly.io/scala-did-demo # +- 52MB

flyctl image update -a scala-did-demo
# FIXME: Error image is not eligible for automated image updates
```

## Others

jar tvf demo/jvm/target/scala-3.3.0/scala-did-demo-server.jar | sort -k1nr | less