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

package org.eclipse.fennec.services.broker.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.fennec.mcp.gogo.runtime.annotation.RequireGogoMCP;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

/**
 * This is a Demo Resource for a Jakartars Whiteboard 
 * 
 * @since 1.0
 */
@RequireGogoMCP
@RequireHttpWhiteboard
@RequireJakartarsWhiteboard
@JakartarsResource
@JakartarsName("demo-http-whiteboard")
@Component(service = DemoResource.class, enabled = true, scope = ServiceScope.PROTOTYPE)
@Path("/")
public class DemoResource {

	/**
	 * Please check http://0.0.0.0:8887/ddsr/rest/hello-http-whiteboard
	 * @return
	 */
	@GET
	@Path("/hello-http-whiteboard")
	public String hello() {
		return "Hello World (via HTTP Whiteboard)!";
	}

}
