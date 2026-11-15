# Copyright (c) 2026 Contributors to the Eclipse Foundation.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

# What the tag policy decides (#282) — run with: opa test itest/opa
package fennec.tags_test

import rego.v1

import data.fennec.tags

# An unsigned token is enough here: the policy decodes, the facade verifies.
token(claims) := concat(".", [
	base64url.encode_no_pad(json.marshal({"alg": "none", "typ": "JWT"})),
	base64url.encode_no_pad(json.marshal(claims)),
	"sig",
])

request(path, claims) := {"request": {"path": path, "headers": {"authorization": concat(" ", ["Bearer", token(claims)])}}}

test_a_group_grants_its_tag if {
	tags.allow with input as request("/ddsr/rest/scoped/payment/terms/instant/references", {"groups": ["/tags/payment/terms/instant"]})
}

test_a_group_grants_no_sibling if {
	not tags.allow with input as request("/ddsr/rest/scoped/payment/terms/installment/references", {"groups": ["/tags/payment/terms/instant"]})
}

test_a_parent_group_covers_its_children if {
	tags.allow with input as request("/ddsr/rest/scoped/payment/terms/installment/events", {"groups": ["/tags/payment"]})
}

test_a_child_group_does_not_cover_its_parent if {
	not tags.allow with input as request("/ddsr/rest/scoped/payment/references", {"groups": ["/tags/payment/terms/instant"]})
}

test_a_prefix_is_not_a_parent if {
	not tags.allow with input as request("/ddsr/rest/scoped/payment/termsX/references", {"groups": ["/tags/payment/terms"]})
}

test_an_administrator_sees_every_tag if {
	tags.allow with input as request("/ddsr/rest/scoped/anything/catalog/", {"realm_access": {"roles": ["service-admin"]}})
}

test_no_token_no_tag if {
	not tags.allow with input as {"request": {"path": "/ddsr/rest/scoped/payment/references", "headers": {}}}
}

test_only_the_facade_is_decided_on if {
	not tags.allow with input as request("/ddsr/rest/references", {"realm_access": {"roles": ["service-admin"]}})
}
