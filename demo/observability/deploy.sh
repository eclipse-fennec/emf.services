#!/usr/bin/env bash
# Copyright (c) 2026 Data In Motion and others.
# SPDX-License-Identifier: EPL-2.0
#
# Starts the observability stack as one podman pod: OpenTelemetry
# Collector, Tempo, Prometheus, Loki and Grafana.
#
# The pod definition is observability-stack.yaml; the dashboards in
# dashboards/ are turned into a ConfigMap here rather than pasted into
# that file, so a dashboard stays an editable JSON file.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STACK="$DIR/observability-stack.yaml"
PLAY="${TMPDIR:-/tmp}/fennec-observability-stack.yaml"

command -v podman >/dev/null || { echo "podman is not installed" >&2; exit 1; }

echo ">> composing the pod definition with $(ls "$DIR"/dashboards/*.json | wc -l) dashboard(s)"
python3 "$DIR/with-dashboards.py" --stack "$STACK" --dashboards "$DIR/dashboards" --out "$PLAY"

echo ">> stopping a previous pod, if any"
podman pod stop observability >/dev/null 2>&1 || true
podman pod rm observability >/dev/null 2>&1 || true

echo ">> starting the stack"
podman kube play "$PLAY"

cat <<'ENDPOINTS'

=== the stack is up ===
  Grafana      http://localhost:3000        (no login; dashboard "Fennec Services")
  OTLP HTTP    http://localhost:4318        (what the frameworks send to)
  OTLP gRPC    localhost:4317

  Internal to the pod: Prometheus :9090, Loki :3100, Tempo :3200

  logs:  podman pod logs observability
  stop:  demo/observability/stop.sh
ENDPOINTS
