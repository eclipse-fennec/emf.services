#!/usr/bin/env bash
# Copyright (c) 2026 Data In Motion and others.
# SPDX-License-Identifier: EPL-2.0
#
# The whole demo in one command: the observability stack, a broker, a
# payment provider and a consumer that keeps calling it — all three
# reporting traces, metrics and logs to the collector.
#
#   demo/observability/run-demo.sh
#
# Then open http://localhost:3000 and pick the "Fennec Services"
# dashboard. Ctrl-C stops the three frameworks; the stack keeps running
# so the last minutes stay readable, and stop.sh takes it down.
#
# Java 21: newer JVMs break the SPI Fly weaving these launches need.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
TS="$ROOT/ddsr-ts-client"
WORK="$DIR/work"
BROKER_URL="${BROKER_URL:-http://localhost:8887/ddsr/rest}"
OTLP="${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:4318}"
PIDS=()

log() { printf '\n=== %s ===\n' "$*"; }

cleanup() {
  for pid in "${PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
  echo
  echo "the frameworks are stopped; the stack is still up at http://localhost:3000"
  echo "take it down with demo/observability/stop.sh"
}
trap cleanup EXIT

wait_for_url() { # url timeout_s
  local url="$1" timeout="${2:-60}" waited=0
  until curl -sf -o /dev/null "$url"; do
    sleep 0.5
    waited=$((waited + 1))
    if [ "$waited" -ge $((timeout * 2)) ]; then
      echo "TIMEOUT waiting for $url" >&2
      return 1
    fi
  done
}

wait_for_line() { # file pattern timeout_s
  local file="$1" pattern="$2" timeout="${3:-60}" waited=0
  until grep -q "$pattern" "$file" 2>/dev/null; do
    sleep 0.5
    waited=$((waited + 1))
    if [ "$waited" -ge $((timeout * 2)) ]; then
      echo "TIMEOUT waiting for '$pattern' in $file" >&2
      tail -20 "$file" >&2 || true
      return 1
    fi
  done
}

# Starts a launch in the background. --nointeractive: without a TTY the
# gogo shell would stop the framework right after startup.
start_jar() { # name jar service-name
  local name="$1" jar="$2" service="$3"
  mkdir -p "$WORK/$name"
  (cd "$WORK/$name" \
    && OTEL_SERVICE_NAME="$service" OTEL_EXPORTER_OTLP_ENDPOINT="$OTLP" \
       exec java -Dgosh.args=--nointeractive -jar "$jar") >"$WORK/$name.log" 2>&1 &
  LAST_PID=$!
  PIDS+=("$LAST_PID")
}

# ------------------------------------------------------------ the stack
log "observability stack"
"$DIR/deploy.sh"
wait_for_url "http://localhost:3000/api/health" 120
# The collector answers 404 on /, which a `curl -f` calls a failure —
# what matters is that something is listening at all.
until curl -s -o /dev/null "http://localhost:4318/"; do sleep 1; done

# ------------------------------------------------------------- the build
log "build + export"
rm -rf "$WORK"
mkdir -p "$WORK"
# The bnd export embeds its bundles but its up-to-date check does not
# track them — delete the outputs so an export always re-runs against
# fresh workspace bundles.
rm -f "$ROOT"/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker-otel.jar \
      "$ROOT"/org.eclipse.fennec.services.examples.payment/generated/distributions/executable/payment-provider-otel.jar \
      "$ROOT"/org.eclipse.fennec.services.client.java/generated/distributions/executable/client-otel.jar
(cd "$ROOT" && ./gradlew build \
  :org.eclipse.fennec.services.broker.rest:export.broker-otel \
  :org.eclipse.fennec.services.examples.payment:export.payment-provider-otel \
  :org.eclipse.fennec.services.client.java:export.client-otel) >"$WORK/gradle.log" 2>&1 \
  || { tail -30 "$WORK/gradle.log"; exit 1; }

BROKER_JAR="$ROOT/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker-otel.jar"
PROVIDER_JAR="$ROOT/org.eclipse.fennec.services.examples.payment/generated/distributions/executable/payment-provider-otel.jar"
CLIENT_JAR="$ROOT/org.eclipse.fennec.services.client.java/generated/distributions/executable/client-otel.jar"

# --------------------------------------------------------------- broker
log "broker"
start_jar broker "$BROKER_JAR" fennec-broker
wait_for_url "$BROKER_URL/catalog" 90
echo "broker up — $BROKER_URL"

# ------------------------------------------------------------- provider
log "payment provider"
# BindingProbe as well as Payment: it is served by the generic REST
# distribution, which continues the caller's trace, so a call to it
# shows all three parties in one trace. Payment's own endpoint is a
# hand-written JAX-RS resource — traced on the calling side only.
export PAYMENTS_PUBLISH_BINDING_PROBE=true
start_jar payment-provider "$PROVIDER_JAR" fennec-payment-provider
unset PAYMENTS_PUBLISH_BINDING_PROBE
wait_for_line "$WORK/payment-provider.log" "published payments-java" 90
echo "provider up — http://localhost:9091/payments"

# ------------------------------------------------------------- consumer
log "consumer (Java)"
start_jar consumer "$CLIENT_JAR" fennec-consumer
wait_for_line "$WORK/consumer.log" "calling Payment every" 90
echo "consumer up — calling Payment on a timer"

# -------------------------------------------------- consumer (TypeScript)
# The cross-language half (#146): the same three stages from a Node
# process, reporting to the same collector. A trace that starts here
# and ends in the Java provider is the claim this project makes, in
# one picture.
log "consumer (TypeScript)"
(cd "$TS" && corepack pnpm install --frozen-lockfile && corepack pnpm -r build) >"$WORK/pnpm.log" 2>&1 \
  || { tail -30 "$WORK/pnpm.log"; exit 1; }
mkdir -p "$WORK/ts-consumer"
(cd "$TS/examples/payment" \
  && OTEL_SERVICE_NAME=fennec-ts-consumer OTEL_EXPORTER_OTLP_ENDPOINT="$OTLP" \
     BROKER_URL="$BROKER_URL" exec node --import tsx demo-traffic.ts) >"$WORK/ts-consumer.log" 2>&1 &
PIDS+=("$!")
wait_for_line "$WORK/ts-consumer.log" "calling Payment every" 120
echo "TypeScript consumer up — same broker, same provider, same trace"

cat <<ENDPOINTS

=== the demo is running ===
  Grafana        http://localhost:3000   → dashboard "Fennec Services"
  Broker REST    $BROKER_URL
  Provider       http://localhost:9091/payments

  logs           $WORK/{broker,payment-provider,consumer,ts-consumer}.log

  What to show: the traces panel has one trace per call — the consumer's
  span, the broker lookup it needed and the provider that answered, in
  one tree. Two of those consumers are different languages: filter the
  traces by service fennec-ts-consumer and the same tree appears with a
  Node process at its root. The gauges beside it come from the runtime
  services, and the lease count is the number no consumer of the wire
  can work out.

  Ctrl-C stops the frameworks.
ENDPOINTS

# Keep the frameworks in the foreground until Ctrl-C.
wait
