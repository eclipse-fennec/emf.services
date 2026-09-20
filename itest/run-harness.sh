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

# A raw tap on the event stream, opened before anything publishes: no
# SDK, no model, just what a curl sees. The envelope is a cross-language
# contract (#101), and the honest proof that it is on the wire is one
# that reads the wire.
curl -sN --max-time 300 -H 'Accept: text/event-stream' "$BROKER_URL/events" \
  >"$WORK/sse-tap.log" 2>&1 &
SSE_TAP_PID=$!
PIDS+=("$SSE_TAP_PID")

# ============================================================ Scenario A
log "Scenario A: Java provider -> TS consumer"
# Only this scenario's provider also announces BindingProbe: its one
# operation carries three arguments in three different places, and the
# probe checks that the consumer put them where the flavor says (#74).
# Nothing in the provider bundle publishes it — the variable reaches a
# factory configuration of the generic distribution, which serves the
# contract AND announces it from the same document (#84). A provider
# that is a configuration and a service, with no code of its own.
export PAYMENTS_PUBLISH_BINDING_PROBE=true
# And PersonStore, whose argument and result are MODELS and whose
# contract declares protobuf (#100). Same mechanism as the probe: a
# factory configuration of the generic distribution serves it and
# announces it from the same document, with no endpoint code anywhere.
export PAYMENTS_PUBLISH_PERSON_STORE=true
start_jar payment-java "$PROVIDER_JAR" "$WORK/payment-java"
unset PAYMENTS_PUBLISH_BINDING_PROBE
unset PAYMENTS_PUBLISH_PERSON_STORE
PROVIDER_PID=$LAST_PID
wait_for_line "$WORK/payment-java.log" "published payments-java" 60

# --------------------------------------------------- the frame on the wire
wait_for_line "$WORK/sse-tap.log" "specversion" 60
SSE_FRAME=$(grep -m1 '^data: ' "$WORK/sse-tap.log" | sed 's/^data: //')
for expected in '"specversion":"1.0"' \
                '"type":"org.eclipse.fennec.services.registered"' \
                '"datacontenttype":"application/xml"' \
                '"data":'; do
  case "$SSE_FRAME" in
    *"$expected"*) ;;
    *) echo "SCENARIO A FAILED: the SSE frame carries no $expected"; echo "$SSE_FRAME"; exit 1 ;;
  esac
done
grep -q "^event: ddsr-service-event" "$WORK/sse-tap.log" \
  || { echo "SCENARIO A FAILED: the frozen SSE event name is gone"; exit 1; }
echo "  ✓ sse-frame-is-a-cloud-event: $(echo "$SSE_FRAME" | cut -c1-96)…"

(cd "$TS/examples/payment" \
  && BROKER_URL="$BROKER_URL" EXPECT_LANG=java EXPECT_BINDINGS=1 corepack pnpm exec tsx harness-probe.ts) \
  >"$WORK/probe-a.log" 2>&1 &
PROBE_PID=$!
PIDS+=("$PROBE_PID")

wait_for_line "$WORK/probe-a.log" "PROBE_READY" 90

# --------------------------------------------------- Scenario P (protobuf)
log "Scenario P: Java provider -> Java consumer, body encoded as protobuf"
# The one call in this harness whose payload is neither XMI nor
# hand-rolled JSON. PersonStore takes a Person and returns a Person, and
# its contract says consumes/produces = application/x-protobuf (#100);
# nothing in the provider or the consumer mentions an encoding.
#
# Java on both ends on purpose: the TypeScript track has no
# protobuf-to-EMF binding, so this contract is one a TS consumer cannot
# read. That is why it runs here, while the Java provider from scenario
# A is still up, and not as a cross-language scenario.
start_jar client-protobuf "$CLIENT_JAR" "$WORK/client-protobuf"
PROTOBUF_CLIENT_PID=$LAST_PID
wait_for_line "$WORK/client-protobuf.log" "travelled as protobuf" 90
grep -E "\[DDSR-Protobuf\]" "$WORK/client-protobuf.log" | tail -2
kill "$PROTOBUF_CLIENT_PID" 2>/dev/null || true
wait "$PROTOBUF_CLIENT_PID" 2>/dev/null || true
echo "  ✓ protobuf-invocation: a modelled argument and result travelled in the declared encoding"

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
UNREG_REASON=$(grep -o 'UNREGISTERING_REASON [A-Z_-]*' "$WORK/probe-a.log" | awk '{print $2}')
if [ "$UNREG_REASON" != "WITHDRAWN" ]; then
  echo "SCENARIO A FAILED: UNREGISTERING reason is '$UNREG_REASON', expected WITHDRAWN (provider shutdown = withdraw)"
  cat "$WORK/probe-a.log"; exit 1
fi
if grep -q "withdraw failed" "$WORK/payment-java.log"; then
  echo "SCENARIO A FAILED: the Java provider's withdraw at deactivate failed (#50) — the consumer was only informed by the late shutdown path"
  grep -A2 "withdraw failed" "$WORK/payment-java.log" | head -6; exit 1
fi
echo "Scenario A OK: consumer informed $((PROVIDER_EXIT_MS - UNREG_MS)) ms before the provider was gone (reason $UNREG_REASON)"
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
# That call is the mixed-binding proof in the other direction (#82): the
# TS provider serves charge as /charge/{amount} with the currency in
# X-Currency and refuses anything else, so a Java consumer only reaches
# it by reading the bindings the TS side published. The broker has to
# carry them through publish, snapshot and lookup for that to work —
# assert them on the wire too, so a regression names itself instead of
# showing up as a failed call.
TS_LOOKUP=$(curl -sf "$BROKER_URL/references?interface=Payment")
if ! grep -q 'binding="PATH"' <<<"$TS_LOOKUP" || ! grep -q 'parameter="' <<<"$TS_LOOKUP"; then
  echo "SCENARIO B FAILED: the broker does not re-serve the TS provider's parameter bindings"
  echo "$TS_LOOKUP"
  exit 1
fi
echo "  ✓ ts-bindings-survive-the-broker: PATH + parameter references present in the lookup"
# Cross-language origin (#132): the broker records where a registration
# came from, and until the TypeScript client sent X-DDSR-Origin every TS
# publish was written down as "anonymous". Read it back off the wire —
# a cross-language claim is only worth what the other language proves.
TS_ORIGIN=$(grep -o 'name="ddsr.origin" value="[^"]*"' <<<"$TS_LOOKUP" | head -1)
if ! grep -q 'payments-ts-harness/' <<<"$TS_ORIGIN"; then
  echo "SCENARIO B FAILED: the TS provider's registration carries no origin of its own"
  echo "  got: ${TS_ORIGIN:-<nothing>}"
  exit 1
fi
echo "  ✓ ts-origin-reaches-the-broker: $TS_ORIGIN"

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
# ============================================================ Scenario F
# A second instance of the SAME (name, version) on another port while a TS
# consumer holds a tracked locator and a lease (#58, #57, #55): the SDK's
# reconnect check sees the same contract with a drifted endpoint and MODIFIES
# the registration in place — the consumer gets MODIFIED under the same
# reference id, refreshes its endpoint and keeps its lease. No churn.
log "Scenario F: same identity restarts on a new port -> MODIFIED in place, consumer follows"
require_port_free 9091
require_port_free 9092
start_jar payment-f1 "$PROVIDER_JAR" "$WORK/payment-f1"
F1_PID=$LAST_PID
wait_for_line "$WORK/payment-f1.log" "published payments-java" 60
(cd "$TS/examples/payment" \
  && BROKER_URL="$BROKER_URL" SCENARIO=F NEW_PORT=9092 exec node --import tsx harness-probe-lifecycle.ts) \
  >"$WORK/probe-f.log" 2>&1 &
PROBE_F_PID=$!
PIDS+=("$PROBE_F_PID")
wait_for_line "$WORK/probe-f.log" "PROBE_READY" 90
PAYMENTS_HTTP_PORT=9092 PAYMENTS_PUBLIC_URL="http://localhost:9092/payments" \
  start_jar payment-f2 "$PROVIDER_JAR" "$WORK/payment-f2"
F2_PID=$LAST_PID
wait_for_line "$WORK/payment-f2.log" "published payments-java" 60
if ! wait "$PROBE_F_PID"; then
  echo "SCENARIO F FAILED"; cat "$WORK/probe-f.log"; exit 1
fi
echo "Scenario F OK: MODIFIED in place, same reference id, consumer followed the endpoint"
grep -E '  [✓✗]' "$WORK/probe-f.log" || true
# The second instance owns the (modified) registration now; its withdraw must
# go first, otherwise the first instance's identity-based withdraw would
# remove it (same (name, version) — the price of sharing an identity).
kill "$F2_PID"; wait "$F2_PID" 2>/dev/null || true
kill "$F1_PID"; wait "$F1_PID" 2>/dev/null || true

# ============================================================ Scenario G
# DEPRECATE_AND_DRAIN (#58, #45): version 2.0.0 publishes with replaces=1.0.0
# while the consumer holds a lease on 1.0.0. UPGRADE_AVAILABLE, lookups prefer
# the successor, the predecessor keeps serving until the lease is released,
# then the broker's policy sweep retires it and the locator rebinds.
log "Scenario G: DEPRECATE_AND_DRAIN with two provider versions"
require_port_free 9091
require_port_free 9092
start_jar payment-g1 "$PROVIDER_JAR" "$WORK/payment-g1"
G1_PID=$LAST_PID
wait_for_line "$WORK/payment-g1.log" "published payments-java" 60
(cd "$TS/examples/payment" \
  && BROKER_URL="$BROKER_URL" SCENARIO=G exec node --import tsx harness-probe-lifecycle.ts) \
  >"$WORK/probe-g.log" 2>&1 &
PROBE_G_PID=$!
PIDS+=("$PROBE_G_PID")
wait_for_line "$WORK/probe-g.log" "PROBE_READY" 90
PAYMENTS_HTTP_PORT=9092 PAYMENTS_PUBLIC_URL="http://localhost:9092/payments" \
  PAYMENTS_IMPL_VERSION=2.0.0 PAYMENTS_REPLACES_VERSION=1.0.0 PAYMENTS_UPDATE_POLICY=DEPRECATE_AND_DRAIN \
  start_jar payment-g2 "$PROVIDER_JAR" "$WORK/payment-g2"
G2_PID=$LAST_PID
wait_for_line "$WORK/payment-g2.log" "published payments-java" 60
if ! wait "$PROBE_G_PID"; then
  echo "SCENARIO G FAILED"; cat "$WORK/probe-g.log"; exit 1
fi
echo "Scenario G OK: UPGRADE_AVAILABLE, drain on lease release, rebind to the successor"
grep -E '  [✓✗]' "$WORK/probe-g.log" || true
kill "$G2_PID"; wait "$G2_PID" 2>/dev/null || true
kill "$G1_PID"; wait "$G1_PID" 2>/dev/null || true

# ============================================================ Scenario H
# Provider liveness (#52): the Java provider heartbeats every 2 s; SIGKILL
# leaves no withdraw and no shutdown hook behind. The broker has to notice the
# silence (two missed heartbeats, then its liveness sweep) and retire the
# registration with PROVIDER_LOST — the consumer learns the true cause and the
# lookup stops listing the dead endpoint.
log "Scenario H: provider dies without withdraw (SIGKILL) -> PROVIDER_LOST via heartbeat"
require_port_free 9091
ARMED_BEFORE=$(grep -c "provider liveness armed" "$WORK/broker.log" || true)
DDSR_PROVIDER_HEARTBEAT_SECONDS=2 start_jar payment-h "$PROVIDER_JAR" "$WORK/payment-h"
H_PID=$LAST_PID
wait_for_line "$WORK/payment-h.log" "published payments-java" 60
(cd "$TS/examples/payment" \
  && BROKER_URL="$BROKER_URL" SCENARIO=H corepack pnpm exec tsx harness-probe-lifecycle.ts) \
  >"$WORK/probe-h.log" 2>&1 &
PROBE_H_PID=$!
PIDS+=("$PROBE_H_PID")
wait_for_line "$WORK/probe-h.log" "PROBE_READY" 90
# The kill must hit a supervised registration: wait for the broker to log
# that THIS instance's heartbeat armed the liveness (earlier scenarios armed
# the same identity before, hence the count instead of a plain match).
for _ in $(seq 1 60); do
  [ "$(grep -c "provider liveness armed" "$WORK/broker.log" || true)" -gt "$ARMED_BEFORE" ] && break
  sleep 0.5
done
kill -9 "$H_PID"; wait "$H_PID" 2>/dev/null || true
KILL_MS=$(now_ms)
if ! wait "$PROBE_H_PID"; then
  echo "SCENARIO H FAILED"; cat "$WORK/probe-h.log"; exit 1
fi
LOST_MS=$(grep -o 'LOST_AT [0-9]*' "$WORK/probe-h.log" | awk '{print $2}')
if [ -z "$LOST_MS" ]; then
  echo "SCENARIO H FAILED: no PROVIDER_LOST reached the consumer"; cat "$WORK/probe-h.log"; exit 1
fi
H_LATENCY_MS=$((LOST_MS - KILL_MS))
if [ "$H_LATENCY_MS" -gt 30000 ]; then
  echo "SCENARIO H FAILED: PROVIDER_LOST took $H_LATENCY_MS ms (heartbeat 2 s, expected < 30 s)"; exit 1
fi
echo "Scenario H OK: PROVIDER_LOST reached the consumer $H_LATENCY_MS ms after SIGKILL (heartbeat 2 s, two missed + sweep)"
grep -E '  [✓✗]' "$WORK/probe-h.log" || true

log "HARNESS PASSED (A + B + F + G + H)"
