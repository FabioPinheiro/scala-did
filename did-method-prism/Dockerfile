FROM azul/zulu-openjdk-alpine:17

COPY ./jvm/target/scala-3.6.4/prism-Indexer.jar /prism-indexer.jar
VOLUME /data

# ENTRYPOINT [ "java", "-Dport=8080", "-XX:+UseContainerSupport", "-jar", "app.jar" ]
# #-Xmx100m
# #-Xss256k
# #-XX:MaxMetaspaceSize=N
# #-XX:+UseContainerSupport


# ## docker

# ```shell
# sbt didResolverPrismJVM/assembly
# #docker build --tag fmgp/prism-indexer ./did-method-prism/
# docker buildx build --platform linux/amd64,linux/arm64 --tag fmgp/prism-indexer ./did-method-prism/
# docker buildx build --platform linux/amd64,linux/arm64 --tag fmgp/prism-indexer --push ./did-method-prism/
# # OLD_TAG=docker.io/fmgp/prism-indexer:latest; NEW_TAG=docker.io/fmgp/prism-indexer:20250403; docker buildx imagetools create --tag "$NEW_TAG" "$OLD_TAG"
# docker run --rm -it --entrypoint "sh" --memory="300m" --cpus="1.0" --volume ./mainnet:/data fmgp/prism-indexer -c "java -XX:+UseContainerSupport -jar prism-indexer.jar /data mainnet mainnetpfwHjPZPaTmlyJOJ8oIdpVw4v5EwYdQL"
# ```