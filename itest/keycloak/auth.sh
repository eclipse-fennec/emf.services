# shellcheck shell=bash
# Copyright (c) 2026 Contributors to the Eclipse Foundation.
# SPDX-License-Identifier: EPL-2.0
#
# Authentication for both harnesses (SECURITY.md §7a, slice 4): the broker
# runs in mode bearer against a Keycloak realm, and every process gets the
# client identity it calls the broker as. Sourced, not executed.
#
#   HARNESS_AUTH=bearer (default)  Keycloak in a container, broker secured
#   HARNESS_AUTH=none              no Keycloak, broker open — the developer
#                                  path, and what the harness ran before
#
# The realm (fennec-realm.json) has one confidential client per role in the
# harness; its secret is "<client>-secret". Audiences are optional client
# scopes of the same name: a token for the broker is asked for with
# scope=ddsr-broker, one for the RSA example with scope=rsa-example, and
# neither carries the other's audience (SECURITY.md §1). The broker takes
# the caller's name from azp — the client id — so a consumer's session id
# is its client id. Roles are realm roles, read from realm_access.roles —
# the broker's three (service-provider, service-consumer, service-admin,
# SECURITY.md §3) and the RSA example's ledger roles.

HARNESS_AUTH="${HARNESS_AUTH:-bearer}"
KEYCLOAK_IMAGE="${KEYCLOAK_IMAGE:-quay.io/keycloak/keycloak:26.4}"
KEYCLOAK_PORT="${KEYCLOAK_PORT:-18080}"
KEYCLOAK_REALM_URL="http://localhost:${KEYCLOAK_PORT}/realms/fennec"
KEYCLOAK_TOKEN_URI="${KEYCLOAK_REALM_URL}/protocol/openid-connect/token"

case "$HARNESS_AUTH" in
  bearer|none) ;;
  *) echo "HARNESS_AUTH must be bearer or none, not '$HARNESS_AUTH'" >&2; exit 1 ;;
esac

secured() { [ "$HARNESS_AUTH" = bearer ]; }

# Keycloak on the host network, realm imported at start. --replace drops a
# container a previous run left behind.
start_keycloak() {
  secured || return 0
  if ss -ltn 2>/dev/null | grep -q ":${KEYCLOAK_PORT} "; then
    podman rm -f ddsr-keycloak >/dev/null 2>&1 || true
    sleep 1
  fi
  podman run -d --replace --name ddsr-keycloak --network=host \
    -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
    -v "$ROOT/itest/keycloak:/opt/keycloak/data/import:ro,Z" \
    "$KEYCLOAK_IMAGE" start-dev --http-port="$KEYCLOAK_PORT" --import-realm >/dev/null
  local waited=0
  until curl -sf -o /dev/null "$KEYCLOAK_REALM_URL/.well-known/openid-configuration"; do
    sleep 1
    waited=$((waited + 1))
    if [ "$waited" -ge 120 ]; then
      echo "TIMEOUT waiting for Keycloak at $KEYCLOAK_REALM_URL" >&2
      podman logs --tail 30 ddsr-keycloak >&2 || true
      return 1
    fi
  done
  echo "keycloak up after ${waited}s ($KEYCLOAK_REALM_URL)"
}

stop_keycloak() {
  secured || return 0
  podman rm -f ddsr-keycloak >/dev/null 2>&1 || true
}

# What the broker needs to verify tokens of that realm. Empty when open,
# and then the broker falls back to its launch's mode none.
broker_auth_env() {
  secured || return 0
  echo "DDSR_AUTH_MODE=bearer"
  echo "DDSR_JWT_ISSUER=$KEYCLOAK_REALM_URL"
  echo "DDSR_JWT_JWKS_URI=$KEYCLOAK_REALM_URL/protocol/openid-connect/certs"
  echo "DDSR_JWT_SUBJECT_CLAIM=azp"
  echo "DDSR_JWT_SCOPE_CLAIM=realm_access.roles"
}

# Runs a command (start_jar) with the broker's half of the above.
as_broker() { # command...
  if secured; then
    DDSR_AUTH_MODE=bearer DDSR_JWT_ISSUER="$KEYCLOAK_REALM_URL" \
      DDSR_JWT_JWKS_URI="$KEYCLOAK_REALM_URL/protocol/openid-connect/certs" DDSR_JWT_SUBJECT_CLAIM=azp \
      DDSR_JWT_SCOPE_CLAIM=realm_access.roles "$@"
  else
    "$@"
  fi
}

# The same, as podman -e arguments, one per line (use with mapfile).
broker_auth_args() {
  local line
  while IFS= read -r line; do
    [ -n "$line" ] && printf -- '-e\n%s\n' "$line"
  done < <(broker_auth_env)
}

# Runs a command (a function such as start_jar, too) as the given client.
as_client() { # client command...
  local client="$1"; shift
  if secured; then
    DDSR_OAUTH_TOKEN_URI="$KEYCLOAK_TOKEN_URI" DDSR_OAUTH_CLIENT_ID="$client" \
      DDSR_OAUTH_CLIENT_SECRET="$client-secret" DDSR_OAUTH_SCOPE=ddsr-broker "$@"
  else
    "$@"
  fi
}

# The same inside a subshell that ends in exec.
export_client() { # client
  secured || return 0
  export DDSR_OAUTH_TOKEN_URI="$KEYCLOAK_TOKEN_URI" DDSR_OAUTH_CLIENT_ID="$1" DDSR_OAUTH_CLIENT_SECRET="$1-secret" \
    DDSR_OAUTH_SCOPE=ddsr-broker
}

# The same as podman -e arguments, one per line (use with mapfile).
client_args() { # client
  secured || return 0
  printf -- '-e\n%s\n' "DDSR_OAUTH_TOKEN_URI=$KEYCLOAK_TOKEN_URI" \
    "DDSR_OAUTH_CLIENT_ID=$1" "DDSR_OAUTH_CLIENT_SECRET=$1-secret" "DDSR_OAUTH_SCOPE=ddsr-broker"
}

# What a node that serves guarded contracts verifies, as podman -e
# arguments: tokens of this realm for its own audience, roles from
# realm_access.roles, the caller named by azp.
provider_args() { # audience
  secured || return 0
  printf -- '-e\n%s\n' "DDSR_JWT_ISSUER=$KEYCLOAK_REALM_URL" \
    "DDSR_JWT_JWKS_URI=$KEYCLOAK_REALM_URL/protocol/openid-connect/certs" \
    "DDSR_JWT_AUDIENCES=$1" "DDSR_JWT_SCOPE_CLAIM=realm_access.roles" "DDSR_JWT_SUBJECT_CLAIM=azp"
}

token_for() { # client [scope] — the broker's audience unless another is asked for
  curl -sf -u "$1:$1-secret" -d grant_type=client_credentials -d "scope=${2:-ddsr-broker}" "$KEYCLOAK_TOKEN_URI" \
    | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
}

# curl against the broker as the harness itself.
curl_broker() { # curl args...
  if secured; then
    curl -H "Authorization: Bearer $(token_for harness)" "$@"
  else
    curl "$@"
  fi
}

# The role matrix, checked from outside right after the broker is up: a
# consumer may not announce, and only an admin reads the whole registry.
check_role_matrix() {
  secured || return 0
  local consumer status
  consumer="Authorization: Bearer $(token_for java-client)"
  status=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "$consumer" -H 'Content-Type: application/xml' \
    --data-binary '<x/>' "$BROKER_URL/implementations/")
  [ "$status" = 403 ] || { echo "ROLE MATRIX BROKEN: a consumer's publish answered $status, expected 403"; return 1; }
  status=$(curl -s -o /dev/null -w '%{http_code}' -H "$consumer" "$BROKER_URL/registry")
  [ "$status" = 403 ] || { echo "ROLE MATRIX BROKEN: a consumer read the registry with $status, expected 403"; return 1; }
  status=$(curl_broker -s -o /dev/null -w '%{http_code}' "$BROKER_URL/registry")
  [ "$status" = 200 ] || { echo "ROLE MATRIX BROKEN: the admin read the registry with $status, expected 200"; return 1; }
  echo "  ✓ role-matrix: a consumer may not publish nor read the registry, an admin may"
}
