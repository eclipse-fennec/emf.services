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
#
# MQTT ships in this image and stays asleep: with DDSR_MQTT_URL empty the
# event transport registers no sink and says so once, so a REST-only
# deployment carries the bundles and notices nothing. Setting the URL is
# the whole difference between the two — there is no second image.
#
# DDSR_AUTH_MODE is bearer, and it has to be said here: the jar is
# exported from the development launch, which says none, and without this
# line the image would inherit that. Bearer needs a token verifier —
# DDSR_JWT_ISSUER, DDSR_JWT_JWKS_URI and DDSR_JWT_AUDIENCES. Left empty,
# the broker has no verifier and, in bearer mode, serves nothing at all:
# it fails closed rather than open. A deployment that really wants an
# open broker says DDSR_AUTH_MODE=none (SECURITY.md §7a). DDSR_DOMAIN
# names the one domain the broker serves; empty is the default domain.
# Mode mtls instead wants HTTPS with client certificates: DDSR_HTTPS_*
# (key and trust store mounted into the container) and DDSR_MTLS_ROLES
# (SECURITY.md §7d). HTTPS stays off unless DDSR_HTTPS_ENABLE says so.
ENV APP_HOME=${APP_HOME} \
    DATA_DIR=${DATA_DIR} \
    TMPDIR=${TMPDIR} \
    BROKER_JAR=${BROKER_JAR} \
    DDSR_HTTP_PORT=8887 \
    DDSR_HTTP_HOST=0.0.0.0 \
    DDSR_PUBLIC_URL=http://localhost:8887/ddsr/rest \
    DDSR_MQTT_URL= \
    DDSR_MQTT_TOPIC_PREFIX=ddsr/events \
    DDSR_MQTT_CLIENT_ID=ddsr-broker \
    DDSR_MQTT_EVENT_SOURCE=/fennec/services/broker \
    DDSR_MQTT_QOS=0 \
    DDSR_DOMAIN= \
    DDSR_AUTH_MODE=bearer \
    DDSR_JWT_ISSUER= \
    DDSR_JWT_JWKS_URI= \
    DDSR_JWT_AUDIENCES=ddsr-broker \
    DDSR_JWT_SCOPE_CLAIM=scope \
    DDSR_JWT_SUBJECT_CLAIM=sub \
    DDSR_HTTP_ENABLE=true \
    DDSR_HTTPS_ENABLE=false \
    DDSR_HTTPS_PORT=8443 \
    DDSR_HTTPS_CLIENT_CERTIFICATE=none

COPY --from=builder --chown=65532:65532 ${APP_HOME} ${APP_HOME}
COPY --from=builder --chown=65532:65532 ${TMPDIR} ${TMPDIR}

# REST + SSE (8443 when DDSR_HTTPS_ENABLE). Readiness is GET /ddsr/rest/catalog answering at all — 200
# in mode none, 401 without a token in mode bearer; 404 means the
# applications are not deployed (bearer without a verifier). There is no HEALTHCHECK
# because the distroless base ships no shell and no curl.
EXPOSE 8887 8443

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
