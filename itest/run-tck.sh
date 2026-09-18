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

echo "=== run the TCK ==="
set +e
( cd "$ROOT" && ./gradlew :org.eclipse.fennec.services.rsa.tck:testrun.rsa-tck ) 2>&1 | tee "$WORK/tck.log"
RC=${PIPESTATUS[0]}
set -e
echo "=== TCK exit $RC — reports under org.eclipse.fennec.services.rsa.tck/generated/test-reports ==="
exit "$RC"
