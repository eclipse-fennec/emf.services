#!/usr/bin/env bash
# Copyright (c) 2026 Contributors to the Eclipse Foundation.
# SPDX-License-Identifier: EPL-2.0
#
# A certificate authority for one harness run, and what scenario M needs
# from it (SECURITY.md §7d): issued fresh every time, so nothing secret
# and nothing that expires is checked in.
#
#   ca.crt                 the harness CA            CN=Fennec Harness CA,O=Fennec
#   server.p12             the broker's TLS identity CN=localhost, SAN localhost + 127.0.0.1
#   truststore.p12         the CA, for whoever must believe it (broker and Java client)
#   <name>.crt/.key/.p12   a client certificate      CN=<name>,O=Fennec
#   stranger.*             a client of ANOTHER authority — must not get in
#
# Every store's password is "changeit". Usage: make-certs.sh <dir> <client>...
set -euo pipefail

DIR="$1"; shift
PASS=changeit
mkdir -p "$DIR"
cd "$DIR"

openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -nodes -days 2 \
  -subj "/O=Fennec/CN=Fennec Harness CA" -keyout ca.key -out ca.crt \
  -addext "basicConstraints=critical,CA:TRUE" -addext "keyUsage=critical,keyCertSign,cRLSign" 2>/dev/null
openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -nodes -days 2 \
  -subj "/O=Elsewhere/CN=Other CA" -keyout other-ca.key -out other-ca.crt \
  -addext "basicConstraints=critical,CA:TRUE" -addext "keyUsage=critical,keyCertSign,cRLSign" 2>/dev/null

issue() { # name subject ca extensions
  local name="$1" subject="$2" ca="$3" ext="$4"
  openssl req -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -nodes -subj "$subject" \
    -keyout "$name.key" -out "$name.csr" 2>/dev/null
  printf '%s\n' "$ext" >"$name.ext"
  openssl x509 -req -in "$name.csr" -CA "$ca.crt" -CAkey "$ca.key" -CAcreateserial -days 2 \
    -extfile "$name.ext" -out "$name.crt" 2>/dev/null
  openssl pkcs12 -export -name "$name" -inkey "$name.key" -in "$name.crt" -certfile "$ca.crt" \
    -passout "pass:$PASS" -out "$name.p12"
}

issue server "/O=Fennec/CN=localhost" ca \
  "subjectAltName=DNS:localhost,IP:127.0.0.1
extendedKeyUsage=serverAuth"
for client in "$@"; do
  issue "$client" "/O=Fennec/CN=$client" ca "extendedKeyUsage=clientAuth"
done
issue stranger "/O=Elsewhere/CN=java-client" other-ca "extendedKeyUsage=clientAuth"

rm -f truststore.p12
keytool -importcert -noprompt -alias harness-ca -file ca.crt -keystore truststore.p12 \
  -storetype PKCS12 -storepass "$PASS" >/dev/null 2>&1
