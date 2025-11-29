FROM azul/zulu-openjdk-alpine:17

# COPY ./target/scala-3.3.7/prism-Indexer.jar /prism-indexer.jar
COPY ./target/scala-3.3.7/cardano-prism.jar /cardano-prism.jar
VOLUME /data


# sbt "cardanoPrismCli/assembly"
# java -jar cardano-prism-cli/target/scala-3.3.7/cardano-prism.jar indexer ../prism-vdr/preprod
# shasum -a 256 cardano-prism-cli/target/scala-3.3.7/cardano-prism.jar

# ENTRYPOINT [ "java", "-Dport=8080", "-XX:+UseContainerSupport", "-jar", "app.jar" ]
# #-Xmx100m
# #-Xss256k
# #-XX:MaxMetaspaceSize=N
# #-XX:+UseContainerSupport


# ## docker

# ```shell
# sbt cardanoPrismCli/assembly
# #docker build --tag fmgp/prism-indexer ./cardano-prism-cli/
# docker buildx build --platform linux/amd64,linux/arm64 --tag fmgp/prism-indexer ./cardano-prism-cli/
# docker buildx build --platform linux/amd64,linux/arm64 --tag fmgp/prism-indexer --push ./cardano-prism-cli/
# # OLD_TAG=docker.io/fmgp/prism-indexer:latest; NEW_TAG=docker.io/fmgp/prism-indexer:20250505; docker buildx imagetools create --tag "$NEW_TAG" "$OLD_TAG"
# docker run --rm -it --entrypoint "sh" --memory="300m" --cpus="1.0" --volume ./mainnet:/data fmgp/prism-indexer -c "java -XX:+UseContainerSupport -jar prism-indexer.jar indexer --token mainnetpfwHjPZPaTmlyJOJ8oIdpVw4v5EwYdQL ./data"
# ```
