# Copyright (c) 2026 Data In Motion and others.
# SPDX-License-Identifier: EPL-2.0
#
# Generic runner for an exported bnd executable jar. Build with the
# module's generated/distributions/executable/ directory as context:
#
#   podman build -f itest/containers/Containerfile.java \
#     --build-arg JAR=broker.jar -t ddsr/broker \
#     org.eclipse.fennec.services.broker.rest/generated/distributions/executable/
FROM docker.io/library/eclipse-temurin:21-jre
ARG JAR
COPY ${JAR} /app/app.jar
WORKDIR /work
# --nointeractive: without a TTY the gogo shell would stop the framework.
ENTRYPOINT ["java", "-Dgosh.args=--nointeractive", "-jar", "/app/app.jar"]
