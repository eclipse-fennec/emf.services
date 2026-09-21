#!/usr/bin/env bash
# Copyright (c) 2026 Data In Motion and others.
# SPDX-License-Identifier: EPL-2.0
#
# Stops and removes the observability pod. The data goes with it — the
# volumes are emptyDir, which is what a demo wants.
set -euo pipefail

podman pod stop observability >/dev/null 2>&1 || true
podman pod rm observability >/dev/null 2>&1 || true
echo ">> observability stack stopped"
