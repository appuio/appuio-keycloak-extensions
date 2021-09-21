FROM docker.io/library/maven:3-eclipse-temurin-16 AS build

ENV MAVEN_SUPPRESS_DOWNLOADS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"

COPY pom.xml .

# Download the packages
RUN \
    mvn ${MAVEN_SUPPRESS_DOWNLOADS} -B dependency:go-offline

COPY src ./src

RUN \
    mvn ${MAVEN_SUPPRESS_DOWNLOADS} -B package

# Runtime image - technically there's nothing to run, it just contains the JAR
FROM docker.io/library/busybox AS runtime

COPY --from=build ./target/*.jar /extensions/

RUN \
  chmod -R +r /extensions

USER 1001:0
