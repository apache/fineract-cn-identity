FROM openjdk:8-jdk-alpine

ARG identity_port=2021

ENV server.max-http-header-size=16384 \
    cassandra.clusterName="Test Cluster" \
    server.port=$identity_port

WORKDIR /tmp
COPY identity-service-boot-0.1.0-BUILD-SNAPSHOT.jar .

CMD ["java", "-jar", "identity-service-boot-0.1.0-BUILD-SNAPSHOT.jar"]
