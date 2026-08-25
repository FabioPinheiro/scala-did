FROM azul/zulu-openjdk-alpine:17

COPY ./target/scala-3.3.8/scala-did-demo-server.jar /app/app.jar
WORKDIR /app

EXPOSE 8080

ENTRYPOINT [ "java", "-Dport=8080", "-XX:+UseContainerSupport", "-jar", "app.jar" ]
