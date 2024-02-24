# DEMO

## run

**Run Server:**

```shell
# Build Frontend
sbt 'serviceworker/fullLinkJS';
npm run build
```

```shell
sbt '~ demoJVM/reStart'
```

Open [chrome://inspect/#devices](chrome://inspect/#devices)

**Run Frontend:**

```shell
# Live reload
sbt '~serviceworker/fastLinkJS';
sbt '~webapp/fastLinkJS' # run on another console
npm run dev
```

## docker

```shell
sbt assemblyAll
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
sbt assemblyAll
flyctl deploy ./demo/
```

**deploy by pushing docker image**

```shell
sbt assemblyAll
docker build --tag scala_did_demo ./demo/
docker tag scala_did_demo registry.fly.io/scala-did-demo
# flyctl auth docker
docker push registry.fly.io/scala-did-demo
# +- 52MB
flyctl image update -a scala-did-demo
flyctl deploy ./demo/ -i registry.fly.io/scala-did-demo
```

## History of deployments

Size of the last docker layer:
- 2024/02/18 +- 121.2MB
- 2024/02/13 +- 120.8MB
- 2024/02/05 +- 119.3MB
- 2023/11/15 +- 124.1MB
- 2023/10/28 +- 118.1MB
- 2023/10/20 +- 117.3MB
- 2023/09/24 +- 115.1MB

## Others

Sort by file size
`jar tvf demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar | sort -k1nr | less`

Show assets
`jar tvf demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar | sort -k1nr | grep "assets/"`

Show jar size
`du -h demo/jvm/target/scala-3.3.1/scala-did-demo-server.jar`