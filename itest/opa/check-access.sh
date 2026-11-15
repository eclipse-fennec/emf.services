#!/usr/bin/env bash
# Copyright (c) 2026 Contributors to the Eclipse Foundation.
# SPDX-License-Identifier: EPL-2.0
#
# Runs the access checklist of the tag facade (#282) against a gateway and
# a broker, and writes a JUnit XML report — one testcase per row, so any CI
# or test dashboard can show and keep the result.
#
#   check-access.sh <matrix.tsv> <gateway-base> <broker-base> <report.xml>
#
#   gateway-base  e.g. http://127.0.0.1:18092/ddsr/rest — what the G: rows go to
#   broker-base   e.g. http://localhost:8887/ddsr/rest  — what the B: rows go to
#
# Tokens come from token_for <client> (itest/keycloak/auth.sh, sourced when
# not defined); a deployment of its own defines token_for before calling, or
# sets TOKEN_<CLIENT> (upper case, '-' as '_') per client. Exit code 0 when
# every row holds.
set -uo pipefail

MATRIX="$1"
GATEWAY="$2"
BROKER="$3"
REPORT="$4"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if ! declare -F token_for >/dev/null; then
  # shellcheck source=../keycloak/auth.sh
  . "$HERE/../keycloak/auth.sh"
fi

declare -A TOKENS
token() { # client
  local var="TOKEN_$(tr 'a-z-' 'A-Z_' <<<"$1")"
  if [ -n "${!var:-}" ]; then printf '%s' "${!var}"; return; fi
  if [ -z "${TOKENS[$1]:-}" ]; then TOKENS[$1]=$(token_for "$1"); fi
  printf '%s' "${TOKENS[$1]}"
}

# A token that looks right and is not: RS256 in the header, the group
# asked for, a signature nobody made.
forged() { # group
  local b64='base64 -w0'
  local header payload
  header=$(printf '{"alg":"RS256","typ":"JWT","kid":"forged"}' | $b64 | tr '+/' '-_' | tr -d '=')
  payload=$(printf '{"azp":"forger","groups":["%s"],"realm_access":{"roles":["tags-consumer","service-admin"]},"exp":4102444800}' "$1" \
    | $b64 | tr '+/' '-_' | tr -d '=')
  printf '%s.%s.%s' "$header" "$payload" "c2lnbmF0dXJl"
}

SESSION=$(mktemp)
trap 'rm -f "$SESSION" "$SESSION.body"' EXIT
printf '%s' '<?xml version="1.0" encoding="UTF-8"?>
<services:ConsumerSession xmlns:services="http://eclipse.org/fennec/services/1.0" consumerId="shop-instant"/>' >"$SESSION"

xml() { sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' -e 's/"/\&quot;/g' <<<"$1"; }

total=0
failed=0
cases=""
started=$(date +%s.%N)
while IFS=$'\t' read -r id caller method target expect check why; do
  [ -z "$id" ] && continue
  case "$id" in \#*) continue ;; esac
  total=$((total + 1))
  base="$GATEWAY"
  path="${target#G:}"
  if [ "${target#B:}" != "$target" ]; then
    base="$BROKER"
    path="${target#B:}"
  fi
  args=(-s --path-as-is -m 5 -o "$SESSION.body" -w '%{http_code}' -X "$method")
  case "$caller" in
    none) ;;
    header:*) args+=(-H "Authorization: ${caller#header:}") ;;
    forged:*) args+=(-H "Authorization: Bearer $(forged "${caller#forged:}")") ;;
    client:*) args+=(-H "Authorization: Bearer $(token "${caller#client:}")") ;;
  esac
  if [ "$method" = PUT ] && [[ "$path" == */consumers/* ]]; then
    args+=(-H 'Content-Type: application/xml' --data-binary "@$SESSION")
  fi
  one_started=$(date +%s.%N)
  status=$(curl "${args[@]}" "$base$path" 2>/dev/null)
  [ -z "$status" ] && status=000
  elapsed=$(awk -v a="$one_started" -v b="$(date +%s.%N)" 'BEGIN { printf "%.3f", b - a }')
  body=$(cat "$SESSION.body" 2>/dev/null || true)

  problem=""
  case "$expect" in
    !*) [ "$status" = "${expect#!}" ] && problem="answered $status, which it must not" ;;
    ?xx) [ "${status:0:1}" = "${expect:0:1}" ] || problem="answered $status, expected $expect" ;;
    *) [ "$status" = "$expect" ] || problem="answered $status, expected $expect" ;;
  esac
  if [ -z "$problem" ] && [ -n "$check" ]; then
    case "$check" in
      refs=*) count=$(grep -o '<references' <<<"$body" | wc -l)
              [ "$count" = "${check#refs=}" ] || problem="$count reference(s), expected ${check#refs=}" ;;
      has:*) grep -q "${check#has:}" <<<"$body" || problem="the answer does not name ${check#has:}" ;;
      lacks:*) grep -q "${check#lacks:}" <<<"$body" && problem="the answer names ${check#lacks:}" ;;
    esac
  fi

  group="${id%%/*}"
  name="$id: $why"
  if [ -z "$problem" ]; then
    printf '  ✓ %-40s %s %s → %s\n' "$id" "$method" "$target" "$status"
    cases+="    <testcase classname=\"tag-access.$(xml "$group")\" name=\"$(xml "$name")\" time=\"$elapsed\"/>"$'\n'
  else
    failed=$((failed + 1))
    printf '  ✗ %-40s %s %s → %s\n' "$id" "$method" "$target" "$problem"
    detail="$method $target as $caller: $problem"$'\n'"$(head -c 600 <<<"$body")"
    cases+="    <testcase classname=\"tag-access.$(xml "$group")\" name=\"$(xml "$name")\" time=\"$elapsed\">"$'\n'
    cases+="      <failure message=\"$(xml "$problem")\">$(xml "$detail")</failure>"$'\n'
    cases+="    </testcase>"$'\n'
  fi
done <"$MATRIX"

took=$(awk -v a="$started" -v b="$(date +%s.%N)" 'BEGIN { printf "%.3f", b - a }')
{
  printf '<?xml version="1.0" encoding="UTF-8"?>\n'
  printf '<testsuites name="tag-access" tests="%d" failures="%d" time="%s">\n' "$total" "$failed" "$took"
  printf '  <testsuite name="tag-access" tests="%d" failures="%d" errors="0" skipped="0" time="%s" timestamp="%s" hostname="%s">\n' \
    "$total" "$failed" "$took" "$(date -u +%Y-%m-%dT%H:%M:%S)" "$(xml "$GATEWAY")"
  printf '%s' "$cases"
  printf '  </testsuite>\n</testsuites>\n'
} >"$REPORT"

echo "  $((total - failed))/$total access checks hold — report: $REPORT"
[ "$failed" -eq 0 ]
