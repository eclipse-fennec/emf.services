# Copyright (c) 2026 Data In Motion and others.
# SPDX-License-Identifier: EPL-2.0
#
# DDSR TypeScript workspace image. Build with ddsr-ts-client/ as
# context (its .dockerignore keeps node_modules/dist out):
#
#   podman build -f itest/containers/Containerfile.ts -t ddsr/ts ddsr-ts-client/
#
# Run any example/probe by overriding the command:
#   podman run --network=host -e BROKER_URL=... ddsr/ts provider.ts
#   podman run --network=host -e BROKER_URL=... ddsr/ts harness-probe.ts
FROM docker.io/library/node:24-alpine
RUN corepack enable pnpm
WORKDIR /app
COPY . .
RUN corepack pnpm install --frozen-lockfile && corepack pnpm -r build
WORKDIR /app/examples/payment
# node directly (no pnpm wrapper): SIGTERM must reach the process so
# the FR-P3 shutdown hooks run.
ENTRYPOINT ["node", "--import", "tsx"]
CMD ["provider.ts"]
