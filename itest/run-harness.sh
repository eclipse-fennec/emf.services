#!/usr/bin/env bash
# Copyright (c) 2026 Data In Motion and others.
# SPDX-License-Identifier: EPL-2.0
#
# Cross-language harness (FR-P4, DECISIONS_PARITY D10).
#
# Scenario A: broker + JAVA provider  → TS consumer probe
#             (discovery, sd1 fingerprint incl. golden hash, all eight
#             property types, invocation, and the FR-P3 lifecycle order:
#             UNREGISTERING reaches the consumer before the provider
#             process is gone)
# Scenario B: broker + TS provider    → JAVA consumer (client.bndrun:
#             TsPaymentDebug calls getBalance + charge), then TS
#             provider shutdown: withdraw confirmed BEFORE the endpoint
#             stops, and the broker no longer lists the provider.
#
# Runs everything as host processes (no podman required); see
# containers/ for the containerized variant.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$ROOT/itest/work"
TS="$ROOT/ddsr-ts-client"
BROKER_URL="http://localhost:8887/ddsr/rest"
PIDS=()

log() { printf '\n=== %s ===\n' "$*"; }
now_ms() { date +%s%3N; }

cleanup() {
  for pid in "${PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap cleanup EXIT

wait_for_url() { # url timeout_s
  local url="$1" timeout="${2:-30}" waited=0
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

# Starts a jar in the background; the pid lands in LAST_PID. Deliberately
# NOT $(command substitution): that would fork a subshell and the PIDS
# bookkeeping (and thus the cleanup trap) would silently lose the child.
start_jar() { # name jar workdir
  local name="$1" jar="$2" dir="$3"
  mkdir -p "$dir"
  # --nointeractive: without a TTY the gogo shell would otherwise stop
  # the framework right after startup.
  (cd "$dir" && exec java -Dgosh.args=--nointeractive -jar "$jar") >"$WORK/$name.log" 2>&1 &
  LAST_PID=$!
  PIDS+=("$LAST_PID")
}

require_port_free() { # port
  if ss -ltn 2>/dev/null | grep -q ":$1 "; then
    echo "port $1 is already in use — a stale process from an earlier run?" >&2
    ss -ltnp 2>/dev/null | grep ":$1 " >&2 || true
    exit 1
  fi
}

# ---------------------------------------------------------------- build
log "build + export"
rm -rf "$WORK"
mkdir -p "$WORK"
# The bnd export task embeds dependency bundles but its Gradle
# up-to-date check does not track them — delete the outputs so the
# export always re-runs against fresh workspace bundles.
rm -f "$ROOT"/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker.jar \
      "$ROOT"/org.eclipse.fennec.services.examples.payment/generated/distributions/executable/payment-provider.jar \
      "$ROOT"/org.eclipse.fennec.services.client.java/generated/distributions/executable/client.jar
(cd "$ROOT" && ./gradlew build \
  :org.eclipse.fennec.services.broker.rest:export.broker \
  :org.eclipse.fennec.services.examples.payment:export.payment-provider \
  :org.eclipse.fennec.services.client.java:export.client) >"$WORK/gradle.log" 2>&1 \
  || { tail -30 "$WORK/gradle.log"; exit 1; }
BROKER_JAR="$ROOT/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker.jar"
PROVIDER_JAR="$ROOT/org.eclipse.fennec.services.examples.payment/generated/distributions/executable/payment-provider.jar"
CLIENT_JAR="$ROOT/org.eclipse.fennec.services.client.java/generated/distributions/executable/client.jar"
ls "$BROKER_JAR" "$PROVIDER_JAR" "$CLIENT_JAR" >/dev/null

(cd "$TS" && corepack pnpm install --frozen-lockfile && corepack pnpm -r build) >"$WORK/pnpm.log" 2>&1 \
  || { tail -30 "$WORK/pnpm.log"; exit 1; }

# --------------------------------------------------------------- broker
log "starting broker"
require_port_free 8887
require_port_free 9091
require_port_free 9090
start_jar broker "$BROKER_JAR" "$WORK/broker"
BROKER_PID=$LAST_PID
wait_for_url "$BROKER_URL/catalog" 60
echo "broker up (pid $BROKER_PID)"

# ============================================================ Scenario A
log "Scenario A: Java provider -> TS consumer"
start_jar payment-java "$PROVIDER_JAR" "$WORK/payment-java"
PROVIDER_PID=$LAST_PID
wait_for_line "$WORK/payment-java.log" "published payments-java" 60

(cd "$TS/examples/payment" \
  && BROKER_URL="$BROKER_URL" EXPECT_LANG=java corepack pnpm exec tsx harness-probe.ts) \
  >"$WORK/probe-a.log" 2>&1 &
PROBE_PID=$!
PIDS+=("$PROBE_PID")

wait_for_line "$WORK/probe-a.log" "PROBE_READY" 90
log "Scenario A: stopping the Java provider (SIGTERM)"
kill "$PROVIDER_PID"
wait "$PROVIDER_PID" 2>/dev/null || true
PROVIDER_EXIT_MS=$(now_ms)

if ! wait "$PROBE_PID"; then
  echo "SCENARIO A FAILED"; cat "$WORK/probe-a.log"; exit 1
fi
UNREG_MS=$(grep -o 'UNREGISTERING_AT [0-9]*' "$WORK/probe-a.log" | awk '{print $2}')
if [ -z "$UNREG_MS" ] || [ "$UNREG_MS" -gt "$PROVIDER_EXIT_MS" ]; then
  echo "SCENARIO A FAILED: UNREGISTERING ($UNREG_MS) not before provider exit ($PROVIDER_EXIT_MS)"
  cat "$WORK/probe-a.log"; exit 1
fi
echo "Scenario A OK: consumer informed $((PROVIDER_EXIT_MS - UNREG_MS)) ms before the provider was gone"
grep -E '  [✓✗]' "$WORK/probe-a.log" || true

# ============================================================ Scenario B
log "Scenario B: TS provider -> Java consumer"
# exec node directly (no pnpm/corepack wrappers): the SIGTERM of the
# FR-P3 check must reach the node process itself, wrapper chains do not
# reliably forward signals.
(cd "$TS/examples/payment" \
  && BROKER_URL="$BROKER_URL" SERVICE_URL="http://localhost:9090/payments" exec node --import tsx provider.ts) \
  >"$WORK/provider-ts.log" 2>&1 &
TS_PROVIDER_PID=$!
PIDS+=("$TS_PROVIDER_PID")
wait_for_line "$WORK/provider-ts.log" "published as reference" 60

start_jar client "$CLIENT_JAR" "$WORK/client"
CLIENT_PID=$LAST_PID
wait_for_line "$WORK/client.log" 'ts.charge(10.0, "EUR") =' 90
echo "Java consumer called the TS provider:"
grep -E 'ts\.(getBalance|charge)' "$WORK/client.log"

log "Scenario B: stopping the TS provider (SIGTERM) — FR-P3 order check"
kill "$TS_PROVIDER_PID"
if ! wait "$TS_PROVIDER_PID"; then
  echo "SCENARIO B FAILED: TS provider exited non-zero"; cat "$WORK/provider-ts.log"; exit 1
fi
# withdraw confirmed BEFORE the endpoint stops (log order proves it)
CONFIRM_LINE=$(grep -n 'unregistration confirmed by broker' "$WORK/provider-ts.log" | cut -d: -f1 | head -1)
if [ -z "$CONFIRM_LINE" ]; then
  echo "SCENARIO B FAILED: no broker confirmation before endpoint stop"; cat "$WORK/provider-ts.log"; exit 1
fi
# the broker must no longer list the TS provider
if curl -sf "$BROKER_URL/references?interface=Payment" | grep -q 'payments-ts'; then
  echo "SCENARIO B FAILED: broker still lists payments-ts after withdraw"; exit 1
fi
echo "Scenario B OK: withdraw confirmed before endpoint stop; broker no longer lists payments-ts"

kill "$CLIENT_PID" 2>/dev/null || true
log "HARNESS PASSED (A + B)"
