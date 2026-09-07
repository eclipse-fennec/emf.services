# Copyright (c) 2026 Contributors to the Eclipse Foundation.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# The deployable Fennec Services broker: the bnd executable jar exported
# from broker.bndrun (see docs/GETTING_STARTED.md §2) on a distroless
# JRE. The jar is NOT built here — it is staged into content/ by the
# :docker:broker:prepareDocker task (locally) or by the container
# workflow (CI), so the image embeds exactly the jar that was built and
# published alongside it.

ARG APP_HOME=/opt/services
ARG DATA_DIR=${APP_HOME}/data
ARG TMPDIR=/tmp/services
ARG BROKER_JAR=${APP_HOME}/broker.jar

FROM eclipse-temurin:21-jre-alpine AS builder

ARG APP_HOME
ARG DATA_DIR
ARG TMPDIR
ARG BROKER_JAR

# 65532 = the `nonroot` user of the distroless base. The data dir has to
# be writable by it: the broker persists its registry synchronously
# after every mutation, and the bnd launcher unpacks the embedded
# bundles into TMPDIR.
RUN set -eux; \
    mkdir -p "${APP_HOME}" "${DATA_DIR}" "${TMPDIR}"; \
    chown -R 65532:65532 "${APP_HOME}" "${TMPDIR}"; \
    chmod -R u+rwX,go-rwx "${DATA_DIR}" "${TMPDIR}"

COPY content/broker.jar "${BROKER_JAR}"

RUN chown -R 65532:65532 "${APP_HOME}"

FROM gcr.io/distroless/java21-debian12:nonroot

ARG APP_HOME
ARG DATA_DIR
ARG TMPDIR
ARG BROKER_JAR

# Every value below is read by the broker's configurator JSON through the
# configadmin interpolation plugin, so `docker run -e ...` is the whole
# configuration surface. DDSR_PUBLIC_URL is the one that MUST be set for
# any deployment other than --network=host: it is the address the broker
# advertises for itself in the catalog, so consumers in other containers
# or on other hosts dial exactly this URL.
ENV APP_HOME=${APP_HOME} \
    DATA_DIR=${DATA_DIR} \
    TMPDIR=${TMPDIR} \
    BROKER_JAR=${BROKER_JAR} \
    DDSR_HTTP_PORT=8887 \
    DDSR_HTTP_HOST=0.0.0.0 \
    DDSR_PUBLIC_URL=http://localhost:8887/ddsr/rest

COPY --from=builder --chown=65532:65532 ${APP_HOME} ${APP_HOME}
COPY --from=builder --chown=65532:65532 ${TMPDIR} ${TMPDIR}

# REST + SSE. Readiness is GET /ddsr/rest/catalog — there is no HEALTHCHECK
# because the distroless base ships no shell and no curl.
EXPOSE 8887

# The broker persists its registry as ./broker-state.xmi relative to the
# working directory, so the working directory IS the data volume. This is
# deliberately not a configuration: a configuration for the broker core PID
# would re-activate the component after the catalog has already been seeded,
# and the broker would lose its own self-publication.
VOLUME ["/opt/services/data"]
WORKDIR ${DATA_DIR}

# --nointeractive: without a TTY the gogo shell would stop the framework.
ENTRYPOINT ["java", \
    "-Dgosh.args=--nointeractive", \
    "-Djava.io.tmpdir=/tmp/services", \
    "-jar", "/opt/services/broker.jar"]
