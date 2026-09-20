#!/usr/bin/env bash
#
# Copyright (c) 2026 Contributors to the Eclipse Foundation.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Run the OSGi Remote Service Admin TCK against this implementation (#99).
#
# The broker runs as its own process, outside both frameworks the TCK
# drives: the parent that hosts the tests and the child it creates. Both
# have to see one and the same broker — that is what makes a service
# exported in one discoverable in the other — and a broker inside the
# parent would drag its own dependencies into the launch and clash with a
# second one in the child.
#
# Expected to run red before it runs green. The point of running it early
# is to be told what is missing rather than to guess.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$ROOT/itest/work/tck"
BROKER_JAR="$ROOT/org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker.jar"
mkdir -p "$WORK/broker"

cleanup() { [ -n "${BROKER_PID:-}" ] && kill "$BROKER_PID" 2>/dev/null || true; }
trap cleanup EXIT

echo "=== build + export the broker ==="
( cd "$ROOT" && ./gradlew --quiet :org.eclipse.fennec.services.broker.rest:export.broker )

echo "=== start the broker ==="
( cd "$WORK/broker" && exec java -Dgosh.args=--nointeractive -jar "$BROKER_JAR" ) >"$WORK/broker.log" 2>&1 &
BROKER_PID=$!
for _ in $(seq 1 60); do curl -sf -o /dev/null http://localhost:8887/ddsr/rest/catalog && break; sleep 1; done
curl -sf -o /dev/null http://localhost:8887/ddsr/rest/catalog || { echo "broker did not come up"; exit 1; }

REPORTS="$ROOT/org.eclipse.fennec.services.rsa.tck/generated/test-reports"

echo "=== run the TCK ==="
# --rerun, and the old reports out of the way first. Without this the
# task can be UP-TO-DATE, and then the script reports success without a
# single test having run — which happened, and looks exactly like a
# green run from the outside.
rm -rf "$REPORTS"
set +e
( cd "$ROOT" && ./gradlew :org.eclipse.fennec.services.rsa.tck:testrun.rsa-tck --rerun ) 2>&1 | tee "$WORK/tck.log"
RC=${PIPESTATUS[0]}
set -e

# A green exit code is not the evidence; the report is. Nothing written
# means nothing ran, and that must not pass.
SUMMARY=$(grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
          "$REPORTS"/*/*.xml 2>/dev/null || true)
if [ -z "$SUMMARY" ]; then
  echo "=== TCK produced no test report — treating this as a failure ==="
  exit 1
fi
echo "=== TCK exit $RC — $SUMMARY ==="
case "$SUMMARY" in
  *'tests="0"'*) echo "=== zero tests ran — treating this as a failure ==="; exit 1 ;;
esac
exit "$RC"
