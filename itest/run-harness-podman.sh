#!/usr/bin/env bash
# Copyright (c) 2026 Data In Motion and others.
# SPDX-License-Identifier: EPL-2.0
#
# The containerized variant of run-harness.sh (FR-P4 / D10): broker,
# providers and the TS probe run as podman containers with
# --network=host, so the localhost defaults of all configs apply
# unchanged. Podman is the vehicle, not a requirement — the host-process
# variant covers the same assertions.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$ROOT/itest/work-podman"
BROKER_URL="http://localhost:8887/ddsr/rest"
CONTAINERS=()

log() { printf '\n=== %s ===\n' "$*"; }
now_ms() { date +%s%3N; }

cleanup() {
  for c in "${CONTAINERS[@]:-}"; do
    podman rm -f "$c" >/dev/null 2>&1 || true
  done
}
trap cleanup EXIT

wait_for_url() { # url timeout_s
  local url="$1" timeout="${2:-60}" waited=0
  until curl -sf -o /dev/null "$url"; do
    sleep 0.5
    waited=$((waited + 1))
    # if/then, NOT `[ ] && { }`: the failing guard list would become the
    # loop's (and function's) exit status and set -e would kill the
    # script exactly when the wait SUCCEEDS.
    if [ "$waited" -ge $((timeout * 2)) ]; then
      echo "TIMEOUT waiting for $url" >&2
      return 1
    fi
  done
}

wait_for_log() { # container pattern timeout_s
  local container="$1" pattern="$2" timeout="${3:-60}" waited=0
  local snapshot="$WORK/.$container.wait.log"
  # NOT `podman logs | grep -q`: grep -q exits on the first match, podman
  # logs then dies of SIGPIPE (141), and with pipefail the pipeline fails
  # EXACTLY when the pattern is found — the wait would never succeed on
  # containers with large logs. Capture to a file first, then grep.
  while :; do
    podman logs "$container" >"$snapshot" 2>&1 || true
    if grep -q "$pattern" "$snapshot"; then
      return 0
    fi
    sleep 0.5
    waited=$((waited + 1))
    if [ "$waited" -ge $((timeout * 2)) ]; then
      echo "TIMEOUT waiting for '$pattern' in $container" >&2
      # thread dump into the log before capturing it
      podman exec "$container" kill -3 1 >/dev/null 2>&1 || true
      sleep 2
      podman logs "$container" >"$WORK/$container.timeout.log" 2>&1 || true
      echo "full log: $WORK/$container.timeout.log" >&2
      return 1
    fi
  done
}

run_container() { # name image args...
  local name="$1" image="$2"; shift 2
  podman run -d --replace --name "$name" --network=host "$@" "$image" >/dev/null
  CONTAINERS+=("$name")
}

# ---------------------------------------------------------------- build
log "gradle export + image build"
rm -rf "$WORK"; mkdir -p "$WORK"
# clean slate: leftovers from earlier runs would otherwise be --replace'd
# mid-scenario (e.g. a broker torn down while a provider activates)
podman rm -f ddsr-broker ddsr-payment-java ddsr-probe ddsr-provider-ts ddsr-client-java \
  ddsr-broker-mqtt ddsr-client-mqtt ddsr-payment-mqtt ddsr-provider-ts-mqtt ddsr-probe-mqtt >/dev/null 2>&1 || true
rm -f "$ROOT"/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker.jar \
      "$ROOT"/org.eclipse.fennec.services.examples.payment/generated/distributions/executable/payment-provider.jar \
      "$ROOT"/org.eclipse.fennec.services.client.java/generated/distributions/executable/client.jar
rm -f "$ROOT"/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker-mqtt.jar \
      "$ROOT"/org.eclipse.fennec.services.client.java/generated/distributions/executable/client-mqtt.jar
(cd "$ROOT" && ./gradlew build \
  :org.eclipse.fennec.services.broker.rest:export.broker \
  :org.eclipse.fennec.services.broker.rest:export.broker-mqtt \
  :org.eclipse.fennec.services.examples.payment:export.payment-provider \
  :org.eclipse.fennec.services.client.java:export.client \
  :org.eclipse.fennec.services.client.java:export.client-mqtt) >"$WORK/gradle.log" 2>&1 \
  || { tail -30 "$WORK/gradle.log"; exit 1; }

podman build -q -f "$ROOT/itest/containers/Containerfile.java" --build-arg JAR=broker.jar \
  -t ddsr/broker "$ROOT/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/"
podman build -q -f "$ROOT/itest/containers/Containerfile.java" --build-arg JAR=payment-provider.jar \
  -t ddsr/payment-java "$ROOT/org.eclipse.fennec.services.examples.payment/generated/distributions/executable/"
podman build -q -f "$ROOT/itest/containers/Containerfile.java" --build-arg JAR=client.jar \
  -t ddsr/client-java "$ROOT/org.eclipse.fennec.services.client.java/generated/distributions/executable/"
podman build -q -f "$ROOT/itest/containers/Containerfile.java" --build-arg JAR=broker-mqtt.jar \
  -t ddsr/broker-mqtt "$ROOT/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/"
podman build -q -f "$ROOT/itest/containers/Containerfile.java" --build-arg JAR=client-mqtt.jar \
  -t ddsr/client-mqtt "$ROOT/org.eclipse.fennec.services.client.java/generated/distributions/executable/"
# --network=host: the ONLY image whose build needs the network (corepack +
# pnpm install). Rootless podman 5.x defaults to pasta, which copies the
# host's /etc/resolv.conf into the build netns verbatim — on hosts resolving
# via the systemd-resolved stub (nameserver 127.0.0.53, e.g. the GitHub
# runners) that address is unreachable from inside, and every lookup dies as
# EAI_AGAIN. In the host netns the stub resolves as it does for the harness
# itself. podman 4.x with slirp4netns rewrote resolv.conf and did not need
# this, which is why the podman harness only started failing in CI.
podman build -q --network=host -f "$ROOT/itest/containers/Containerfile.ts" -t ddsr/ts "$ROOT/ddsr-ts-client/"

# --------------------------------------------------------------- broker
log "starting broker container"
run_container ddsr-broker ddsr/broker
wait_for_url "$BROKER_URL/catalog" 60
echo "broker up"

# ============================================================ Scenario A
log "Scenario A: Java provider (container) -> TS consumer (container)"
run_container ddsr-payment-java ddsr/payment-java
wait_for_log ddsr-payment-java "published payments-java" 150

podman run -d --replace --name ddsr-probe --network=host \
  -e BROKER_URL="$BROKER_URL" -e EXPECT_LANG=java ddsr/ts harness-probe.ts >/dev/null
CONTAINERS+=(ddsr-probe)

wait_for_log ddsr-probe "PROBE_READY" 150
log "Scenario A: stopping the Java provider (podman stop = SIGTERM)"
podman stop -t 20 ddsr-payment-java >/dev/null
PROVIDER_EXIT_MS=$(now_ms)

probe_rc=$(podman wait ddsr-probe)
podman logs ddsr-probe >"$WORK/probe-a.log" 2>&1
if [ "$probe_rc" != "0" ]; then
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
log "Scenario B: TS provider (container) -> Java consumer (container)"
podman run -d --replace --name ddsr-provider-ts --network=host \
  -e BROKER_URL="$BROKER_URL" -e SERVICE_URL="http://localhost:9090/payments" ddsr/ts >/dev/null
CONTAINERS+=(ddsr-provider-ts)
wait_for_log ddsr-provider-ts "published as reference" 120

run_container ddsr-client-java ddsr/client-java
wait_for_log ddsr-client-java 'ts.charge(10.0, "EUR") =' 150
echo "Java consumer called the TS provider:"
podman logs ddsr-client-java 2>&1 | grep -E 'ts\.(getBalance|charge)'

log "Scenario B: stopping the TS provider — FR-P3 order check"
podman stop -t 20 ddsr-provider-ts >/dev/null
ts_rc=$(podman wait ddsr-provider-ts)
podman logs ddsr-provider-ts >"$WORK/provider-ts.log" 2>&1
if [ "$ts_rc" != "0" ]; then
  echo "SCENARIO B FAILED: TS provider exited $ts_rc"; cat "$WORK/provider-ts.log"; exit 1
fi
grep -q 'unregistration confirmed by broker' "$WORK/provider-ts.log" \
  || { echo "SCENARIO B FAILED: no broker confirmation before endpoint stop"; cat "$WORK/provider-ts.log"; exit 1; }
if curl -sf "$BROKER_URL/references?interface=Payment" | grep -q 'payments-ts'; then
  echo "SCENARIO B FAILED: broker still lists payments-ts after withdraw"; exit 1
fi
echo "Scenario B OK: withdraw confirmed before endpoint stop; broker no longer lists payments-ts"

# ============================================================ Scenario C
log "Scenario C: MQTT wire proof (mosquitto container, TS transport over TCP)"
podman run -d --replace --name ddsr-mosquitto --network=host \
  -v "$ROOT/itest/containers/mosquitto.conf:/mosquitto/config/mosquitto.conf:ro,Z" \
  docker.io/library/eclipse-mosquitto:2 >/dev/null
CONTAINERS+=(ddsr-mosquitto)
wait_for_log ddsr-mosquitto "mosquitto version 2" 60

podman run --rm --name ddsr-mqtt-probe --network=host \
  -e MQTT_URL="mqtt://localhost:1883" ddsr/ts mqtt-wire-probe.ts >"$WORK/mqtt-probe.log" 2>&1 \
  || { echo "SCENARIO C FAILED"; cat "$WORK/mqtt-probe.log"; exit 1; }
grep -q "MQTT_PROBE_OK" "$WORK/mqtt-probe.log" \
  || { echo "SCENARIO C FAILED"; cat "$WORK/mqtt-probe.log"; exit 1; }
echo "Scenario C OK: broker-shaped event document delivered and decoded over real MQTT/TCP"

# ============================================================ Scenario D
log "Scenario D: Java MQTT wire proof (broker sink + client source over mosquitto TCP)"
# Fresh broker+client pair from the -mqtt launch variants: same
# bundles plus org.eclipse.fennec.services.itest.mqtt.config, which
# wakes the dormant MQTT transports (configurationPolicy REQUIRE) with
# localhost Mosquitto settings and ranks the client's MQTT EventSource
# above SSE. Config as a BUNDLE on purpose: configurator.initial is
# parsed before the jakarta.json provider bundle starts ("Invalid
# JSON"), and JAVA_TOOL_OPTIONS strips the double quotes inline JSON
# would need.
podman rm -f ddsr-client-java ddsr-broker >/dev/null 2>&1 || true
run_container ddsr-broker-mqtt ddsr/broker-mqtt
wait_for_url "$BROKER_URL/catalog" 60
wait_for_log ddsr-broker-mqtt "event transport connected to tcp://localhost:1883" 60

run_container ddsr-client-mqtt ddsr/client-mqtt
wait_for_log ddsr-client-mqtt "client transport connected to tcp://localhost:1883" 90

run_container ddsr-payment-mqtt ddsr/payment-java
wait_for_log ddsr-payment-mqtt "published payments-java" 150
wait_for_log ddsr-client-mqtt "EVENT REGISTERED" 60

podman stop -t 20 ddsr-payment-mqtt >/dev/null
wait_for_log ddsr-client-mqtt "EVENT UNREGISTERING" 60

podman logs ddsr-client-mqtt >"$WORK/client-mqtt.log" 2>&1
# Ordering control: SSE may legitimately carry the stream for a moment
# (RestEventSource activates before the configurator ranks MQTT in; the
# SDK then closes and reopens the stream on the greedy rebind). What
# pins the EVENTs onto MQTT is the order: the LAST stream (re)open
# before the events must be the MQTT subscription, with no SSE
# subscription after it.
mqtt_sub=$(grep -n "subscribed to ddsr/events/#" "$WORK/client-mqtt.log" | tail -1 | cut -d: -f1)
sse_sub=$(grep -n "subscribed to http" "$WORK/client-mqtt.log" | tail -1 | cut -d: -f1)
first_event=$(grep -n "EVENT REGISTERED" "$WORK/client-mqtt.log" | head -1 | cut -d: -f1)
if [ -z "$mqtt_sub" ] || [ -n "$sse_sub" ] && [ "$sse_sub" -gt "$mqtt_sub" ]; then
  echo "SCENARIO D FAILED: events flowed over SSE, not MQTT (mqtt_sub=$mqtt_sub sse_sub=$sse_sub)"
  cat "$WORK/client-mqtt.log"; exit 1
fi
if [ -z "$first_event" ] || [ "$first_event" -lt "$mqtt_sub" ]; then
  echo "SCENARIO D FAILED: EVENT not after the MQTT subscription (event=$first_event mqtt_sub=$mqtt_sub)"
  cat "$WORK/client-mqtt.log"; exit 1
fi
grep -q "provider=payments-java" "$WORK/client-mqtt.log" \
  || { echo "SCENARIO D FAILED: event without provider payload"; cat "$WORK/client-mqtt.log"; exit 1; }
echo "Scenario D OK: Java lifecycle events delivered over real MQTT/TCP (REGISTERED + UNREGISTERING)"

# ============================================================ Scenario E
log "Scenario E: MQTT service flavor (A2 Etappe 2) — TS provider serves Payment over MQTT, probe invokes it"
# The TS provider announces BOTH flavors on one implementation (the DoD
# scenario's "same interface, different transports") and listens on the
# mosquitto container; the probe reads the broker address from the
# ANNOUNCED MqttFlavor and calls getBalance over topics.
podman rm -f ddsr-payment-mqtt >/dev/null 2>&1 || true
podman run -d --replace --name ddsr-provider-ts-mqtt --network=host \
  -e BROKER_URL="$BROKER_URL" -e SERVICE_URL="http://localhost:9090/payments" \
  -e MQTT_URL="mqtt://localhost:1883" ddsr/ts >/dev/null
CONTAINERS+=(ddsr-provider-ts-mqtt)
wait_for_log ddsr-provider-ts-mqtt "published as reference" 120
wait_for_log ddsr-provider-ts-mqtt "serving Payment over MQTT" 30

podman run -d --replace --name ddsr-probe-mqtt --network=host \
  -e BROKER_URL="$BROKER_URL" -e EXPECT_LANG=typescript -e EXPECT_MQTT=1 \
  ddsr/ts harness-probe.ts >/dev/null
CONTAINERS+=(ddsr-probe-mqtt)
wait_for_log ddsr-probe-mqtt "PROBE_READY" 150

podman stop -t 20 ddsr-provider-ts-mqtt >/dev/null
probe_mqtt_rc=$(podman wait ddsr-probe-mqtt)
podman logs ddsr-probe-mqtt >"$WORK/probe-e.log" 2>&1
if [ "$probe_mqtt_rc" != "0" ]; then
  echo "SCENARIO E FAILED"; cat "$WORK/probe-e.log"; exit 1
fi
grep -q "✓ mqtt-flavor-announced" "$WORK/probe-e.log" \
  || { echo "SCENARIO E FAILED: provider did not announce the MqttFlavor"; cat "$WORK/probe-e.log"; exit 1; }
grep -q "✓ mqtt-invoke-getBalance" "$WORK/probe-e.log" \
  || { echo "SCENARIO E FAILED: invocation over MQTT failed"; cat "$WORK/probe-e.log"; exit 1; }
echo "Scenario E OK: Payment invoked over the announced MQTT flavor (request/response via mosquitto)"
grep -E '  [✓✗] mqtt' "$WORK/probe-e.log" || true

log "HARNESS (podman) PASSED (A + B + C + D + E)"
