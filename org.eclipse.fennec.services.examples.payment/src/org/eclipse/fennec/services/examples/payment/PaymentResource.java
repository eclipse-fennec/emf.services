/********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Data In Motion Consulting - initial implementation
 ********************************************************************/

package org.eclipse.fennec.services.examples.payment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Demo Java Payment service. In-memory balances, no persistence,
 * no auth. Mirrors the operations declared in the Payment
 * ServiceInterface that the TypeScript colleague publishes to the
 * broker catalog:
 * <ul>
 *   <li>{@code GET  /balance?accountId=...} → current balance</li>
 *   <li>{@code POST /charge?amount=...&currency=...&accountId=...}
 *       → remaining balance after the charge</li>
 * </ul>
 *
 * <p>The TS impl uses {@code POST /charge} and {@code GET /balance}
 * (see broker-state.xmi); this Java impl exposes the same shape so a
 * single PaymentRemote stub works against either provider.
 */
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsName("ddsr-example-payment")
@Component(service = PaymentResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/")
public class PaymentResource {

	private static final Map<String, Double> balances = new ConcurrentHashMap<>();
	private static final double STARTING_BALANCE = 1234.0;

	@GET
	@Path("/balance")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBalance(@QueryParam("accountId") String accountId) {
		if (accountId == null || accountId.isBlank()) {
			return Response.status(400).entity("accountId is required").build();
		}
		double balance = balances.computeIfAbsent(accountId, k -> STARTING_BALANCE);
		return Response.ok(Double.toString(balance)).build();
	}

	@POST
	@Path("/charge")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public Response charge(@QueryParam("accountId") String accountId,
			@QueryParam("amount") Double amount,
			@QueryParam("currency") String currency) {
		if (accountId == null || accountId.isBlank()) {
			// The published contract is charge(amount, currency?) — the
			// account is not part of it, so charges without one go to a
			// default account instead of rejecting what the service
			// description explicitly allows (found by the FR-P4 harness).
			accountId = "default";
		}
		if (amount == null || amount <= 0) {
			return Response.status(400).entity("amount must be positive").build();
		}
		double previous = balances.computeIfAbsent(accountId, k -> STARTING_BALANCE);
		double remaining = previous - amount;
		balances.put(accountId, remaining);
		return Response.ok(Double.toString(remaining)).build();
	}
}
