# Copyright (c) 2026 Contributors to the Eclipse Foundation.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

# Who may read the registry through the tag facade (#282, TAGS.md).
#
# OPA is the policy decision point, the gateway (APISIX, plugin opa) the
# enforcement point in front of the facade. The decision needs only the
# caller and the path: /…/scoped/<tag>/<api>[/…] names the tag, and the
# caller's Keycloak groups name what it may see — a group /tags/<path>
# grants that tag and everything under it. An administrator sees every
# tag. Anything else is denied, including a request without a token.
#
# The token is decoded, not verified, here: the facade verifies every token
# itself (signature, issuer, audience), and a forged one gets past this
# policy only to be turned away there.
package fennec.tags

import rego.v1

default allow := false

# The tag a facade request names.
tag := parts[0][1] if {
	parts := regex.find_all_string_submatch_n(`/scoped/(.+?)/(references|events|catalog|consumers)(/.*)?$`, input.request.path, 1)
	count(parts) == 1
}

claims := payload if {
	authorization := input.request.headers.authorization
	startswith(lower(authorization), "bearer ")
	[_, payload, _] := io.jwt.decode(substring(authorization, 7, -1))
}

# An administrator sees every tag.
allow if {
	tag
	"service-admin" in claims.realm_access.roles
}

# A group /tags/<granted> covers <granted> and every tag under it.
allow if {
	some group in claims.groups
	startswith(group, "/tags/")
	covers(substring(group, 6, -1), tag)
}

covers(granted, requested) if granted == requested

covers(granted, requested) if startswith(requested, concat("", [granted, "/"]))
